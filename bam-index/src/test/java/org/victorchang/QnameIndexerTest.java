package org.victorchang;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QnameIndexerTest {
    private static final Logger log = LoggerFactory.getLogger(QnameIndexerTest.class);

    @Test
    public void createIndex() throws IOException, URISyntaxException {
        URL example2 = ClassLoader.getSystemResource("bam/example2");
        Path path = Paths.get(example2.toURI());

        BamFileReader fileReader = new DefaultBamFileReader(new DefaultBamRecordParser());
        QnameIndexer indexer = new QnameIndexer(fileReader);

        long start = System.nanoTime();
        indexer.createIndex(path);
        long finish = System.nanoTime();

        log.info("index duration " + (finish - start) / 1000_000 + "ms");
    }
}