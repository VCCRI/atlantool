package org.victorchang;

import java.io.DataInput;
import java.io.IOException;
import java.util.function.Consumer;

public class QnameParser implements BamRecordParser<QnameRecord> {
    private static final int QNAME_SIZE = 256;

    /**
     * reuse mutable object to minimize memory allocation.
     */
    private final QnameRecord current;

    public QnameParser() {
        current = new QnameRecord(QNAME_SIZE);
    }

    @Override
    public void parse(DataInput dataInput, int recordLength, Consumer<QnameRecord> consumer) throws IOException {
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
        consumer.accept(current);
    }

    private void readQname(DataInput dataInput, int qnameLen) throws IOException {
        if (qnameLen >= 256) {
            throw new IllegalStateException("qname must be less than 256 bytes");
        }
        dataInput.readFully(current.qname, 0, qnameLen);
        current.qnameLen = qnameLen - 1;
    }
}
