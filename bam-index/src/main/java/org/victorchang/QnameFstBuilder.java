package org.victorchang;

import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class QnameFstBuilder {
    private static final Logger log = LoggerFactory.getLogger(QnameFstBuilder.class);

    private final QnamePosBufferPool bufferPool;
    private final ExecutorService executorService;
    private final List<Future<Integer>> pendingTasks;
    private final FileStore indexStore;

    private QnamePosBuffer currentBuffer;

    private final byte[] previousQname;
    private int previousLen;

    private QnamePos startRecord;
    private int occurrence;

    private final List<QnamePos> qnameRanges;

    public QnameFstBuilder(QnamePosBufferPool bufferPool, ExecutorService executorService, FileStore fstStore) {
        this.bufferPool = bufferPool;
        this.executorService = executorService;
        this.indexStore = fstStore;

        currentBuffer = bufferPool.getBuffer();
        pendingTasks = new ArrayList<>();
        qnameRanges = new ArrayList<>();
        previousQname = new byte[256];
    }

    public void build(Iterator<QnamePos> merged) {
        while (merged.hasNext()) {
            QnamePos qnamePos = merged.next();
            add(qnamePos);
        }
    }

    private void add(QnamePos currentRecord) {
        if (startRecord == null) {
            startRecord = currentRecord;
        }

        if (Arrays.equals(currentRecord.getQname(), 0, currentRecord.getQname().length - 1,
                previousQname, 0, previousLen)) {
            occurrence++; // duplicated key
            if (occurrence > 255) {
                throw new IllegalStateException("There is more than 256 records with the same qname");
            }
            currentRecord.setOccurrence(occurrence);
        } else {
            occurrence = 0;
            if (currentBuffer.size() > 0 && currentBuffer.size() + 256 >= currentBuffer.capacity()) {
                flush();
                startRecord = currentRecord;
            }
        }

        previousLen = currentRecord.getQname().length - 1;
        System.arraycopy(currentRecord.getQname(), 0, previousQname, 0, previousLen);
        currentBuffer.add(currentRecord);
    }

    public void flush() {
        QnamePosBuffer busyBuffer = currentBuffer;
        currentBuffer = bufferPool.getBuffer();
        Path path = indexStore.generate();
        log.info("Creating fst starting at {}",
                Ascii7Coder.INSTANCE.decode(startRecord.getQname(), 0, startRecord.getQname().length));
        Future<Integer> flushingTask = executorService.submit(() -> {
            IntsRefBuilder intsRefBuilder = new IntsRefBuilder();
            Builder<Long> fstBuilder = new Builder<>(FST.INPUT_TYPE.BYTE1, PositiveIntOutputs.getSingleton());
            busyBuffer.forEach(currentRecord -> {
                byte[] qname = currentRecord.getQname();
                long position = currentRecord.getPosition();
                try {
                    fstBuilder.add(createIntsRef(qname, intsRefBuilder), position);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            FST<Long> fst = fstBuilder.finish();
            fst.save(path);
            log.info("{} is created", path);

            busyBuffer.release();
            return busyBuffer.size();
        });
        pendingTasks.add(flushingTask);
        qnameRanges.add(startRecord);
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

    public List<QnamePos> getQnameRanges() {
        return qnameRanges;
    }

    private IntsRef createIntsRef(byte[] bytes, IntsRefBuilder intsRefBuilder) {
        intsRefBuilder.clear();
        for (byte b : bytes) {
            intsRefBuilder.append(b & 0xff);
        }
        return intsRefBuilder.get();
    }
}
