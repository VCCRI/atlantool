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
        name = "qname-search",
        description = "Search BAM file by QNAME"
)
public class QnameCommand {
    static final Logger LOG = LoggerFactory.getLogger(QnameCommand.class);

    public static void main(String[] args) {
        int exitCode = new CommandLine(new QnameCommand()).execute(args);
        System.exit(exitCode);
    }
}

@Command(name = "index")
class IndexCommand implements Callable<Integer> {

    private static final int DEFAULT_THREAD_COUNT = 1;
    private static final int DEFAULT_SORT_BUFFER_SIZE = 500_000;

    @Parameters(paramLabel = "bam-file", description = "Path to the bam file")
    Path bamPath;
    @Parameters(paramLabel = "index-path", description = "Directory to store index file")
    Path indexDirectory;
    @Option(names = "--thread-count", description = "No.of threads to use", defaultValue = "1")
    int threadCount;
    @Option(names = "--limit-bytes", description = "Only read and index first given bytes")
    long bytesLimit;


    @Override
    public Integer call() throws Exception {
        if (!Files.isRegularFile(bamPath)) {
            System.err.println(bamPath + " not found.");
            return -1;
        }
        bytesLimit = bytesLimit == 0 ? Long.MAX_VALUE : bytesLimit;
        int sortBufferSize = DEFAULT_SORT_BUFFER_SIZE;

        BamFileReader fileReader = new DefaultBamFileReader(new DefaultBamRecordParser());
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
            indexer.index(indexDirectory, bamPath, bytesLimit);
            long finish = System.nanoTime();

            LOG.info("Create index completed in {}", (finish - start) / 1000_000 + "ms");
        } catch (IOException e) {
            LOG.info("Failed to index", e);
            return -1;
        } finally {
            indexer.shutDown();
        }
        return 0;
    }
}

@Command(name = "view")
class ViewCommand implements Callable<Integer> {
    @Parameters(paramLabel = "bam-file", description = "Path to the bam file")
    Path bamPath;
    @Parameters(paramLabel = "index-path", description = "Directory to store index file")
    Path indexPath;
    @Parameters(paramLabel = "qname", description = "Qname to search for")
    String qname;

    @Override
    public Integer call() throws Exception {
        if (!Files.isRegularFile(bamPath)) {
            System.err.println(bamPath + " not found.");
            return -1;
        }
        if (!Files.isDirectory(indexPath)) {
            System.err.println(indexPath + " not found.");
            return -1;
        }

        long start = System.nanoTime();

        BamRecordReader bamRecordReader = new DefaultBamRecordReader(new DefaultBamRecordParser());
        KeyPointerReader qnamePosReader = new KeyPointerReader();
        QnameSearcher searcher = new QnameSearcher(qnamePosReader, bamRecordReader);

        searcher.search(bamPath, indexPath, qname);

        long finish = System.nanoTime();

        LOG.info("Search completed in " + (finish - start) / 1000_000 + "ms");
        return 0;
    }
}