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

public class QnameIndexer implements BamRecordHandler {
    private static final Logger log = LoggerFactory.getLogger(QnameIndexer.class);

    private static final int BUFFER_SIZE = 1000_000;

    private final BamFileReader fileReader;
    private long blockPos;
    private int offset;

    private QnamePos[] data;
    private int count;
    private int indexFileNo;
    private Path indexFolder;

    public QnameIndexer(BamFileReader fileReader) {
        this.fileReader = fileReader;
    }

    public void createIndex(Path bamFile, Path indexFolder) throws IOException {
        this.indexFileNo = 0;
        this.indexFolder = indexFolder;
        this.data = new QnamePos[BUFFER_SIZE];
        this.count = 0;

        fileReader.read(bamFile, this);
        if (count > 0) {
            createFst(nextIndexFile());
        }
    }

    private Path nextIndexFile() {
        Path path = indexFolder.resolve("qname" + indexFileNo);
        indexFileNo++;
        return path;
    }

    private void createFst(Path path) throws IOException {
        Arrays.sort(data, 0, count);

        PositiveIntOutputs outputs = PositiveIntOutputs.getSingleton();
        Builder<Long> fstBuilder = new Builder<>(FST.INPUT_TYPE.BYTE1, outputs);
        IntsRefBuilder intsRefBuilder = new IntsRefBuilder();
        for (int i = 0; i < count; i++) {
            fstBuilder.add(toIntsRef(data[i].getQname(), intsRefBuilder), data[i].getPosition());
        }

        FST<Long> fst = fstBuilder.finish();
        fst.save(path);

        log.info(path.toString() + " is created");

        Arrays.fill(data, null);
        count = 0;
    }

    private IntsRef toIntsRef(byte[] bytes, IntsRefBuilder intsRefBuilder) {
        intsRefBuilder.clear();
        for (byte b : bytes) {
            intsRefBuilder.append(b & 0xff);
        }
        return intsRefBuilder.get();
    }

    @Override
    public void onRecord(long blockPos, int offset) {
        this.blockPos = blockPos;
        this.offset = offset;
    }

    @Override
    public void onQname(byte[] bytes) {
        data[count++] = new QnamePos(blockPos, offset, bytes);

        if (count >= BUFFER_SIZE) {
            try {
                createFst(nextIndexFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onSequence(byte[] bytes, int fieldLen) {
    }
}
