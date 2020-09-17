package org.victorchang;

import java.util.Arrays;
import java.util.function.BiConsumer;

public class QnameCollector implements BamRecordHandler {

    private final BiConsumer<QnamePos[], Integer> flusher;

    private final int maxRecord;
    private final QnamePos[] buffer;
    private int recordCount;

    private long blockPos;
    private int offset;

    public QnameCollector(BiConsumer<QnamePos[], Integer> flusher, int maxRecord) {
        this.flusher = flusher;
        this.maxRecord = maxRecord;
        this.buffer = new QnamePos[maxRecord];
        this.recordCount = 0;
    }

    @Override
    public void onRecord(long blockPos, int offset) {
        this.blockPos = blockPos;
        this.offset = offset;
    }

    @Override
    public void onQname(byte[] bytes) {
        buffer[recordCount++] = new QnamePos(blockPos, offset, bytes);

        if (recordCount >= maxRecord) {
            flush();
        }
    }

    @Override
    public void onSequence(byte[] bytes, int fieldLen) {
    }

    public void flush() {
        if (recordCount > 0) {
            flusher.accept(buffer, recordCount);
            Arrays.fill(buffer, null);
            recordCount = 0;
        }
    }
}
