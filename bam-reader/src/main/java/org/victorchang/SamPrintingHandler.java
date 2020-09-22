package org.victorchang;

import htsjdk.samtools.BAMRecord;

public class SamPrintingHandler implements BamRecordHandler {
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
    public void onAlignmentRecord(BAMRecord record) {
    }
}
