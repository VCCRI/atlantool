package org.victorchang;

import com.google.common.collect.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class SortedQnameFileFactory {
    private static final Logger log = LoggerFactory.getLogger(SortedQnameFileFactory.class);

    private final FileStore pathGenerator;

    private final KeyPointerWriter writer;

    public SortedQnameFileFactory(FileStore pathGenerator, KeyPointerWriter writer) {
        this.pathGenerator = pathGenerator;
        this.writer = writer;
    }

    public Path create(KeyPointerBuffer buffer) throws IOException {
        Path path = pathGenerator.generate();
        log.info("Creating sorted {}", path);
        buffer.sort();
        try (FileChannel fileChannel = FileChannel.open(path, CREATE, WRITE, TRUNCATE_EXISTING)) {
            writer.write(Channels.newOutputStream(fileChannel), Streams.stream(buffer));
        }
        log.info(path.toString() + " is created");
        return path;
    }
}
