package org.victorchang;

public class GzipEntryPosition {
    private GzipEntryPosition previous;

    private final long compressed;
    private final long uncompressed;

    public GzipEntryPosition(GzipEntryPosition previous, long compressed, long uncompressed) {
        this.previous = previous;
        this.compressed = compressed;
        this.uncompressed = uncompressed;
    }

    public long getCompressed() {
        return compressed;
    }

    public long getUncompressed() {
        return uncompressed;
    }

    public void setPrevious(GzipEntryPosition previous) {
        this.previous = previous;
    }

    public GzipEntryPosition getPrevious() {
        return previous;
    }
}
