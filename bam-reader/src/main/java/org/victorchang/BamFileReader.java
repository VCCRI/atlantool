package org.victorchang;

import java.io.IOException;
import java.nio.file.Path;

public interface BamFileReader {
    void read(Path bamFile, BamRecordHandler handler) throws IOException;
}
