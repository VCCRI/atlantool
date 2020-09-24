package org.victorchang;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class QnameIndexer {
    private static final Logger log = LoggerFactory.getLogger(QnameIndexer.class);

    private final BamFileReader bamFileReader;
    private final KeyPointerWriter keyPointerWriter;
    private final KeyPointerReader keyPointerReader;

    private final ExecutorService executorService;
    private final KeyPointerBufferPool bufferPool;

    public QnameIndexer(BamFileReader bamFileReader,
                        KeyPointerWriter keyPointerWriter,
                        KeyPointerReader keyPointerReader,
                        int threadCount,
                        int bufferSize) {
        this.bamFileReader = bamFileReader;
        this.keyPointerWriter = keyPointerWriter;
        this.keyPointerReader = keyPointerReader;

        executorService = Executors.newFixedThreadPool(threadCount);
        bufferPool = new KeyPointerBufferPool(threadCount + 1, bufferSize);
    }

    public long index(Path indexFolder, Path bamFile, Path tempDir) throws IOException {
        return index(indexFolder, bamFile, tempDir, Long.MAX_VALUE);
    }

    @SuppressWarnings("UnstableApiUsage")
    public long index(Path indexDir, Path bamFile, Path tempDir, long bytesLimit) throws IOException {

        FileStore qnameStore = new DefaultFileStore(tempDir, "qname", "part");
        SortedQnameFileFactory qnameFileFactory = new SortedQnameFileFactory(qnameStore, keyPointerWriter);

        QnamePosCollector collector = new QnamePosCollector(bufferPool, executorService, buffer -> {
            try {
                qnameFileFactory.create(buffer);
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        });

        long recordCount = bamFileReader.read(bamFile, collector, bytesLimit);
        collector.await();

        List<Stream<KeyPointer>> parts = qnameStore.list().stream()
                .map(path -> {
                    InputStream inputStream;
                    try {
                        FileChannel fileChannel = FileChannel.open(path, READ);
                        inputStream = Channels.newInputStream(fileChannel);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return keyPointerReader.read(inputStream);
                })
                .collect(Collectors.toList());


        Iterator<KeyPointer> merged = Iterators.mergeSorted(parts.stream()
                .map(BaseStream::iterator)
                .collect(Collectors.toList()), KeyPointer::compareTo);

        Path indexLevel0 = indexDir.resolve(IndexVersion.LATEST.fileName("record"));
        try (FileChannel fileChannel0 = FileChannel.open(indexLevel0, CREATE, WRITE, TRUNCATE_EXISTING)) {
            List<KeyPointer> metadata = keyPointerWriter.write(Channels.newOutputStream(fileChannel0),
                    Streams.stream(merged), (int) Math.sqrt(recordCount));

            Path indexLevel1 = indexDir.resolve(IndexVersion.LATEST.fileName("index"));
            try (FileChannel fileChannel1 = FileChannel.open(indexLevel1, CREATE, WRITE, TRUNCATE_EXISTING)) {
                keyPointerWriter.write(Channels.newOutputStream(fileChannel1), metadata.stream(), metadata.size());
            }

            log.info("First 5 index blocks");
            metadata.stream()
                    .limit(5)
                    .forEach(x -> log.info("{}", x));
        }

        parts.forEach(BaseStream::close);

        qnameStore.deleteAll();

        return recordCount;
    }

    public void shutDown() {
        executorService.shutdown();
    }
}
