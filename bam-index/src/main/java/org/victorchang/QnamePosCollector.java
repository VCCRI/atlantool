package org.victorchang;

import java.util.Arrays;

public class QnamePosCollector implements BamRecordHandler {

    private final QnamePosFlusher flusher;

    private final int maxRecord;
    private final QnamePos[] buffer;
    private int recordCount;

    private long blockPos;
    private int offset;

    public QnamePosCollector(QnamePosFlusher flusher, int maxRecord) {
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
    public void onQname(byte[] qnameBuffer, int qnameLen) {
        this.buffer[recordCount++] = new QnamePos(blockPos, offset, qnameBuffer, qnameLen);

        if (recordCount >= maxRecord) {
            flush();
        }
    }

    @Override
    public void onSequence(byte[] seqBuffer, int seqLen) {
    }

    public void flush() {
        if (recordCount > 0) {
            flusher.flush(buffer, recordCount);
            Arrays.fill(buffer, null);
            recordCount = 0;
        }
    }
}
