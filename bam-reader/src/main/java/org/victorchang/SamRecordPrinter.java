package org.victorchang;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTextHeaderCodec;
import htsjdk.samtools.SAMTextWriter;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

public class SamRecordPrinter {

    private final Writer writer;
    private final SAMTextWriter samWriter;

    private boolean printHeader;

    public SamRecordPrinter(OutputStream outputStream, boolean printHeader) {
        this.writer = new PrintWriter(outputStream);
        this.samWriter = new SAMTextWriter(outputStream);
        this.printHeader = printHeader;
    }

    public SamRecordPrinter(OutputStream outputStream) {
        this(outputStream, false);
    }

    public void print(SAMRecord record) {
        if (printHeader) {
            new SAMTextHeaderCodec().encode(writer, record.getHeader());
            printHeader = false;
        }
        samWriter.writeAlignment(record);
    }

    public void finish() {
        samWriter.finish();
    }
}
