package org.victorchang;

import htsjdk.samtools.DefaultSAMRecordFactory;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordFactory;

import java.io.DataInput;
import java.io.IOException;

public class SamtoolsBasedParser implements BamRecordParser {

    private final SAMRecordFactory samRecordFactory = new DefaultSAMRecordFactory();

    @Override
    public void parse(SAMFileHeader header, DataInput dataInput, int recordLength, BamRecordHandler handler) throws IOException {
        final int referenceSeqId = dataInput.readInt();// reference seq id
        final int pos = dataInput.readInt();// pos
        final int qnameLen = dataInput.readUnsignedByte();
        final int mapq = dataInput.readUnsignedByte();// map q
        final int bin = dataInput.readUnsignedShort();// bin
        final int cigarCount = dataInput.readUnsignedShort();
        final int flag = dataInput.readUnsignedShort();// flag
        final int seqLen = dataInput.readInt();
        final int nextRefId = dataInput.readInt();// next ref id
        final int nextPos = dataInput.readInt();// next pos
        final int templateLen = dataInput.readInt();// template len
        final byte[] restOfData = new byte[recordLength - 32]; // Excluding the data we've read
        dataInput.readFully(restOfData);

        final SAMRecord samRecord = samRecordFactory.createBAMRecord(
                header,
                referenceSeqId,
                pos + 1, // In SAM specification this is 1-based
                (short) qnameLen,
                (short) mapq,
                bin,
                cigarCount,
                flag,
                seqLen,
                nextRefId,
                nextPos + 1, // In SAM specification this is 1-based
                templateLen,
                restOfData);

        handler.onQname(samRecord.getReadName().getBytes(), qnameLen - 1); // subtract 1 for \x0 terminated
        handler.onSequence(samRecord.getReadBases(), seqLen);
        handler.onAlignmentRecord(samRecord);
    }

}
