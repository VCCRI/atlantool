package org.victorchang;

import htsjdk.samtools.BAMRecord;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class BamFileReaderTest {
    private static final Logger log = LoggerFactory.getLogger(BamFileReaderTest.class);

    @Test
    public void testReadSingleRecordSample() throws IOException, URISyntaxException {
        URL example1 = ClassLoader.getSystemResource("bam/single-record");
        Path path = Paths.get(example1.toURI());

        BamFileReader fileReader = new DefaultBamFileReader(new SamtoolsBasedParser());

        AlignmentRecordingHandler handler = new AlignmentRecordingHandler();
        fileReader.read(path, handler);

        assertThat(handler.getRecord().getReadString(), equalTo("CCCCAACCCTAACCCTAACCCTAACCCTAACCTAAC"));
        assertThat(handler.getRecord().getReadName(), equalTo("SOLEXA-1GA-1_0047_FC62472:5:81:15648:19537#0"));
    }

    @Test
    public void testReadSample1() throws IOException, URISyntaxException {
        URL example1 = ClassLoader.getSystemResource("bam/example1");
        Path path = Paths.get(example1.toURI());

        BamFileReader fileReader = new DefaultBamFileReader(new EfficientBamRecordParser());

        TestHandler handler = new TestHandler();
        long start = System.nanoTime();
        fileReader.read(path, handler);
        long finish = System.nanoTime();

        log.info(String.format("#blocks %d, #records %d, duration %d ms\n",
                handler.getBlockCount(), handler.getRecordCount(),  (finish - start) / 1000_000));
    }

    @Test
    public void testReadSample2() throws IOException, URISyntaxException {
        URL example2 = ClassLoader.getSystemResource("bam/example2");
        Path path = Paths.get(example2.toURI());

        BamFileReader fileReader = new DefaultBamFileReader(new EfficientBamRecordParser());

        TestHandler handler = new TestHandler();
        long start = System.nanoTime();
        fileReader.read(path, handler);
        long finish = System.nanoTime();

        log.info(String.format("#blocks %d, #records %d, duration %d ms\n",
                handler.getBlockCount(), handler.getRecordCount(),  (finish - start) / 1000_000));
    }

    @Test
    public void testReadSample3() throws IOException, URISyntaxException {
        URL example3 = ClassLoader.getSystemResource("bam/example3");
        Path path = Paths.get(example3.toURI());

        BamFileReader fileReader = new DefaultBamFileReader(new EfficientBamRecordParser());

        TestHandler handler = new TestHandler();
        long start = System.nanoTime();
        fileReader.read(path, handler);
        long finish = System.nanoTime();

        log.info(String.format("#blocks %d, #records %d, duration %d ms\n",
                handler.getBlockCount(), handler.getRecordCount(),  (finish - start) / 1000_000));
    }

    private static class TestHandler implements BamRecordHandler {
        private long blockCount;
        private long recordCount;
        private long blockPos;
        private long offset;

        public TestHandler() {
            blockCount = 0;
            recordCount = 0;
        }

        public long getBlockCount() {
            return blockCount;
        }

        public long getRecordCount() {
            return recordCount;
        }

        @Override
        public void onAlignmentPosition(long blockPos, int offset) {
            this.blockPos = blockPos;
            this.offset = offset;

            recordCount++;
            blockCount++;
        }

        @Override
        public void onQname(byte[] qnameBuffer, int qnameLen) {
            if (recordCount % 100_000 == 1) {
                String decoded = Ascii7Coder.INSTANCE.decode(qnameBuffer, 0, qnameLen);
                log.debug(String.format("block pos %d, offset %d, qname %s\n", blockPos, offset, decoded));
            }
        }

        @Override
        public void onSequence(byte[] seqBuffer, int seqLen) {
            if (recordCount % 100_000 == 1) {
                String decoded = SeqDecoder.INSTANCE.decode(seqBuffer, 0, seqLen);
                log.debug(String.format("block pos %d, offset %d, seq %s\n", blockPos, offset, decoded));
            }
        }

        @Override
        public void onAlignmentRecord(BAMRecord record) {
        }
    }

    private class AlignmentRecordingHandler implements BamRecordHandler {

        private BAMRecord record;

        @Override
        public void onAlignmentPosition(long blockPos, int offset) {
        }

        @Override
        public void onQname(byte[] qnameBuffer, int qnameLen) {
        }

        @Override
        public void onSequence(byte[] seqBuffer, int seqLen) {
        }

        @Override
        public void onAlignmentRecord(BAMRecord record) {
            this.record = record;
        }

        public BAMRecord getRecord() {
            return record;
        }
    }
}