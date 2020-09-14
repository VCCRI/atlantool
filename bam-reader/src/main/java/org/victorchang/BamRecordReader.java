package org.victorchang;

import java.io.IOException;
import java.nio.file.Path;

public interface BamRecordReader {
    void read(Path path, long blockPos, int recordNum, BamRecordHandler recordHandler) throws IOException;
}
