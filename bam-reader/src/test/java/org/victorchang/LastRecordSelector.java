package org.victorchang;

import java.io.DataInput;
import java.io.IOException;

public class LastRecordSelector<T> implements BamRecordHandler {
    private final BamRecordParser<T> recordParser;

    private T last;

    LastRecordSelector(BamRecordParser<T> recordParser) {
        this.recordParser = recordParser;
    }

    public T getLast() {
        return last;
    }

    @Override
    public void onAlignmentRecord(long coffset, int uoffset, DataInput dataInput, int recordLength) {
        try {
            recordParser.parse(dataInput, recordLength, record -> last = record);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }
}
