package org.victorchang;

@FunctionalInterface
public interface QnamePosFlusher {
    void flush(QnamePosBuffer buffer);
}
