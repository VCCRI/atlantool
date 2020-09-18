package org.victorchang;

public class GzipEntryPositionFinder implements GzipEntryEventHandler {
    private GzipEntryPosition tail;
    private int entryCount;

    public GzipEntryPositionFinder() {
        tail = null;
        entryCount = 0;
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
        entryCount++;
    }

    public int getEntryCount() {
        return entryCount;
    }
}
