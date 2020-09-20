package org.victorchang;

import java.util.Arrays;
import java.util.Iterator;

public final class KeyPointerBuffer implements Iterable<KeyPointer> {
    private final KeyPointerBufferPool pool;
    private final KeyPointer[] buffer;
    private int size;

    public KeyPointerBuffer(KeyPointerBufferPool pool, int buffSize) {
        this.pool = pool;
        buffer = new KeyPointer[buffSize];
        size = 0;
    }

    public void add(KeyPointer qnamePos) {
        buffer[size] = qnamePos;
        size++;
    }

    public int capacity() {
        return buffer.length;
    }

    public int size() {
        return size;
    }

    public void sort() {
        Arrays.sort(buffer, 0, size);
    }

    @Override
    public Iterator<KeyPointer> iterator() {
        return new Iterator<>() {
            private int i = 0;
            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public KeyPointer next() {
                return i < size ? buffer[i++] : null;
            }
        };
    }

    void release() {
        size = 0;
        Arrays.fill(buffer, null);
        pool.release(this);
    }
}
