package org.victorchang;

import java.nio.file.Path;
import java.util.List;

public interface FileStore {
    Path generate();
    List<Path> list();
    void deleteAll();
}
