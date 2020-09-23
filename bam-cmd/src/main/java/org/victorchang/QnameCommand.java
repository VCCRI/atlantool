package org.victorchang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

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
        return Path.of(bamPath.getParent().toString(), fileName + ".qindex");
    }
}

@Command(name = "index")
class IndexCommand implements Callable<Integer> {
    @Parameters(paramLabel = "bam-file", description = "Path to the BAM file")
    Path bamPath;

    @Option(names = {"-i", "--index-path"}, description = "Directory to store index files. By default uses the same directory as bam file.")
    Path indexDirectory;
    @Option(names = "--thread-count", description = "Number of threads used for sorting", defaultValue = "1")
    int threadCount;
    @Option(names = "--sort-buffer-size", description = "Maximum number of records per buffer used for sorting", defaultValue = "500000")
    int sortBufferSize;
    @Option(names = {"-l", "--limit-bytes"}, description = "Only read and index first given bytes")
    long bytesLimit;
    @Option(names = {"-t", "--temporary-path"}, description = "Directory to store temporary files for sorting. By default uses the index directory.")
    Path tempDirectory;
    @Option(names = {"-v", "--verbose" }, description = "Switch on verbose output", defaultValue = "false")
    boolean verbose;

    @Override
    public Integer call() {
        java.util.logging.Logger.getLogger("")
                .setLevel(verbose ? java.util.logging.Level.ALL : java.util.logging.Level.SEVERE);

        if (!Files.isRegularFile(bamPath)) {
            System.err.println(bamPath + " not found.");
            return -1;
        }
        if (!setIndexDirectory(bamPath)) {
            return -1;
        }

        if (tempDirectory == null) {
            tempDirectory = indexDirectory;
        } else if (!checkDirectory(tempDirectory)) {
            return -1;
        }

        bytesLimit = bytesLimit == 0 ? Long.MAX_VALUE : bytesLimit;

        BamFileReader fileReader = new DefaultBamFileReader(new EfficientBamRecordParser());
        QnameIndexer indexer = new QnameIndexer(fileReader,
                new KeyPointerWriter(),
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

    private boolean setIndexDirectory(Path bamPath) {
        if (indexDirectory == null) {
            indexDirectory = QnameCommand.getDefaultIndexPath(bamPath);
        }
        return checkDirectory(indexDirectory);
    }

    private boolean checkDirectory(Path path) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                LOG.error("Could not create directory: {}. Error : {}", path, e.getMessage());
                return false;
            }
        } else {
            LOG.error("{} already exists.", path);
            return false;
        }
        return true;
    }
}

@Command(name = "view")
class ViewCommand implements Callable<Integer> {
    @Parameters(paramLabel = "bam-file", description = "Path to the BAM file")
    Path bamPath;
    @Parameters(paramLabel = "qname", description = "QNAME to search for")
    String qname;

    @Option(names = {"-i", "--index-path"}, description = "Index directory.")
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

        searcher.search(bamPath, indexDirectory, qname);
        handler.finish();

        long finish = System.nanoTime();

        LOG.info("Search completed in " + (finish - start) / 1000_000 + "ms");
        return 0;
    }
}