package org.victorchang;

import org.junit.Before;
import org.junit.Test;

import java.io.DataInput;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class BamRecordReaderTest {
    private BamRecordReader recordReader;
    private QnameSeqRecordGenerator recordFetcher;

    @Before
    public void setUp() throws Exception {
        recordReader = new DefaultBamRecordReader();
        recordFetcher = new QnameSeqRecordGenerator(new QnameSeqParser());
    }

    @Test
    public void testReadSample1() throws IOException, URISyntaxException {
        URL example1 = ClassLoader.getSystemResource("bam/example1");
        Path path = Paths.get(example1.toURI());

        recordReader.read(path, 36300895, 59353, recordFetcher);
        assertThat(recordFetcher.getQname(), equalTo("SOLEXA-1GA-1_4_FC20ENL:7:76:613:540"));
        assertThat(recordFetcher.getSeq(), equalTo("TTAATATATGAATGGATTAATTCATTC"));
    }

    @Test
    public void testReadSample2() throws IOException, URISyntaxException {
        URL example1 = ClassLoader.getSystemResource("bam/example2");
        Path path = Paths.get(example1.toURI());

        BamRecordReader recordReader = new DefaultBamRecordReader();

        recordReader.read(path, 28645829, 12964, recordFetcher);

        assertThat(recordFetcher.getQname(), equalTo("SOLEXA-1GA-1_6_FC20ET7:6:291:877:537"));
        assertThat(recordFetcher.getSeq(), equalTo("TGTTGAGTGCTATAGTGGTTTGGGAGG"));
    }

    @Test
    public void testReadSample3() throws IOException, URISyntaxException {
        URL example1 = ClassLoader.getSystemResource("bam/example3");
        Path path = Paths.get(example1.toURI());

        recordReader.read(path, 0, 64974, recordFetcher);
        assertThat(recordFetcher.getQname(), equalTo("SOLEXA-1GA-1_6_FC20ET7:7:22:94:703"));
        assertThat(recordFetcher.getSeq(), equalTo("TGCCCTCTGACTGTGCTCAGGGGGCTC"));
    }

    private static class QnameSeqRecordGenerator implements BamRecordHandler {
        private final QnameSeqParser parser;

        private QnameSeqRecord record;

        private QnameSeqRecordGenerator(QnameSeqParser parser) {
            this.parser = parser;
        }

        public String getQname() {
            return new String(record.qname, 0, record.qnameLen, StandardCharsets.US_ASCII);
        }

        public String getSeq() {
            return SeqDecoder.INSTANCE.decode(record.seq, 0, record.seqLen);
        }

        @Override
        public void onAlignmentRecord(long coffset, int uoffset, DataInput dataInput, int recordLength) {
            try {
                parser.parse(dataInput, recordLength, x -> {
                    this.record = x;
                });
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        }
    }
}