package org.victorchang;

public class QnameSeqRecord {
    public byte[] qname;
    int qnameLen;
    public byte[] seq;
    public int seqLen;

    public QnameSeqRecord(int qnameCapacity, int seqCapacity) {
        qname = new byte[qnameCapacity];
        seq = new byte[seqCapacity];
    }
}
