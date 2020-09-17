package org.victorchang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Arrays;

public class SortedQnameFileFactory {
    private static final Logger log = LoggerFactory.getLogger(SortedQnameFileFactory.class);

    private final Path folder;
    private int fileCount;

    private final QnamePosWriter writer;

    public SortedQnameFileFactory(Path folder, QnamePosWriter writer) {
        this.folder = folder;
        this.writer = writer;
        fileCount = 0;
    }

    public Path create(QnamePos[] buffer, int recordCount) {
        Arrays.sort(buffer, 0, recordCount);
        Path path = nextFile();
        writer.create(path, buffer, recordCount);
        log.info(path.toString() + " is created");
        return path;
    }

    private Path nextFile() {
        Path path = folder.resolve("qname" + fileCount + ".sorted");
        fileCount++;
        return path;
    }
}
