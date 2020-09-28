package org.victorchang;

import java.io.DataInput;

/**
 * Callbacks that are invoked by {@link BamFileReader} and {@link BamRecordReader}.
 */
public interface BamRecordHandler {
    void onAlignmentRecord(long coffset, int uoffset, DataInput dataInput, int recordLength);
}
