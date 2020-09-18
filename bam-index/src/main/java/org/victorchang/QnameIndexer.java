package org.victorchang;

import com.google.common.collect.Iterators;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QnameIndexer {
    private final BamFileReader bamFileReader;
    private final QnamePosWriter qnamePosWriter;
    private final QnamePosReader qnamePosReader;
    private final int maxRecord;

    private final int threadCount;

    private final ExecutorService executorService;
    private final  QnamePosBufferPool bufferPool;


    public QnameIndexer(BamFileReader bamFileReader,
                        QnamePosWriter qnamePosWriter,
                        QnamePosReader qnamePosReader,
                        int threadCount,
                        int maxRecord) {
        this.bamFileReader = bamFileReader;
        this.qnamePosWriter = qnamePosWriter;
        this.qnamePosReader = qnamePosReader;
        this.threadCount = threadCount;
        this.maxRecord = maxRecord;

        executorService = Executors.newFixedThreadPool(threadCount);
        bufferPool = new QnamePosBufferPool(threadCount, maxRecord);
    }

    public long createIndex(Path indexFolder, Path bamFile) throws IOException {
        return createIndex(indexFolder, bamFile, Long.MAX_VALUE);
    }

    @SuppressWarnings("UnstableApiUsage")
    public long createIndex(Path indexFolder, Path bamFile, long limit) throws IOException {

        FileStore qnameStore = new DefaultFileStore(indexFolder, "qname", "sorted");
        SortedQnameFileFactory qnameFileFactory = new SortedQnameFileFactory(qnameStore, qnamePosWriter);

        QnamePosCollector collector = new QnamePosCollector(bufferPool, executorService, qnameFileFactory::create);
        long recordCount = bamFileReader.read(bamFile, collector, limit);
        collector.await();

        List<Stream<QnamePos>> sorted = qnameStore.list().stream()
                .map(qnamePosReader::read)
                .collect(Collectors.toList());

        Iterator<QnamePos> merged = Iterators.mergeSorted(sorted.stream()
                .map(BaseStream::iterator)
                .collect(Collectors.toList()), QnamePos::compareTo);

        FileStore fstStore = new DefaultFileStore(indexFolder, "qname", "fst");
        QnameFstBuilder fstBuilder = new QnameFstBuilder(bufferPool, executorService, fstStore);
        fstBuilder.build(merged);
        fstBuilder.await();

        Path rangesPath = indexFolder.resolve("ranges.sorted");
        List<QnamePos> ranges = fstBuilder.getQnameRanges();
        QnamePosBuffer rangesBuffer = bufferPool.getBuffer();
        for (QnamePos x : ranges) {
            rangesBuffer.add(x);
        }
        qnamePosWriter.create(rangesPath, rangesBuffer);

        sorted.forEach(BaseStream::close);

        qnameStore.deleteAll();

        return recordCount;
    }
}
