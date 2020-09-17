package org.victorchang;

@FunctionalInterface
public interface QnamePosFlusher {
    void flush(QnamePos[] buffer, int recordCount);
}
