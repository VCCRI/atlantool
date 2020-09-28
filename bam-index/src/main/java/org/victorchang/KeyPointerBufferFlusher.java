package org.victorchang;

@FunctionalInterface
public interface KeyPointerBufferFlusher {
    void flush(KeyPointerBuffer buffer);
}
