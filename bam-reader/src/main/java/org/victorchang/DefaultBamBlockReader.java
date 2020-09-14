package org.victorchang;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DefaultBamBlockReader implements BamBlockReader {
    public static byte[] MAGIC = {(byte) 'B', (byte) 'A', (byte) 'M', (byte) 1};

    @Override
    public void read(ByteBuffer byteBuffer, BamRecordHandler recordHandler, boolean header) {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        if (header) {
            assertMagic(byteBuffer);
            skipHeaderText(byteBuffer);
            skipReferences(byteBuffer);
        }

        for (int recordNum = 0; byteBuffer.remaining() > 0; recordNum++) {
            int recordLength = byteBuffer.getInt();
            int startRecord = byteBuffer.position();

            int markPos = byteBuffer.position();
            recordHandler.onRecord(byteBuffer, recordLength, recordNum);
            byteBuffer.position(markPos);

            byteBuffer.getInt(); // reference seq id
            byteBuffer.getInt(); // pos
            int qnameLen = byteBuffer.get() & 0xFF; // 1 unsigned byte
            byteBuffer.get(); // map q
            byteBuffer.getShort(); // bin
            int cigarCount = byteBuffer.getShort() & 0xFFFF; // 1 unsigned short
            byteBuffer.getShort(); // flag
            int seqLength = byteBuffer.getInt();
            byteBuffer.getInt(); // next ref id
            byteBuffer.getInt(); // next pos
            byteBuffer.getInt(); // template len

            markPos = byteBuffer.position();
            recordHandler.onQname(byteBuffer, qnameLen);
            byteBuffer.position(markPos + qnameLen);

            byteBuffer.position(byteBuffer.position() + 4 * cigarCount); // cigar

            markPos = byteBuffer.position();
            recordHandler.onSequence(byteBuffer, seqLength);
            byteBuffer.position(markPos + (seqLength + 1) / 2);

            byteBuffer.position(startRecord + recordLength);
        }
    }

    private void assertMagic(ByteBuffer byteBuffer) {
        int b0 = byteBuffer.get();
        int b1 = byteBuffer.get();
        int b2 = byteBuffer.get();
        int b3 = byteBuffer.get();

        if (b0 != MAGIC[0] || b1 != MAGIC[1] || b2 != MAGIC[2] || b3 != MAGIC[3]) {
            throw new IllegalStateException("Invalid BAM magic");
        }
    }

    private void skipHeaderText(ByteBuffer byteBuffer) {
        int len = byteBuffer.getInt();
        byteBuffer.position(byteBuffer.position() + len);
    }

    private void skipReferences(ByteBuffer byteBuffer) {
        int refCount = byteBuffer.getInt();
        for (int i = 0; i < refCount; i++) {
            int len = byteBuffer.getInt();
            byteBuffer.position(byteBuffer.position() + len);
            byteBuffer.position(byteBuffer.position() + 4);
        }
    }
}
