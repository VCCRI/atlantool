package org.victorchang;

import java.io.DataInput;

public class RecordCounter implements BamRecordHandler {
    private long recordCount;

    public RecordCounter() {
        recordCount = 0;
    }

    public long getRecordCount() {
        return recordCount;
    }

    @Override
    public void onAlignmentRecord(long coffset, int uoffset, DataInput dataInput, int recordLength) {
        recordCount++;
    }
}
