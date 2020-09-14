package org.victorchang;

import java.nio.ByteBuffer;

/**
 * Callbacks that are invoked by {@link BamRecordReader}. Field data is available at the current position of the
 * {@link ByteBuffer}.
 */
public interface BamRecordHandler {
    void onRecord(ByteBuffer byteBuffer, int byteLen, int recordNum);

    void onQname(ByteBuffer byteBuffer, int byteLen);

    void onSequence(ByteBuffer byteBuffer, int fieldLen);
}
