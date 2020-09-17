package org.victorchang;

/**
 * Callbacks that are invoked by {@link BamFileReader}.
 */
public interface BamRecordHandler {
    void onRecord(long blockPos, int offset);

    void onQname(byte[] qnameBuffer, int qnameLen);

    void onSequence(byte[] seqBuffer, int seqLen);
}
