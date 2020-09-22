package org.victorchang;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTextHeaderCodec;
import htsjdk.samtools.SAMTextWriter;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

public class SamPrintingHandler implements BamRecordHandler {

    private final Writer writer;

    private final SAMTextWriter samWriter;

    private final boolean printHeader;

    public SamPrintingHandler(OutputStream outputStream, boolean printHeader) {
        this.writer = new PrintWriter(outputStream);
        this.samWriter = new SAMTextWriter(outputStream);
        this.printHeader = printHeader;
    }

    @Override
    public void onHeader(SAMFileHeader header) {
        if (printHeader) {
            new SAMTextHeaderCodec().encode(writer, header);
        }
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
