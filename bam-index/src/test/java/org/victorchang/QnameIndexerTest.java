package org.victorchang;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QnameIndexerTest {
    private static final int MAX_RECORD = 2000_000;
    private static final Logger log = LoggerFactory.getLogger(QnameIndexerTest.class);

    @Test
    public void createIndexTest() throws IOException, URISyntaxException {
        Path bamFile = Paths.get(ClassLoader.getSystemResource("bam/example1b").toURI());

        BamFileReader fileReader = new DefaultBamFileReader(new DefaultBamRecordParser());
        QnameIndexer indexer = new QnameIndexer(fileReader, MAX_RECORD);

        long start = System.nanoTime();
        indexer.createIndex(Paths.get("."), bamFile);
        long finish = System.nanoTime();

        log.info("Create index completed in " + (finish - start) / 1000_000 + "ms");
    }
}