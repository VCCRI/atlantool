package org.victorchang;

import java.io.IOException;
import java.nio.file.Path;

public interface BamFileReader {
    /**
     * Reads all records in the file.
     */
    long read(Path bamFile, BamRecordHandler handler) throws IOException;

    /**
     * Reads up to {@code limit} records.
     */
    long read(Path bamFile, BamRecordHandler handler, long limit) throws IOException;
}
