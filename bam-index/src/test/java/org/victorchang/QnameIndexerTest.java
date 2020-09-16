package org.victorchang;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QnameIndexerTest {
    private static final Logger log = LoggerFactory.getLogger(QnameIndexerTest.class);

    @Test
    public void createIndexTest() throws IOException, URISyntaxException {
        Path example2 = Paths.get(ClassLoader.getSystemResource("bam/example2").toURI());

        BamFileReader fileReader = new DefaultBamFileReader(new DefaultBamRecordParser());
        QnameIndexer indexer = new QnameIndexer(fileReader);

        long start = System.nanoTime();
        indexer.createIndex(example2, Paths.get("."));
        long finish = System.nanoTime();

        log.info("Creating index completed " + (finish - start) / 1000_000 + "ms");
    }
}