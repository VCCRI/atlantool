package org.victorchang;

@FunctionalInterface
public interface QnamePosFlusher {
    void flush(KeyPointerBuffer buffer);
}
