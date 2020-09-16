package org.victorchang;

import java.io.DataInput;
import java.io.IOException;

public class DefaultBamRecordParser implements BamRecordParser {
    @Override
    public void parse(DataInput dataInput, BamRecordHandler handler) throws IOException {
        dataInput.readInt(); // reference seq id
        dataInput.readInt(); // pos
        int qnameLen = dataInput.readUnsignedByte();
        dataInput.readUnsignedByte(); // map q
        dataInput.readUnsignedShort(); // bin
        int cigarCount = dataInput.readUnsignedShort();
        dataInput.readUnsignedShort(); // flag
        int seqLength = dataInput.readInt();
        dataInput.readInt(); // next ref id
        dataInput.readInt(); // next pos
        dataInput.readInt(); // template len

        byte[] qname = new byte[qnameLen];
        dataInput.readFully(qname);
        handler.onQname(qname);

        int cigarLen = 4 * cigarCount; // skip cigar
        while (cigarLen > 0) {
            cigarLen -= dataInput.skipBytes(cigarLen);
        }

        byte[] sequence = new byte[(seqLength + 1) / 2];
        dataInput.readFully(sequence);
        handler.onSequence(sequence, seqLength);
    }
}
