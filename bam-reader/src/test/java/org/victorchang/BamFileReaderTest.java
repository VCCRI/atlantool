package org.victorchang;

import htsjdk.samtools.SAMRecord;
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

        log.info(String.format("#records %d, duration %d ms", handler.getRecordCount(),  (finish - start) / 1000_000));
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

        log.info(String.format("#records %d, duration %d ms", handler.getRecordCount(),  (finish - start) / 1000_000));
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
        public void onAlignmentPosition(long coffset, int uoffset) {
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

        @Override
        public void onAlignmentRecord(SAMRecord record) {
        }
    }

    private static class AlignmentRecordingHandler implements BamRecordHandler {

        private SAMRecord record;

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
        public void onAlignmentRecord(SAMRecord record) {
            this.record = record;
        }

        public SAMRecord getRecord() {
            return record;
        }
    }
}