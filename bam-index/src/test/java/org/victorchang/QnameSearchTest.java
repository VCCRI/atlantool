package org.victorchang;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QnameSearchTest {
    private static final int MAX_RECORD = 500_000;
    private static final int MAX_THREAD = 4;

    private static final Logger log = LoggerFactory.getLogger(QnameSearchTest.class);

    private QnameIndexer indexer;
    private  QnameSearcher searcher;

    @Before
    public void setUp() {
        BamFileReader fileReader = new DefaultBamFileReader(new DefaultBamRecordParser());
        indexer = new QnameIndexer(fileReader, new QnamePosWriter(), new QnamePosReader(), MAX_THREAD, MAX_RECORD);

        BamRecordReader bamRecordReader = new DefaultBamRecordReader(new DefaultBamRecordParser());
        QnamePosReader qnamePosReader = new QnamePosReader();
        searcher = new QnameSearcher(qnamePosReader, bamRecordReader);
    }

    @Test
    public void createIndexTest() throws IOException, URISyntaxException {
        Path bamFile = Paths.get(ClassLoader.getSystemResource("bam/example1b").toURI());
        Path indexFolder = Paths.get("bam/example1b");
        Files.createDirectories(indexFolder);

        long start = System.nanoTime();
        long recordCount = indexer.createIndex(indexFolder, bamFile);
        long finish = System.nanoTime();

        log.info("Create index of {} records completed in {}", recordCount, (finish - start) / 1000_000 + "ms");

        searcher.search(bamFile, indexFolder, "SOLEXA-1GA-1_4_FC20ENL:7:172:55:704");
        searcher.search(bamFile, indexFolder, "SOLEXA-1GA-1_1_FC20EMA:7:100:100:372");
    }

    @Test
    public void createIndex256M() throws IOException, URISyntaxException {
        Path bamFile = Paths.get(ClassLoader.getSystemResource("bam/256M.bam").toURI());
        Path indexFolder = Paths.get("bam/256M.bam");
        Files.createDirectories(indexFolder);

        long start = System.nanoTime();
        long recordCount = indexer.createIndex(indexFolder, bamFile, 2_200_000);
        long finish = System.nanoTime();

        log.info("Create index of {} records completed in {}", recordCount, (finish - start) / 1000_000 + "ms");

        searcher.search(bamFile, indexFolder, "E00431:98:HCK73ALXX:7:2215:12165:39686");
    }
}