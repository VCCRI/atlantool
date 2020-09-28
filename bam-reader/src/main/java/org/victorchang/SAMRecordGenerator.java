package org.victorchang;

import htsjdk.samtools.SAMRecord;

import java.io.DataInput;
import java.io.IOException;
import java.util.function.Consumer;

public class SAMRecordGenerator implements BamRecordHandler {
    private final SAMRecordParser parser;
    private final Consumer<SAMRecord> consumer;

    public SAMRecordGenerator(SAMRecordParser parser, Consumer<SAMRecord> consumer) {
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
