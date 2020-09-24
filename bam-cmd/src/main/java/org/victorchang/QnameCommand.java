package org.victorchang;

import htsjdk.samtools.SAMSequenceRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.partitioningBy;
import static org.victorchang.QnameCommand.LOG;

@Command(
        subcommands = {
                IndexCommand.class,
                ViewCommand.class
        },
        name = "atlantool",
        description = "Search BAM file by QNAME"
)
public class QnameCommand {
    static final Logger LOG = LoggerFactory.getLogger(QnameCommand.class);

    public static void main(String[] args) {
        int exitCode = new CommandLine(new QnameCommand()).execute(args);
        System.exit(exitCode);
    }

    static Path getDefaultIndexPath(Path bamPath) {
        final String fileName = bamPath.getFileName().toString();
        final String parent = Optional
                .ofNullable(bamPath.getParent())
                .map(Path::toString)
                .orElse("");
        return Path.of(parent, fileName + ".atlantool-index");
    }
}

@Command(name = "index")
class IndexCommand implements Callable<Integer> {
    @Parameters(paramLabel = "bam-file", description = "Path to the BAM file")
    Path bamPath;

    @Option(names = {"-i", "--index-path"}, description = "Directory to store index files. By default uses a directory name that starts with the BAM file name (so stored next to it)")
    Path indexDirectory;
    @Option(names = "--thread-count", description = "Number of threads used for sorting", defaultValue = "1")
    int threadCount;
    @Option(names = "--sort-buffer-size", description = "Maximum number of records per buffer used for sorting", defaultValue = "500000")
    int sortBufferSize;
    @Option(names = {"-l", "--limit-bytes"}, description = "Only read and index first given bytes")
    long bytesLimit;
    @Option(names = {"-t", "--temporary-path"}, description = "Directory to store temporary files for sorting. By default uses the index path")
    Path tempDirectory;
    @Option(names = {"-v", "--verbose" }, description = "Switch on verbose output", defaultValue = "false")
    boolean verbose;
    @Option(names = {"--force" }, description = "Overwrite existing index", defaultValue = "false")
    boolean force;
    @Option(names = {"--compression"}, description = "Compression level (1 to 9). 1 = faster but bigger index file size, 9 = slower but smaller index file size", defaultValue = "6")
    int compressionLevel;

    @Override
    public Integer call() {
        java.util.logging.Logger.getLogger("")
                .setLevel(verbose ? java.util.logging.Level.ALL : java.util.logging.Level.SEVERE);

        if (!Files.isRegularFile(bamPath)) {
            System.err.println(bamPath + " not found.");
            return -1;
        }
        if (!createIndexDirectory(bamPath, force)) {
            return -1;
        }

        if (tempDirectory == null) {
            tempDirectory = indexDirectory;
        } else if (!createDirectory(tempDirectory, force)) {
            return -1;
        }

        bytesLimit = bytesLimit == 0 ? Long.MAX_VALUE : bytesLimit;

        BamFileReader fileReader = new DefaultBamFileReader(new EfficientBamRecordParser());
        QnameIndexer indexer = new QnameIndexer(fileReader,
                new KeyPointerWriter(compressionLevel),
                new KeyPointerReader(),
                threadCount,
                sortBufferSize);
        try {
            LOG.info("Creating index {} using {} threads with sort buffer size of {} records",
                    bytesLimit == Long.MAX_VALUE ? "" : "for the first " + bytesLimit + " bytes",
                    threadCount,
                    sortBufferSize);

            long start = System.nanoTime();
            indexer.index(indexDirectory, bamPath, tempDirectory, bytesLimit);
            long finish = System.nanoTime();

            LOG.info("Create index completed in {}", (finish - start) / 1000_000 + "ms");
        } catch (IOException e) {
            LOG.error("Failed to index", e);
            return -1;
        } finally {
            indexer.shutDown();
        }
        return 0;
    }

    private boolean createIndexDirectory(Path bamPath, boolean force) {
        if (indexDirectory == null) {
            indexDirectory = QnameCommand.getDefaultIndexPath(bamPath);
        }
        return createDirectory(indexDirectory, force);
    }

    private boolean createDirectory(Path path, boolean force) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                LOG.error("Could not create directory: {}. Error : {}", path, e.getMessage());
                return false;
            }
        } else {
            if (force) {
                LOG.warn("{} already exists, overwrite it.", path);
            } else {
                LOG.error("{} already exists.", path);
            }
            return force;
        }
        return true;
    }
}

class QnameParam {
    @Option(names = {"-n", "--name"}, description = "QNAME to search for")
    String qname;
    @Option(names = {"-f", "--file-name"}, description = "Path to a file containing QNAMEs to search for (separated by newline).")
    Path qnamePath;

    List<String> getQnames() throws IllegalArgumentException {
        if (qname != null) {
            return singletonList(qname);
        }
        final Map<Boolean, List<String>> qnames = readFile()
                    .stream()
                    .filter(StringUtils::isNotBlank)
                    .collect(partitioningBy(this::isValidQname));
            final List<String> invalidQnames = qnames.getOrDefault(false, emptyList());
            if (!invalidQnames.isEmpty()) {
                throw new IllegalArgumentException("File " + qnamePath + " contains invalid qnames : " + invalidQnames);
            }
            return qnames.get(true);
    }

    private List<String> readFile() {
        try {
            return Files.readAllLines(qnamePath);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read file: " +  qnamePath, e);
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

@Command(name = "view")
class ViewCommand implements Callable<Integer> {
    @Parameters(index = "0", paramLabel = "bam-file", description = "Path to the BAM file")
    Path bamPath;

    @ArgGroup(multiplicity = "1", heading = "One of qname or file containing qnames")
    QnameParam qnameParam;

    @Option(names = {"-i", "--index-path"}, description = "Index directory")
    Path indexDirectory;
    @Option(names = {"-h", "--header"}, description = "Include header in SAM output", defaultValue = "false")
    boolean includeHeader;
    @Option(names = {"-v", "--verbose" }, description = "Switch on verbose output", defaultValue = "false")
    boolean verbose;

    @Override
    public Integer call() throws Exception {
        java.util.logging.Logger.getLogger("")
                .setLevel(verbose ? java.util.logging.Level.ALL : java.util.logging.Level.SEVERE);

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

        long start = System.nanoTime();

        BamRecordReader bamRecordReader = new DefaultBamRecordReader(new SamtoolsBasedParser());
        KeyPointerReader qnamePosReader = new KeyPointerReader();
        SamPrintingHandler handler = new SamPrintingHandler(System.out, includeHeader);
        QnameSearcher searcher = new QnameSearcher(qnamePosReader, bamRecordReader, handler);

        final List<String> qnames;
        try {
            qnames = qnameParam.getQnames();
        } catch (IllegalArgumentException e) {
            LOG.error(e.getMessage());
            return -1;
        }
        for (String qname : qnames) {
            searcher.search(bamPath, indexDirectory, qname);
        }
        handler.finish();

        long finish = System.nanoTime();

        LOG.info("Search completed in " + (finish - start) / 1000_000 + "ms");
        return 0;
    }
}
