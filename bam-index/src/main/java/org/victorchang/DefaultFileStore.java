package org.victorchang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class DefaultFileStore implements FileStore {
    private final Path folder;
    private final String prefix;
    private final String ext;

    private final AtomicLong counter;

    public DefaultFileStore(Path folder, String prefix, String ext) {
        this.folder = folder;
        this.prefix = prefix;
        this.ext = ext;
        counter = new AtomicLong(0);
    }

    @Override
    public Path generate() {
        return folder.resolve(prefix + counter.getAndIncrement() + '.' + ext);
    }

    @Override
    public List<Path> list() {
        try {
            return Files.list(folder)
                    .filter(this::matched)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    @Override
    public void deleteAll() {
        list().forEach(x -> {
            try {
                Files.delete(x);
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        });
    }

    private boolean matched(Path path) {
        return path.getFileName().toString().startsWith(prefix) &&
                path.getFileName().toString().endsWith("." + ext);

    }
}
