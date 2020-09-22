package org.victorchang;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTextWriter;

import java.io.OutputStream;

public class SamPrintingHandler implements BamRecordHandler {

    private final SAMTextWriter samWriter;

    public SamPrintingHandler(OutputStream outputStream) {
        this.samWriter = new SAMTextWriter(outputStream);
    }

    @Override
    public void onAlignmentPosition(long blockPos, int offset) {
    }

    @Override
    public void onQname(byte[] qnameBuffer, int qnameLen) {
    }

    @Override
    public void onSequence(byte[] seqBuffer, int seqLen) {
    }

    @Override
    public void onAlignmentRecord(SAMRecord record) {
        samWriter.writeAlignment(record);
    }

    public void finish() {
        samWriter.finish();
    }
}
