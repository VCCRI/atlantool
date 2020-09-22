package org.victorchang;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;

/**
 * Callbacks that are invoked by {@link BamFileReader}.
 */
public interface BamRecordHandler {

    void onHeader(SAMFileHeader header);

    void onAlignmentPosition(long blockPos, int offset);

    void onQname(byte[] qnameBuffer, int qnameLen);

    void onSequence(byte[] seqBuffer, int seqLen);

    void onAlignmentRecord(SAMRecord record);
}
