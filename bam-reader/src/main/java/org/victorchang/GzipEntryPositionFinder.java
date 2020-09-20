package org.victorchang;

public class GzipEntryPositionFinder implements GzipEntryEventHandler {
    private GzipEntryPosition tail;

    public GzipEntryPositionFinder() {
        tail = null;
    }

    public GzipEntryPosition find(long uncompressed) {
        for (GzipEntryPosition current = tail; current != null; current = current.getPrevious()) {
            if (uncompressed >= current.getUncompressed()) {
                current.setPrevious(null);
                return current;
            }
        }

        return null;
    }

    @Override
    public void onStart(long compressedCount, long uncompressedCount) {
        tail = new GzipEntryPosition(tail, compressedCount, uncompressedCount);
    }
}
