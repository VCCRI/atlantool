package org.victorchang;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QnameSearcherTest {
    @Test
    public void testSearch() throws IOException, URISyntaxException {
        Path example2 = Paths.get(ClassLoader.getSystemResource("bam/example2").toURI());
        Path indexFile = Paths.get(".").resolve("qname0");

        BamRecordReader recordReader = new DefaultBamRecordReader(new DefaultBamRecordParser());
        QnameSearcher searcher = new QnameSearcher(example2, indexFile, recordReader);

        searcher.search("SOLEXA-1GA-1_6_FC20ET7:6:123:333:155");
    }
}