package org.victorchang;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.function.Consumer;

final class QnamePosBuffer {
    private final QnamePosBufferPool pool;
    private final QnamePos[] buffer;
    private int size;

    /**
     * reusable buffer to minimize memory allocation.
     */
    private final byte[] outputBuff;

    QnamePosBuffer(QnamePosBufferPool pool, int buffSize) {
        this.pool = pool;
        buffer = new QnamePos[buffSize];
        outputBuff = new byte[256 + 8 + 2];
        size = 0;
    }

    void add(QnamePos qnamePos) {
        buffer[size] = qnamePos;
        size++;
    }

    int capacity() {
        return buffer.length;
    }

    int size() {
        return size;
    }

    void writeSorted(OutputStream outputStream) throws IOException {
        Arrays.sort(buffer, 0, size);
        for (int i = 0; i < size; i++) {
            QnamePos current = buffer[i];
            byte[] qnameBuff = current.getQname();
            long position = current.getPosition();
            int len = QnamePosPacker.INSTANCE.pack(outputBuff, qnameBuff, qnameBuff.length, position);
            outputStream.write(outputBuff, 0, len);
        }
    }

    void forEach(Consumer<QnamePos> consumer) {
        for (int i = 0; i < size; i++) {
            QnamePos current = buffer[i];
            consumer.accept(current);
        }
    }

    void release() {
        size = 0;
        Arrays.fill(buffer, null);
        pool.release(this);
    }
}
