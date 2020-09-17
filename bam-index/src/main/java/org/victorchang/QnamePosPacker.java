package org.victorchang;

/**
 * Packs a qname and position into a byte array.
 */
public final class QnamePosPacker {
    public static final QnamePosPacker INSTANCE = new QnamePosPacker();

    private QnamePosPacker() {
    }

    /**
     * Packs qname and position into a byte array.
     *
     * @param outputBuff output buffer
     * @param qnameBuff  buffer where qname is stored
     * @param qnameLen   length of qname in the qname buffer in bytes
     * @param position   encoded position
     * @return length of encoded data in output buffer
     */
    public int pack(byte[] outputBuff, byte[] qnameBuff, int qnameLen, long position) {
        if (qnameLen >= 256) {
            throw new IllegalStateException("qname must be less than 256 bytes");
        }
        int len = qnameLen + 8; // 8 bytes for position
        if (outputBuff.length < len + 2) { // 2 bytes for record length
            throw new IllegalStateException("outputBuff is too small");
        }

        // little endian encoded record length excluding the record length
        outputBuff[0] = (byte) len;
        outputBuff[1] = (byte) (len >> 8);

        System.arraycopy(qnameBuff, 0, outputBuff, 2, qnameLen);

        int size = len + 2;
        // little endian encoded position in the last 8 bytes
        outputBuff[size - 8] = (byte) position;
        outputBuff[size - 7] = (byte) (position >> 8);
        outputBuff[size - 6] = (byte) (position >> 16);
        outputBuff[size - 5] = (byte) (position >> 24);
        outputBuff[size - 4] = (byte) (position >> 32);
        outputBuff[size - 3] = (byte) (position >> 40);
        outputBuff[size - 2] = (byte) (position >> 48);
        outputBuff[size - 1] = (byte) (position >> 56);

        return size;
    }

    public long unpackPosition(byte[] buffer, int offset, int len) {
        int i = offset + len - 1;
        long position = ((long) buffer[i] << 56)
                | ((long) buffer[i - 1] & 0xff) << 48
                | ((long) buffer[i - 2] & 0xff) << 40
                | ((long) buffer[i - 3] & 0xff) << 32
                | ((long) buffer[i - 4] & 0xff) << 24
                | ((long) buffer[i - 5] & 0xff) << 16
                | ((long) buffer[i - 6] & 0xff) << 8
                | ((long) buffer[i - 7] & 0xff);
        return position;
    }

    public int unpackQname(byte[] buffer, int offset, int len, ByteConsumer byteConsumer) {
        for (int i = 0; i < len - 8; i++) {
            byteConsumer.accept(buffer[i + offset]);
        }
        return len - 8;
    }
}
