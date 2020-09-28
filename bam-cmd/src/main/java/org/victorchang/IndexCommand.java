package org.victorchang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "index",
        header = "Create a QNAME based index for a BAM file",
        description = "Create an index for a BAM file based on QNAME. The index can be used later to fetch records from the BAM" +
                " file efficiently using the `view` sub command.")
class IndexCommand implements Callable<Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(IndexCommand.class);

    @CommandLine.Parameters(paramLabel = "bam-file", description = "Path to the BAM file")
    private Path bamPath;

    @CommandLine.Option(names = {"-i", "--index-path"},
            description = "Directory to store index files. By default uses a directory name that starts with the BAM file name (so stored next to it)")
    private Path indexDirectory;

    @CommandLine.Option(names = "--thread-count",
            description = "Number of threads used for sorting",
            defaultValue = "1")
    private int threadCount;

    @CommandLine.Option(names = "--sort-buffer-size",
            description = "Maximum number of records per buffer used for sorting",
            defaultValue = "500000")
    private int sortBufferSize;

    @CommandLine.Option(names = {"-l", "--limit-bytes"},
            description = "Only read and index first given bytes")
    private long bytesLimit;

    @CommandLine.Option(names = {"-t", "--temporary-path"},
            description = "Directory to store temporary files for sorting. By default uses the index path")
    private Path tempDirectory;

    @CommandLine.Option(names = {"-v", "--verbose"},
            description = "Switch on verbose output", defaultValue = "false")
    private boolean verbose;

    @CommandLine.Option(names = {"--force"},
            description = "Overwrite existing index",
            defaultValue = "false")
    private boolean force;

    @CommandLine.Option(names = {"--compression"},
            description = "Compression level (1 to 9). 1 = faster but bigger index file size, 9 = slower but smaller index file size",
            defaultValue = "6")
    int compressionLevel;

    @Override
    public Integer call() {
        Logging.configure(verbose);

        if (!Files.isRegularFile(bamPath)) {
            System.err.println(bamPath + " not found.");
            return -1;
        }
        if (!createIndexDirectory(bamPath, force)) {
            return -1;
        }

        if (tempDirectory == null) {
            tempDirectory = indexDirectory;
        } else if (!createDirectory(tempDirectory)) {
            return -1;
        }

        bytesLimit = bytesLimit == 0 ? Long.MAX_VALUE : bytesLimit;

        BamFileReader fileReader = new DefaultBamFileReader();
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
        if (createDirectory(indexDirectory)) {
            Path indexLevel1 = indexDirectory.resolve(IndexVersion.LATEST.fileName("index"));
            Path indexLevel0 = indexDirectory.resolve(IndexVersion.LATEST.fileName("data"));
            if (Files.exists(indexLevel1) || Files.exists(indexLevel0)) {
                if (!force) {
                    LOG.error("Index '{}' exists in '{}'.", IndexVersion.LATEST, indexDirectory);
                    return false;
                } else {
                    LOG.error("Index '{}' exists in '{}', overwrite it", IndexVersion.LATEST, indexDirectory);
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    private boolean createDirectory(Path path) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                LOG.error("Could not create directory: {}. Error : {}", path, e.getMessage());
                return false;
            }
        }
        return Files.isDirectory(path);
    }
}
