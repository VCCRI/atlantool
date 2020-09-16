package org.victorchang;

/**
 * Callbacks that are invoked by {@link BamFileReader}.
 */
public interface BamRecordHandler {
    void onRecord(long blockPos, int offset);

    void onQname(byte[] bytes);

    void onSequence(byte[] bytes, int fieldLen);
}
