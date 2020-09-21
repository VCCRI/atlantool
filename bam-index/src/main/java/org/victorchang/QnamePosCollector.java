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

    private final KeyPointerBufferPool bufferPool;
    private final ExecutorService executorService;
    private final List<Future<Integer>> pendingTasks;

    private KeyPointerBuffer currentBuffer;

    private long coffset;
    private int uoffset;

    public QnamePosCollector(KeyPointerBufferPool bufferPool, ExecutorService executorService, QnamePosFlusher flusher) {
        this.bufferPool = bufferPool;
        this.executorService = executorService;
        this.flusher = flusher;

        currentBuffer = bufferPool.getBuffer();
        pendingTasks = new ArrayList<>();
    }

    @Override
    public void onRecord(long blockPos, int offset) {
        this.coffset = blockPos;
        this.uoffset = offset;
    }

    @Override
    public void onQname(byte[] qnameBuffer, int qnameLen) {
        if (currentBuffer.size() >= currentBuffer.capacity()) {
            flush();
        }
        currentBuffer.add(new KeyPointer(coffset, uoffset, qnameBuffer, qnameLen));
    }

    @Override
    public void onSequence(byte[] seqBuffer, int seqLen) {
    }

    private void flush() {
        KeyPointerBuffer busyBuffer = currentBuffer;
        currentBuffer = bufferPool.getBuffer();
        Future<Integer> flushingTask = executorService.submit(() -> {
            try {
                flusher.flush(busyBuffer);
                busyBuffer.release();
                return busyBuffer.size();
            } catch (Throwable e) {
                log.error("Error when flushing", e);
                System.exit(-1);
            }
            return 0;
        });
        pendingTasks.add(flushingTask);
    }

    public void await() {
        if (currentBuffer.size() > 0) {
            flush();
            currentBuffer.release();
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
