package org.victorchang;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BamFileReaderTest {
    private static final Logger log = LoggerFactory.getLogger(BamFileReaderTest.class);

    @Test
    public void testReadSample1() throws IOException, URISyntaxException {
        URL example1 = ClassLoader.getSystemResource("bam/example1");
        Path path = Paths.get(example1.toURI());

        BamFileReader fileReader = new DefaultBamFileReader(new DefaultBamRecordParser());

        TestHandler handler = new TestHandler();
        long start = System.nanoTime();
        fileReader.read(path, handler);
        long finish = System.nanoTime();

        log.info(String.format("#records %d, duration %d ms", handler.getRecordCount(),  (finish - start) / 1000_000));
    }

    @Test
    public void testReadSample2() throws IOException, URISyntaxException {
        URL example2 = ClassLoader.getSystemResource("bam/example2");
        Path path = Paths.get(example2.toURI());

        BamFileReader fileReader = new DefaultBamFileReader(new DefaultBamRecordParser());

        TestHandler handler = new TestHandler();
        long start = System.nanoTime();
        fileReader.read(path, handler);
        long finish = System.nanoTime();

        log.info(String.format("#records %d, duration %d ms", handler.getRecordCount(),  (finish - start) / 1000_000));
    }

    @Test
    public void testReadSample3() throws IOException, URISyntaxException {
        URL example3 = ClassLoader.getSystemResource("bam/example3");
        Path path = Paths.get(example3.toURI());

        BamFileReader fileReader = new DefaultBamFileReader(new DefaultBamRecordParser());

        TestHandler handler = new TestHandler();
        long start = System.nanoTime();
        fileReader.read(path, handler);
        long finish = System.nanoTime();

        log.info(String.format("#records %d, duration %d ms", handler.getRecordCount(),  (finish - start) / 1000_000));
    }

    private static class TestHandler implements BamRecordHandler {
        private long recordCount;
        private long coffset;
        private int uoffset;

        private String qname;

        public TestHandler() {
            recordCount = 0;
        }

        public long getRecordCount() {
            return recordCount;
        }

        @Override
        public void onRecord(long coffset, int uoffset) {
            this.coffset = coffset;
            this.uoffset = uoffset;
            recordCount++;
        }

        @Override
        public void onQname(byte[] qnameBuffer, int qnameLen) {
            qname = Ascii7Coder.INSTANCE.decode(qnameBuffer, 0, qnameLen);
        }

        @Override
        public void onSequence(byte[] seqBuffer, int seqLen) {
            String seq = SeqDecoder.INSTANCE.decode(seqBuffer, 0, seqLen);
            if (recordCount <= 5) {
                log.debug(String.format("coffset %d, uoffset %d, qname %s, seq %s", coffset, uoffset, qname, seq));
            }
        }
    }
}