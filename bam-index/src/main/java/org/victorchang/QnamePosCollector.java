package org.victorchang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class QnamePosCollector implements BamRecordHandler {
    private static final Logger log = LoggerFactory.getLogger(QnamePosCollector.class);

    private final QnamePosFlusher flusher;

    private final QnamePosBufferPool bufferPool;
    private final ExecutorService executorService;
    private final List<Future<Integer>> pendingTasks;

    private QnamePosBuffer currentBuffer;

    private long blockPos;
    private int offset;


    public QnamePosCollector(QnamePosBufferPool bufferPool, ExecutorService executorService, QnamePosFlusher flusher) {
        this.bufferPool = bufferPool;
        this.executorService = executorService;
        this.flusher = flusher;

        currentBuffer = bufferPool.getBuffer();
        pendingTasks = new ArrayList<>();
    }

    @Override
    public void onRecord(long blockPos, int offset) {
        this.blockPos = blockPos;
        this.offset = offset;
    }

    @Override
    public void onQname(byte[] qnameBuffer, int qnameLen) {
        if (currentBuffer.size() >= currentBuffer.capacity()) {
            flush();
        }
        currentBuffer.add(new QnamePos(blockPos, offset, qnameBuffer, qnameLen));
    }

    @Override
    public void onSequence(byte[] seqBuffer, int seqLen) {
    }

    private void flush() {
        QnamePosBuffer busyBuffer = currentBuffer;
        currentBuffer = bufferPool.getBuffer();
        Future<Integer> flushingTask = executorService.submit(() -> {
            flusher.flush(busyBuffer);
            busyBuffer.release();
            return busyBuffer.size();
        });
        pendingTasks.add(flushingTask);
    }

    public void await() {
        if (currentBuffer.size() > 0) {
            flush();
        }
        for (Future<Integer> task : pendingTasks) {
            try {
                task.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
