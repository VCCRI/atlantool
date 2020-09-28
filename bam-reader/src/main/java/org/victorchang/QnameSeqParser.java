package org.victorchang;

import java.io.DataInput;
import java.io.IOException;
import java.util.function.Consumer;

public class QnameSeqParser implements BamRecordParser<QnameSeqRecord> {
    private static final int QNAME_SIZE = 256;
    private static final int SEQ_SIZE = 256;

    /**
     * reuse mutable object to minimize memory allocation.
     */
    private final QnameSeqRecord current;

    public QnameSeqParser() {
        current = new QnameSeqRecord(QNAME_SIZE, (SEQ_SIZE + 1) / 2);
    }

    @Override
    public void parse(DataInput dataInput, int recordLength, Consumer<QnameSeqRecord> consumer) throws IOException {
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

        int cigarLen = 4 * cigarCount; // skip cigar
        while (cigarLen > 0) {
            cigarLen -= dataInput.skipBytes(cigarLen);
        }

        readSeq(dataInput, seqLen);

        consumer.accept(current);
    }

    private void readQname(DataInput dataInput, int qnameLen) throws IOException {
        if (qnameLen >= 256) {
            throw new IllegalStateException("qname must be less than 256 bytes");
        }
        dataInput.readFully(current.qname, 0, qnameLen);
        current.qnameLen = qnameLen - 1; // remove \x0 terminated;
    }

    private void readSeq(DataInput dataInput, int seqLength) throws IOException {
        int byteLen = (seqLength + 1) / 2;
        // we can't make any assumption on sequence length so grow the buffer if it is required.
        while (byteLen > current.seq.length) {
            current.seq = new byte[Math.max(current.seq.length * 2, byteLen)];
        }
        dataInput.readFully(current.seq, 0, byteLen);
        current.seqLen = seqLength;
    }
}
