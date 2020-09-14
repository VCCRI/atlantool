package org.victorchang;

import java.nio.ByteBuffer;

public class DefaultGzipBlockAssembler implements GzipBlockAssembler {
    private static final int INITIAL_BUFFER_SIZE = 8192;

    private byte[] current;
    private int pos;

    public DefaultGzipBlockAssembler() {
        current = new byte[INITIAL_BUFFER_SIZE];
        pos = 0;
    }

    @Override
    public void append(byte[] buffer, int len) {
        while (pos + len > current.length) { // overflow
            byte[] next = new byte[current.length << 1];
            System.arraycopy(current, 0, next, 0, current.length);
            current = next;
        }
        System.arraycopy(buffer, 0, current, pos, len);
        pos += len;
    }

    @Override
    public ByteBuffer buffer() {
        return ByteBuffer.wrap(current, 0, pos);
    }

    @Override
    public int length() {
        return pos;
    }

    @Override
    public void reset() {
        pos = 0;
    }
}
