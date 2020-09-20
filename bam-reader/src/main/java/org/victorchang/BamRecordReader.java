package org.victorchang;

import java.io.IOException;
import java.nio.file.Path;

public interface BamRecordReader {
    void read(Path bamFile, long coffset, int uoffset, BamRecordHandler recordHandler) throws IOException;
    void read(Path bamFile, long pointer, BamRecordHandler recordHandler) throws IOException;
}
