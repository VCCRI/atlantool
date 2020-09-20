package org.victorchang;

import java.util.ArrayList;
import java.util.List;

public class GzipEntryPositionCollector implements GzipEntryEventHandler {
    private final List<Long> compressed = new ArrayList<>();
    private final List<Long> uncompressed = new ArrayList<>();

    public List<Long> getCompressed() {
        return compressed;
    }

    public List<Long> getUncompressed() {
        return uncompressed;
    }

    @Override
    public void onStart(long compressedCount, long uncompressedCount) {
        compressed.add(compressedCount);
        uncompressed.add(uncompressedCount);
    }
}
