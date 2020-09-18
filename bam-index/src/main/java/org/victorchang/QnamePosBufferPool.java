package org.victorchang;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class QnamePosBufferPool {
    private final BlockingQueue<QnamePosBuffer> queue;

    public QnamePosBufferPool(int poolSize, int bufferSize) {
        queue = new ArrayBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            queue.offer(new QnamePosBuffer(this, bufferSize));
        }
    }

    public QnamePosBuffer getBuffer() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void release(QnamePosBuffer buffer) {
        try {
            queue.put(buffer);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
