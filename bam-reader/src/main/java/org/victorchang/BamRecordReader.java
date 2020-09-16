package org.victorchang;

import java.io.IOException;
import java.nio.file.Path;

public interface BamRecordReader {
    void read(Path bamFile, long blockPos, int offset, BamRecordHandler recordHandler) throws IOException;
}
