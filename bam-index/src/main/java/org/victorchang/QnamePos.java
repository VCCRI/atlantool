package org.victorchang;

import java.util.Arrays;

public class QnamePos implements Comparable<QnamePos> {
    private final long position;
    private final byte[] qname;

    public QnamePos(long blockPos, int offset, byte[] qname) {
        this.qname = qname;
        position = PositionPacker.INSTANCE.pack(blockPos, offset);
    }

    public long getPosition() {
        return position;
    }

    public byte[] getQname() {
        return qname;
    }

    @Override
    public int compareTo(QnamePos that) {
        return Arrays.compare(this.qname, that.qname);
    }
}
