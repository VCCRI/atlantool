package org.victorchang;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.file.StandardOpenOption.READ;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class QnameSearchTest {
    private static final int MAX_RECORD = 500_000;
    private static final int MAX_THREAD = 4;

    private static final Logger log = LoggerFactory.getLogger(QnameSearchTest.class);

    private QnameIndexer indexer;
    private QnameSearcher searcher;

    @Before
    public void setUp() {
        BamFileReader fileReader = new DefaultBamFileReader();
        indexer = new QnameIndexer(fileReader, new KeyPointerWriter(), new KeyPointerReader(), MAX_THREAD, MAX_RECORD);

        BamRecordReader bamRecordReader = new DefaultBamRecordReader();
        KeyPointerReader keyPointerReader = new KeyPointerReader();
        searcher = new QnameSearcher(keyPointerReader, bamRecordReader, (coffset, uoffset, dataInput, recordLength) -> {
        });
    }

    @Test
    public void searchExample1bTest() throws IOException, URISyntaxException {
        Path bamFile = Paths.get(ClassLoader.getSystemResource("bam/example1b").toURI());
        Path indexFolder = Paths.get("target/bam/example1b");
        Files.createDirectories(indexFolder);

        long start = System.nanoTime();
        long recordCount = indexer.index(indexFolder, bamFile, indexFolder);
        long finish = System.nanoTime();

        log.info("Create index of {} records completed in {}", recordCount, (finish - start) / 1000_000 + "ms");
        assertThat(recordCount, equalTo(1837142L));

        // Test some specific keys
        List<Long> pointers = searcher.getPointersForQname(indexFolder, Set.of("SOLEXA-1GA-1_1_FC20EMA:7:100:100:372"));
        assertThat(pointers.size(), equalTo(2));

        pointers = searcher.getPointersForQname(indexFolder, Set.of("SOLEXA-1GA-1_1_FC20EMA:7:233:258:501"));
        assertThat(pointers.size(), equalTo(2));

        // Last QNAME (in sort order)
        pointers = searcher.getPointersForQname(indexFolder, Set.of("SOLEXA-1GA-1_4_FC20ENL:7:9:99:545"));
        assertThat(pointers.size(), equalTo(2));

        // QNAME on 2 adjacent blocks
        pointers = searcher.getPointersForQname(indexFolder, Set.of("SOLEXA-1GA-1_1_FC20EMA:7:100:434:814"));
        assertThat(pointers.size(), equalTo(2));
    }

    @Test
    public void searchMultipleQnames() throws IOException, URISyntaxException {
        Path bamFile = Paths.get(ClassLoader.getSystemResource("bam/example1b").toURI());
        Path indexFolder = Paths.get("target/bam/example1b");
        Files.createDirectories(indexFolder);

        long start = System.nanoTime();
        long recordCount = indexer.index(indexFolder, bamFile, indexFolder);
        long finish = System.nanoTime();

        log.info("Create index of {} records completed in {}", recordCount, (finish - start) / 1000_000 + "ms");

        // Test keys at different blocks
        List<Long> pointers = searcher.getPointersForQname(indexFolder, Set.of("SOLEXA-1GA-1_1_FC20EMA:7:100:100:372",
                "SOLEXA-1GA-1_1_FC20EMA:7:233:258:501", "SOLEXA-1GA-1_4_FC20ENL:7:9:99:545"));
        assertThat(pointers.size(), equalTo(6));

        pointers = searcher.getPointersForQname(indexFolder, Set.of("SOLEXA-1GA-1_1_FC20EMA:7:100:100:448",
                "SOLEXA-1GA-1_1_FC20EMA:7:100:101:206"));
        assertThat(pointers.size(), equalTo(4));

        pointers = searcher.getPointersForQname(indexFolder, Set.of("SOLEXA-1GA-1_1_FC20EMA:7:100:100:372",
                "SOLEXA-1GA-1_1_FC20EMA:7:233:258:501", "SOLEXA-1GA-1_1_FC20EMA:7:100:100:448"));
        assertThat(pointers.size(), equalTo(6));

        pointers = searcher.getPointersForQname(indexFolder, Set.of());
        assertThat(pointers.size(), equalTo(0));
    }

    @Ignore("take too long to run")
    @Test
    public void searchExhaustiveExample1bTest() throws IOException, URISyntaxException {
        Path bamFile = Paths.get(ClassLoader.getSystemResource("bam/example1b").toURI());
        Path indexFolder = Paths.get("target/bam/example1b");
        Files.createDirectories(indexFolder);

        indexer.index(indexFolder, bamFile, indexFolder);

        Path pathLevel0 = indexFolder.resolve(IndexVersion.LATEST.fileName("data"));
        FileChannel channelLevel0 = FileChannel.open(pathLevel0, READ);
        InputStream inputStreamLevel0 = Channels.newInputStream(channelLevel0);

        // Exhaustive test
        Map<String, Integer> expectedMap = new LinkedHashMap<>();
        KeyPointerReader keyPointerReader = new KeyPointerReader();
        Iterable<KeyPointer> indexLevel0 = () -> keyPointerReader.read(inputStreamLevel0).iterator();
        for (KeyPointer x : indexLevel0) {
            String qname = Ascii7Coder.INSTANCE.decode(x.getKey(), 0, x.getKey().length);
            expectedMap.compute(qname, (k, v) -> v == null ? 1 : v + 1);
        }

        for (Map.Entry<String, Integer> entry : expectedMap.entrySet()) {
            Integer expectedMatches = entry.getValue();
            final List<Long> pointers = searcher.getPointersForQname(indexFolder, Set.of(entry.getKey()));
            assertThat("Expected " + expectedMatches + " for name: " + entry.getKey(), pointers.size(), equalTo(expectedMatches));
        }
    }
}