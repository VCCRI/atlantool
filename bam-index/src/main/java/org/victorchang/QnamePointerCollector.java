package org.victorchang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class QnamePointerCollector implements BamRecordHandler {
    private static final Logger log = LoggerFactory.getLogger(QnamePointerCollector.class);

    private final QnameParser parser;
    private final KeyPointerBufferFlusher flusher;

    private final KeyPointerBufferPool bufferPool;
    private final ExecutorService executorService;
    private final List<Future<Integer>> pendingTasks;

    private KeyPointerBuffer currentBuffer;

    public QnamePointerCollector(KeyPointerBufferPool bufferPool,
                                 ExecutorService executorService,
                                 QnameParser parser,
                                 KeyPointerBufferFlusher flusher) {
        this.bufferPool = bufferPool;
        this.executorService = executorService;
        this.parser = parser;
        this.flusher = flusher;

        currentBuffer = bufferPool.getBuffer();
        pendingTasks = new ArrayList<>();
    }

    @Override
    public void onAlignmentRecord(long coffset, int uoffset, DataInput dataInput, int recordLength) {
        if (currentBuffer.size() >= currentBuffer.capacity()) {
            flush();
        }
        try {
            parser.parse(dataInput, recordLength, qname ->
                    currentBuffer.add(new KeyPointer(coffset, uoffset, qname.qname, qname.qnameLen)));
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
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
