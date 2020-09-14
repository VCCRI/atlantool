package org.victorchang;

import java.nio.ByteBuffer;

public interface GzipBlockAssembler {
    void append(byte[] buffer, int len);
    ByteBuffer buffer();
    int length();
    void reset();
}
