package org.victorchang;

public class QnameRecord {
    public byte[] qname;
    public int qnameLen;

    public QnameRecord(int capacity) {
        qname = new byte[capacity];
        qnameLen = 0;
    }
}
