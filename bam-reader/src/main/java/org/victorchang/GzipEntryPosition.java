package org.victorchang;

public class GzipEntryPosition {
    private final long compressed;
    private final long uncompressed;

    public GzipEntryPosition(long compressed, long uncompressed) {
        this.compressed = compressed;
        this.uncompressed = uncompressed;
    }

    public long getCompressed() {
        return compressed;
    }

    public long getUncompressed() {
        return uncompressed;
    }
}
