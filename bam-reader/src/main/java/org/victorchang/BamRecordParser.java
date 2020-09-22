package org.victorchang;

import htsjdk.samtools.SAMFileHeader;

import java.io.DataInput;
import java.io.IOException;

public interface BamRecordParser {
    void parse(SAMFileHeader header, DataInput dataInput, int recordLength, BamRecordHandler handler) throws IOException;
}
