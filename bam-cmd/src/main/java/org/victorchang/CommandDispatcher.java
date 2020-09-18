package org.victorchang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommandDispatcher {
    private static final Logger log = LoggerFactory.getLogger(CommandDispatcher.class);

    private static final int MAX_RECORD = 500_000;

    public static void main(String[] args) throws IOException {
        if (args.length != 3 && args.length != 4) {
            usage();
        }

        if (args.length == 3) {
            if (!args[0].equals("index")) {
                usage();
            }

            Path bamFile = getBamFile(args[1]);
            Path indexFolder = getIndexFolder(args[2]);

            BamFileReader fileReader = new DefaultBamFileReader(new DefaultBamRecordParser());
            QnameIndexer indexer = new QnameIndexer(fileReader, new QnamePosWriter(), new QnamePosReader(), MAX_RECORD);

            long start = System.nanoTime();
            indexer.createIndex(indexFolder, bamFile);
            long finish = System.nanoTime();

            log.info("Create index completed in " + (finish - start) / 1000_000 + "ms");

        }

        if (args.length == 4) {
            if (!args[0].equals("search")) {
                usage();
            }
            Path bamFile = getBamFile(args[1]);
            Path indexFolder = getIndexFolder(args[2]);
            String qname = args[3];

            long start = System.nanoTime();

            BamRecordReader bamRecordReader = new DefaultBamRecordReader(new DefaultBamRecordParser());
            QnamePosReader qnamePosReader = new QnamePosReader();
            QnameSearcher searcher = new QnameSearcher(qnamePosReader, bamRecordReader);

            searcher.search(bamFile, indexFolder, qname);

            long finish = System.nanoTime();

            log.info("Search completed in " + (finish - start) / 1000_000 + "ms");
        }
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
        System.err.println("\tjava -jar bam-cmd.jar index <bam file> <index directory>");
        System.err.println("\tjava -jar bam-cmd.jar search <bam file> <index directory> <qname>");
        System.exit(-1);
    }
}
