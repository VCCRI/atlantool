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
        Path example2 = Paths.get(ClassLoader.getSystemResource("bam/example2").toURI());
        Path indexFolder = Paths.get(".");

        long start = System.nanoTime();

        BamRecordReader recordReader = new DefaultBamRecordReader(new DefaultBamRecordParser());
        QnameSearcher searcher = new QnameSearcher(example2, indexFolder, recordReader);

        searcher.search("SOLEXA-1GA-1_6_FC20ET7:6:123:333:155");

        long finish = System.nanoTime();

        log.info("Search completed in " + (finish - start) / 1000_000 + "ms");
    }
}