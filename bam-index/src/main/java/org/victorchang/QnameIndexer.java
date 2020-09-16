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
import java.nio.file.Paths;
import java.util.Arrays;

public class QnameIndexer implements BamRecordHandler {
    private static final Logger log = LoggerFactory.getLogger(QnameIndexer.class);

    private static final int BUFFER_SIZE = 1000_000;

    private final BamFileReader fileReader;
    private long blockPos;
    private int offset;

    private int fileNo;
    private final QnamePos[] positions;
    private int pos;

    public QnameIndexer(BamFileReader fileReader) {
        this.fileReader = fileReader;

        fileNo = 0;
        positions = new QnamePos[BUFFER_SIZE];
        pos = 0;
    }

    public void createIndex(Path bamFile) throws IOException {
        fileReader.read(bamFile, this);
        if (pos > 0) {
            createFst(nextPath());
        }
    }

    private Path nextPath() {
        Path path = Paths.get(".", "qname" + fileNo);
        fileNo++;
        return path;
    }

    private void createFst(Path path) throws IOException {
        Arrays.sort(positions, 0, pos);

        PositiveIntOutputs outputs = PositiveIntOutputs.getSingleton();
        Builder<Long> fstBuilder = new Builder<>(FST.INPUT_TYPE.BYTE1, outputs);
        IntsRefBuilder intsRefBuilder = new IntsRefBuilder();
        for (int i = 0; i < pos; i++) {
            fstBuilder.add(toIntsRef(positions[i].getQname(), intsRefBuilder), positions[i].getPosition());
        }

        FST<Long> fst = fstBuilder.finish();
        fst.save(path);

        Arrays.fill(positions, null);
        pos = 0;
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
        if (blockPos < 0 || offset < 0) {
            log.warn(String.format("negative block pos %d or offset %d", blockPos, offset));
        }
        this.blockPos = blockPos;
        this.offset = offset;
    }

    @Override
    public void onQname(byte[] bytes) {
        positions[pos++] = new QnamePos(blockPos, offset, bytes);

        if (pos >= BUFFER_SIZE) {
            try {
                createFst(nextPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onSequence(byte[] bytes, int fieldLen) {
    }
}
