package org.victorchang;

import java.util.Arrays;
import java.util.StringJoiner;

public class QnamePos implements Comparable<QnamePos> {
    private final long position;
    private final byte[] qname;

    public QnamePos(long blockPos, int offset, byte[] qnameBuffer, int qnameLen) {
        this.qname = new byte[qnameLen];
        System.arraycopy(qnameBuffer, 0, qname, 0, qnameLen);
        position = PositionPacker.INSTANCE.pack(blockPos, offset);
    }

    public QnamePos(long position, byte[] qnameBuffer, int qnameLen) {
        this.position = position;
        this.qname = new byte[qnameLen];
        System.arraycopy(qnameBuffer, 0, qname, 0, qnameLen);
    }

    public long getPosition() {
        return position;
    }

    public byte[] getQname() {
        return qname;
    }

    @Override
    public int compareTo(QnamePos that) {
        return Arrays.compareUnsigned(this.qname, that.qname);
    }

    public void setOccurrence(int k) {
        qname[qname.length - 1] = (byte)k;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ")
                .add("position=" + PositionPacker.INSTANCE.unpackBlockPos(position))
                .add("offset=" + PositionPacker.INSTANCE.unpackOffset(position))
                .add("qname=" + Ascii7Coder.INSTANCE.decode(qname, 0, qname.length))
                .add("occurrence=" + (qname[qname.length - 1] & 0xff))
                .toString();
    }
}
