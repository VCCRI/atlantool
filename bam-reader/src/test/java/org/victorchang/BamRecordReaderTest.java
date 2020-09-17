package org.victorchang;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class BamRecordReaderTest {
    @Test
    public void testReadSample1() throws IOException, URISyntaxException {
        URL example1 = ClassLoader.getSystemResource("bam/example1");
        Path path = Paths.get(example1.toURI());

        BamRecordReader recordReader = new DefaultBamRecordReader(new DefaultBamRecordParser());

        RecordFetcher handler = new RecordFetcher();
        recordReader.read(path, 36300895, 59353, handler);

        assertThat(handler.getQname(), equalTo("SOLEXA-1GA-1_4_FC20ENL:7:76:613:540"));
        assertThat(handler.getSeq(), equalTo("TTAAAAAAGGAAGGAATTAATTAATT="));
    }

    @Test
    public void testReadSample2() throws IOException, URISyntaxException {
        URL example1 = ClassLoader.getSystemResource("bam/example2");
        Path path = Paths.get(example1.toURI());

        BamRecordReader recordReader = new DefaultBamRecordReader(new DefaultBamRecordParser());

        RecordFetcher handler = new RecordFetcher();
        recordReader.read(path, 28645829, 12964, handler);

        assertThat(handler.getQname(), equalTo("SOLEXA-1GA-1_6_FC20ET7:6:291:877:537"));
        assertThat(handler.getSeq(), equalTo("GGTTAATTCCAAAATTGGTTGGGGGG="));
    }

    @Test
    public void testReadSample3() throws IOException, URISyntaxException {
        URL example1 = ClassLoader.getSystemResource("bam/example3");
        Path path = Paths.get(example1.toURI());

        BamRecordReader recordReader = new DefaultBamRecordReader(new DefaultBamRecordParser());

        RecordFetcher handler = new RecordFetcher();
        recordReader.read(path, 0, 64974, handler);

        assertThat(handler.getQname(), equalTo("SOLEXA-1GA-1_6_FC20ET7:7:22:94:703"));
        assertThat(handler.getSeq(), equalTo("GGCCTTTTAATTTTCCCCGGGGGGTT="));
    }

    private static class RecordFetcher implements BamRecordHandler {
        private String qname;
        private String seq;

        @Override
        public void onRecord(long blockPos, int offset) {
        }

        @Override
        public void onQname(byte[] qnameBuffer, int qnameLen) {
            this.qname = Ascii7Coder.INSTANCE.decode(qnameBuffer, 0, qnameLen);
        }

        @Override
        public void onSequence(byte[] seqBuffer, int seqLen) {
            this.seq = SeqDecoder.INSTANCE.decode(seqBuffer, 0, seqLen);
        }

        public String getQname() {
            return qname;
        }

        public String getSeq() {
            return seq;
        }
    }
}