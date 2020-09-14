package org.victorchang;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BamRecordReaderTest {
    private static final Logger log = LoggerFactory.getLogger(BamFileReaderTest.class);

    @Test
    public void testReadSample1() throws IOException, URISyntaxException {
        URL example1 = ClassLoader.getSystemResource("bam/example1");
        Path path = Paths.get(example1.toURI());

        BamRecordReader recordReader = new DefaultBamRecordReader(new DefaultGzipBlockAssembler(), new DefaultBamBlockReader());

        long start = System.nanoTime();
        recordReader.read(path, 22720L, 1, new TestRecordHandler());
        recordReader.read(path, 4531815L, 108, new TestRecordHandler());
        long finish = System.nanoTime();

        log.info(String.format("duration %d ms\n", (finish - start) / 1000_000));
    }

    private static class TestRecordHandler implements BamRecordHandler {
        @Override
        public void onRecord(ByteBuffer byteBuffer, int byteLen, int recordNum) {
        }

        @Override
        public void onQname(ByteBuffer byteBuffer, int byteLen) {
            byte[] qname = new byte[byteLen];
            byteBuffer.get(qname, 0, byteLen);
            String decoded = Ascii7Decoder.INSTANCE.decode(qname, 0, qname.length);
            log.debug(String.format("qname %s\n", decoded));
        }

        @Override
        public void onSequence(ByteBuffer byteBuffer, int fieldLen) {
            int byteLen = (fieldLen + 1) /2;
            byte[] seq = new byte[byteLen];
            byteBuffer.get(seq, 0, byteLen);
            String decoded = SeqDecoder.INSTANCE.decode(seq, 0, fieldLen);
            log.debug(String.format("seq %s\n", decoded));
        }
    }
}