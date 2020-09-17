package org.victorchang;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QnameSearcherTest {
    private static final Logger log = LoggerFactory.getLogger(QnameIndexerTest.class);

    @Test
    public void testSearch() throws IOException, URISyntaxException {
        Path example2 = Paths.get(ClassLoader.getSystemResource("bam/example1b").toURI());
        Path indexFolder = Paths.get(".");

        long start = System.nanoTime();

        BamRecordReader recordReader = new DefaultBamRecordReader(new DefaultBamRecordParser());
        QnameSearcher searcher = new QnameSearcher(recordReader, indexFolder, example2);

        searcher.search("SOLEXA-1GA-1_4_FC20ENL:7:172:55:704");

        long finish = System.nanoTime();

        log.info("Search completed in " + (finish - start) / 1000_000 + "ms");
    }
}