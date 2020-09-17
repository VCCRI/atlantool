package org.victorchang;

import java.util.ArrayList;
import java.util.List;

public class GzipEntryPositionFinder implements GzipEntryEventHandler {
    private final List<GzipEntryPosition> positions;

    public GzipEntryPositionFinder() {
        this.positions = new ArrayList<>();
    }

    public GzipEntryPosition find(long uncompressed) {
        for (int i = positions.size() - 1; i >= 0; i--) {
            GzipEntryPosition current = positions.get(i);
            if (uncompressed >= current.getUncompressed()) {
                return current;
            }
        }

        return null;
    }

    @Override
    public void onStart(long compressedCount, long uncompressedCount) {
        positions.add(new GzipEntryPosition(compressedCount, uncompressedCount));
    }
}
