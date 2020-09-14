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

public class BamFileReaderTest {
    private static final Logger log = LoggerFactory.getLogger(BamFileReaderTest.class);

    @Test
    public void testReadSample1() throws IOException, URISyntaxException {
        URL example1 = ClassLoader.getSystemResource("bam/example1");
        Path path = Paths.get(example1.toURI());

        BamFileReader fileReader = new DefaultBamFileReader(new DefaultGzipBlockAssembler(), new DefaultBamBlockReader());

        TestHandler handler = new TestHandler();
        long start = System.nanoTime();
        fileReader.read(path, handler);
        long finish = System.nanoTime();

        log.info(String.format("#blocks %d, #records %d, duration %d ms\n",
                handler.getBlockCount(), handler.getRecordCount(),  (finish - start) / 1000_000));
    }

    private static class TestHandler implements BamFileHandler {
        private long blockCount;
        private long recordCount;
        private long blockPos;
        private long recordNum;

        public TestHandler() {
            blockCount = 0;
            recordCount = 0;
        }

        @Override
        public void onBlock(ByteBuffer byteBuffer, long blockPos) {
            this.blockPos = blockPos;

            blockCount++;
            if (blockCount % 100 == 0) {
                log.debug(String.format("block pos %d\n", blockPos));
            }
        }

        @Override
        public void onRecord(ByteBuffer byteBuffer, int byteLen, int recordNum) {
            this.recordNum = recordNum;

            recordCount++;
            if (recordCount % 100_000 == 0) {
                log.debug(String.format("block pos %d, record num %d\n", blockPos, recordNum));
            }
        }

        @Override
        public void onQname(ByteBuffer byteBuffer, int byteLen) {
            if (recordCount % 100_000 == 0) {
                byte[] qname = new byte[byteLen];
                byteBuffer.get(qname, 0, byteLen);
                String decoded = Ascii7Decoder.INSTANCE.decode(qname, 0, qname.length);
                log.debug(String.format("block pos %d, record num %d, qname %s\n", blockPos, recordNum, decoded));
            }
        }

        @Override
        public void onSequence(ByteBuffer byteBuffer, int fieldLen) {
            if (recordCount % 100_000 == 0) {
                int byteLen = (fieldLen + 1) /2;
                byte[] seq = new byte[byteLen];
                byteBuffer.get(seq, 0, byteLen);
                String decoded = SeqDecoder.INSTANCE.decode(seq, 0, fieldLen);
                log.debug(String.format("block pos %d, record num %d, seq %s\n", blockPos, recordNum, decoded));
            }
        }

        public long getBlockCount() {
            return blockCount;
        }

        public long getRecordCount() {
            return recordCount;
        }
    }
}