package org.victorchang;

import htsjdk.samtools.BAMRecord;
import htsjdk.samtools.DefaultSAMRecordFactory;
import htsjdk.samtools.SAMFileHeader;
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

        final BAMRecord bamRecord = samRecordFactory.createBAMRecord(
                header,
                referenceSeqId,
                pos,
                (short) qnameLen,
                (short) mapq,
                bin,
                cigarCount,
                flag,
                seqLen,
                nextRefId,
                nextPos,
                templateLen,
                restOfData);

        handler.onQname(bamRecord.getReadName().getBytes(), qnameLen);
        handler.onSequence(bamRecord.getReadBases(), seqLen);
        handler.onAlignmentRecord(bamRecord);
    }

}
