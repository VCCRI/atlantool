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
import java.util.Arrays;

public class FstFactory {
    private static final Logger log = LoggerFactory.getLogger(QnameIndexer.class);

    private final Path folder;
    private int fileCount;

    public FstFactory(Path folder) {
        this.folder = folder;
        this.fileCount = 0;
    }

    public void create(QnamePos[] buffer, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("number of record count must be greater than 0");
        }
        Arrays.sort(buffer, 0, count);

        PositiveIntOutputs outputs = PositiveIntOutputs.getSingleton();
        Builder<Long> fstBuilder = new Builder<>(FST.INPUT_TYPE.BYTE1, outputs);
        IntsRefBuilder intsRefBuilder = new IntsRefBuilder();

        Path path = nextFile();
        try {
            int k = 0;
            QnamePos previous;
            QnamePos current = buffer[0];
            fstBuilder.add(createIntsRef(current.getQname(), intsRefBuilder), current.getPosition());
            for (int i = 1; i < count; i++) {
                previous = current;
                current = buffer[i];
                if (current.compareTo(previous) == 0) {
                    k++; // duplicated key
                } else {
                    k = 0;
                }
                byte[] qname = current.getQname();
                if (k > 255) {
                    throw new IllegalStateException("There is more than 256 records with the same qname");
                }
                qname[qname.length - 1] = (byte)k; // up to 2^8 duplicated keys are supported
                fstBuilder.add(createIntsRef(current.getQname(), intsRefBuilder), current.getPosition());
            }
            FST<Long> fst = fstBuilder.finish();
            fst.save(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info(path.toString() + " is created");
    }

    private Path nextFile() {
        Path path = folder.resolve("qname" + fileCount + ".fst");
        fileCount++;
        return path;
    }

    private IntsRef createIntsRef(byte[] bytes, IntsRefBuilder intsRefBuilder) {
        intsRefBuilder.clear();
        for (byte b : bytes) {
            intsRefBuilder.append(b & 0xff);
        }
        return intsRefBuilder.get();
    }
}
