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
import java.util.ArrayList;
import java.util.List;

public class QnameFstBuilder {
    private static final Logger log = LoggerFactory.getLogger(QnameFstBuilder.class);

    private final Path folder;
    private int fileCount;

    private final int maxRecordCount;
    private int recordCount;

    private final IntsRefBuilder intsRefBuilder;
    private Builder<Long> fstBuilder;

    private QnamePos startRecord;

    private QnamePos previousRecord;
    private int k;

    private final List<QnamePos> qnameRanges;

    public QnameFstBuilder(Path folder, int maxRecordCount) {
        this.folder = folder;
        this.maxRecordCount = maxRecordCount;

        fileCount = 0;
        recordCount = 0;

        fstBuilder = new Builder<>(FST.INPUT_TYPE.BYTE1, PositiveIntOutputs.getSingleton());
        intsRefBuilder = new IntsRefBuilder();

        qnameRanges = new ArrayList<>();
    }

    public void add(QnamePos currentRecord) {
        if (startRecord == null) {
            startRecord = currentRecord;
        }
        try {
            if (previousRecord != null && currentRecord.compareTo(previousRecord) == 0) {
                k++; // duplicated key
            } else {
                k = 0;
            }
            if (k > 255) {
                throw new IllegalStateException("There is more than 256 records with the same qname");
            }
            byte[] qname = currentRecord.getQname();
            long position = currentRecord.getPosition();
            qname[qname.length - 1] = (byte) k;
            fstBuilder.add(createIntsRef(qname, intsRefBuilder), position);
            previousRecord = currentRecord;
            recordCount++;
            if (recordCount >= maxRecordCount) {
                flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void flush() throws IOException {
        if (recordCount > 0) {
            Path path = nextFile();
            FST<Long> fst = fstBuilder.finish();
            fst.save(path);

            qnameRanges.add(startRecord);

            log.info("{} is created starting at {}", path.toString(),
                    Ascii7Coder.INSTANCE.decode(startRecord.getQname(), 0, startRecord.getQname().length));

            recordCount = 0;
            startRecord = null;
            fstBuilder = new Builder<>(FST.INPUT_TYPE.BYTE1, PositiveIntOutputs.getSingleton());
        }
    }

    public List<QnamePos> getQnameRanges() {
        return qnameRanges;
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
