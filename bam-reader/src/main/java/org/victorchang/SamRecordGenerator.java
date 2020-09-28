package org.victorchang;

import htsjdk.samtools.SAMRecord;

import java.io.DataInput;
import java.io.IOException;
import java.util.function.Consumer;

public class SamRecordGenerator implements BamRecordHandler {
    private final SamRecordParser parser;
    private final Consumer<SAMRecord> consumer;

    public SamRecordGenerator(SamRecordParser parser, Consumer<SAMRecord> consumer) {
        this.parser = parser;
        this.consumer = consumer;
    }

    @Override
    public void onAlignmentRecord(long coffset, int uoffset, DataInput dataInput, int recordLength) {
        try {
            parser.parse(dataInput, recordLength, consumer);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }
}
