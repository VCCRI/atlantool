package org.victorchang;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMHeaderRecordComparator;

import java.io.DataInput;
import java.io.IOException;

public class EfficientBamRecordParser implements BamRecordParser {
    private static final int QNAME_SIZE = 256;
    private static final int SEQ_SIZE = 128;

    /**
     * reusable buffers to minimize memory allocation.
     */
    private final byte[] qnameBuffer;
    private byte[] seqBuffer;

    public EfficientBamRecordParser() {
        qnameBuffer = new byte[QNAME_SIZE];
        seqBuffer = new byte[SEQ_SIZE];
    }

    @Override
    public void parse(SAMFileHeader header, DataInput dataInput, int recordLength, BamRecordHandler handler) throws IOException {
        dataInput.readInt(); // reference seq id
        dataInput.readInt(); // pos
        int qnameLen = dataInput.readUnsignedByte();
        dataInput.readUnsignedByte(); // map q
        dataInput.readUnsignedShort(); // bin
        int cigarCount = dataInput.readUnsignedShort();
        dataInput.readUnsignedShort(); // flag
        int seqLen = dataInput.readInt();
        dataInput.readInt(); // next ref id
        dataInput.readInt(); // next pos
        dataInput.readInt(); // template len

        readQname(dataInput, qnameLen);
        handler.onQname(qnameBuffer, qnameLen);

        int cigarLen = 4 * cigarCount; // skip cigar
        while (cigarLen > 0) {
            cigarLen -= dataInput.skipBytes(cigarLen);
        }

        readSeq(dataInput, seqLen);
        handler.onSequence(seqBuffer, seqLen);
    }

    private void readQname(DataInput dataInput, int qnameLen) throws IOException {
        if (qnameLen >= 256) {
            throw new IllegalStateException("qname must be less than 256 bytes");
        }
        dataInput.readFully(qnameBuffer, 0, qnameLen);
    }

    private void readSeq(DataInput dataInput, int seqLength) throws IOException {
        int byteLen = (seqLength + 1) / 2;
        // we can't make any assumption on sequence length so grow the buffer if it is required.
        while (byteLen > seqBuffer.length) {
            seqBuffer = new byte[Math.max(seqBuffer.length * 2, byteLen)];
        }
        dataInput.readFully(seqBuffer, 0, byteLen);
    }
}
