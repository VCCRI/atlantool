package org.victorchang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class SortedQnameFileFactory {
    private static final Logger log = LoggerFactory.getLogger(SortedQnameFileFactory.class);

    private final FileStore pathGenerator;

    private final QnamePosWriter writer;

    public SortedQnameFileFactory(FileStore pathGenerator, QnamePosWriter writer) {
        this.pathGenerator = pathGenerator;
        this.writer = writer;
    }

    public Path create(QnamePosBuffer buffer) {
        Path path = pathGenerator.generate();
        log.info("Creating sorted qname");
        writer.create(path, buffer);
        log.info(path.toString() + " is created");
        return path;
    }
}
