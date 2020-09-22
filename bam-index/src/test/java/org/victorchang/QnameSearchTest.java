package org.victorchang;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.victorchang.QnameSearcher.DebuggingHandler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class QnameSearchTest {
    private static final int MAX_RECORD = 500_000;
    private static final int MAX_THREAD = 4;

    private static final Logger log = LoggerFactory.getLogger(QnameSearchTest.class);

    private QnameIndexer indexer;
    private  QnameSearcher searcher;

    @Before
    public void setUp() {
        BamFileReader fileReader = new DefaultBamFileReader(new EfficientBamRecordParser());
        indexer = new QnameIndexer(fileReader, new KeyPointerWriter(), new KeyPointerReader(), MAX_THREAD, MAX_RECORD);

        BamRecordReader bamRecordReader = new DefaultBamRecordReader(new EfficientBamRecordParser());
        KeyPointerReader keyPointerReader = new KeyPointerReader();
        searcher = new QnameSearcher(keyPointerReader, bamRecordReader, new DebuggingHandler());
    }

    @Test
    public void searchExample1bTest() throws IOException, URISyntaxException {
        Path bamFile = Paths.get(ClassLoader.getSystemResource("bam/example1b").toURI());
        Path indexFolder = Paths.get("bam/example1b");
        Files.createDirectories(indexFolder);

        long start = System.nanoTime();
        long recordCount = indexer.index(indexFolder, bamFile, indexFolder);
        long finish = System.nanoTime();

        log.info("Create index of {} records completed in {}", recordCount, (finish - start) / 1000_000 + "ms");

        int found = searcher.search(bamFile, indexFolder, "SOLEXA-1GA-1_1_FC20EMA:7:100:100:372");
        assertThat(found, equalTo(2));

        found = searcher.search(bamFile, indexFolder, "SOLEXA-1GA-1_1_FC20EMA:7:233:258:501");
        assertThat(found, equalTo(2));
    }

}