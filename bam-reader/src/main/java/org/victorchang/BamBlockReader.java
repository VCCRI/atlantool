package org.victorchang;

import java.nio.ByteBuffer;

public interface BamBlockReader {
    void read(ByteBuffer buffer, BamRecordHandler recordHandler, boolean header);
}