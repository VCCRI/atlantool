package org.victorchang;

/**
 * A call back that is invoked when starting a new member of Gzip.
 */
@FunctionalInterface
public interface GzipEntryEventHandler {
    void onStart(long compressedCount, long uncompressedCount);
}
