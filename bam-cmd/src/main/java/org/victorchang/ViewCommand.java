package org.victorchang;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReaderFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toSet;

@CommandLine.Command(name = "view",
        header = "View records by QNAME",
        description = "View records with the given QNAME from a BAM file. This command needs an index to be present" +
                " created with the `index` sub command.")
class ViewCommand implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(ViewCommand.class);

    @CommandLine.Parameters(index = "0",
            paramLabel = "bam-file",
            description = "Path to the BAM file")
    private Path bamPath;

    @CommandLine.ArgGroup(multiplicity = "1",
            heading = "One of qname or file containing qnames")
    private QnameParam qnameParam;

    @CommandLine.Option(names = {"-i", "--index-path"},
            description = "Index directory")
    private Path indexDirectory;

    @CommandLine.Option(names = {"-h", "--header"},
            description = "Include header in SAM output",
            defaultValue = "false")
    private boolean includeHeader;

    @CommandLine.Option(names = {"-v", "--verbose"},
            description = "Switch on verbose output",
            defaultValue = "false")
    private boolean verbose;

    @Override
    public Integer call() throws Exception {
        Logging.configure(verbose);

        if (!Files.isRegularFile(bamPath)) {
            System.err.println(bamPath + " not found.");
            return -1;
        }

        if (indexDirectory == null) {
            indexDirectory = QnameCommand.getDefaultIndexPath(bamPath);
        }
        if (!Files.isDirectory(indexDirectory)) {
            System.err.println(indexDirectory + " not found.");
            return -1;
        }

        Path indexLevel1 = indexDirectory.resolve(IndexVersion.LATEST.fileName("index"));
        Path indexLevel0 = indexDirectory.resolve(IndexVersion.LATEST.fileName("data"));

        if (!Files.exists(indexLevel1) || !Files.exists(indexLevel0)) {
            System.err.printf("Index '%s' doesn't exists in '%s'.\n", IndexVersion.LATEST, indexDirectory);
            return -1;
        }

        long start = System.nanoTime();

        SAMFileHeader fileHeader = SamReaderFactory.make().getFileHeader(bamPath);
        BamRecordReader recordReader = new DefaultBamRecordReader();
        KeyPointerReader keyReader = new KeyPointerReader();
        SAMRecordPrinter recordPrinter = new SAMRecordPrinter(System.out, includeHeader);
        SAMRecordGenerator recordHandler = new SAMRecordGenerator(new SAMRecordParser(fileHeader), recordPrinter::print);
        QnameSearcher searcher = new QnameSearcher(keyReader, recordReader, recordHandler);

        final Set<String> qnames;
        try {
            qnames = qnameParam.getQnames();
        } catch (IllegalArgumentException e) {
            LOG.error(e.getMessage());
            return -1;
        }
        searcher.search(bamPath, indexDirectory, qnames);
        recordPrinter.finish();

        long finish = System.nanoTime();

        LOG.info("Search completed in " + (finish - start) / 1000_000 + "ms");
        return 0;
    }
}

class QnameParam {
    @CommandLine.Option(names = {"-n", "--name"}, description = "QNAME to search for")
    String qname;
    @CommandLine.Option(names = {"-f", "--file-name"}, description = "Path to a file containing QNAMEs to search for (separated by newline).")
    Path qnamePath;

    Set<String> getQnames() throws IllegalArgumentException {
        if (qname != null) {
            return Set.of(qname);
        }
        final Map<Boolean, Set<String>> qnames = readFile()
                .stream()
                .filter(StringUtils::isNotBlank)
                .collect(partitioningBy(this::isValidQname, toSet()));
        final Set<String> invalidQnames = qnames.getOrDefault(false, Set.of());
        if (!invalidQnames.isEmpty()) {
            throw new IllegalArgumentException("File " + qnamePath + " contains invalid qnames : " + invalidQnames);
        }
        return qnames.get(true);
    }

    private List<String> readFile() {
        try {
            return Files.readAllLines(qnamePath);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read file: " + qnamePath, e);
        }
    }

    private boolean isValidQname(String qName) {
        try {
            SAMSequenceRecord.validateSequenceName(qName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
