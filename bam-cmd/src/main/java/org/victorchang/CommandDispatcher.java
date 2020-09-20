package org.victorchang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommandDispatcher {
    private static final Logger log = LoggerFactory.getLogger(CommandDispatcher.class);

    private static final int DEFAULT_THREAD_COUNT = 1;
    private static final int DEFAULT_SORT_BUFFER_SIZE = 500_000;

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            usage();
        }

        if (args[0].equals("index")) {
            Path bamFile = getBamFile(args[1]);
            Path indexFolder = getIndexFolder(args[2]);

            int threadCount = DEFAULT_THREAD_COUNT;
            if (args.length > 3) {
                try {
                    threadCount = Integer.parseInt(args[3]);
                } catch (NumberFormatException ignored) {
                }
            }

            int sortBufferSize = DEFAULT_SORT_BUFFER_SIZE;
            if (args.length > 4) {
                try {
                    sortBufferSize = Integer.parseInt(args[4]);
                } catch (NumberFormatException ignored) {
                }
            }

            long bytesLimit = Long.MAX_VALUE;
            if (args.length > 5) {
                try {
                    bytesLimit = Long.parseLong(args[5]);
                } catch (NumberFormatException ignored) {
                }
            }

            BamFileReader fileReader = new DefaultBamFileReader(new DefaultBamRecordParser());
            QnameIndexer indexer = new QnameIndexer(fileReader,
                    new KeyPointerWriter(),
                    new KeyPointerReader(),
                    threadCount,
                    sortBufferSize);
            try {
                log.info("Creating index {} using {} threads with sort buffer size of {} records",
                        bytesLimit == Long.MAX_VALUE ? "" : "for the first " + bytesLimit + " bytes",
                        threadCount,
                        sortBufferSize);

                long start = System.nanoTime();
                indexer.index(indexFolder, bamFile, bytesLimit);
                long finish = System.nanoTime();

                log.info("Create index completed in {}", (finish - start) / 1000_000 + "ms");

            } finally {
                indexer.shutDown();
            }

            return;
        }

        if (args[0].equals("search")) {
            if (args.length != 4) {
                usage();
            }

            Path bamFile = getBamFile(args[1]);
            Path indexFolder = getIndexFolder(args[2]);
            String qname = args[3];

            long start = System.nanoTime();

            BamRecordReader bamRecordReader = new DefaultBamRecordReader(new DefaultBamRecordParser());
            KeyPointerReader qnamePosReader = new KeyPointerReader();
            QnameSearcher searcher = new QnameSearcher(qnamePosReader, bamRecordReader);

            searcher.search(bamFile, indexFolder, qname);

            long finish = System.nanoTime();

            log.info("Search completed in " + (finish - start) / 1000_000 + "ms");
            return;
        }

        usage();
    }

    private static Path getIndexFolder(String arg) {
        Path indexFolder = Paths.get(arg);
        if (!Files.exists(indexFolder) || !Files.isDirectory(indexFolder)) {
            System.err.println(indexFolder + " doesn't exist");
            System.exit(-1);
        }
        return indexFolder;
    }

    private static Path getBamFile(String name) {
        Path bamFile = Paths.get(name);
        if (!Files.exists(bamFile) || !Files.isRegularFile(bamFile)) {
            System.err.println(bamFile + " is not found");
            System.exit(-1);
        }
        return bamFile;
    }

    private static void usage() {
        System.err.println("Usage:");
        System.err.println("\tjava -jar bam-cmd.jar index <bam file> <index directory> [threadCount=1] [sortBufferSize=500000] [bytesLimit]");
        System.err.println("\tjava -jar bam-cmd.jar search <bam file> <index directory> <qname>");
        System.exit(-1);
    }
}
