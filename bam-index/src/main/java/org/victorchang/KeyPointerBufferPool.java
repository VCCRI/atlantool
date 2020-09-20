package org.victorchang;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class KeyPointerBufferPool {
    private final BlockingQueue<KeyPointerBuffer> queue;

    public KeyPointerBufferPool(int poolSize, int bufferSize) {
        queue = new ArrayBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            queue.offer(new KeyPointerBuffer(this, bufferSize));
        }
    }

    public KeyPointerBuffer getBuffer() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void release(KeyPointerBuffer buffer) {
        try {
            queue.put(buffer);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
