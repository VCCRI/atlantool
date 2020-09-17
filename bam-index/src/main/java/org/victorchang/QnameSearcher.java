package org.victorchang;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.apache.lucene.util.fst.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class QnameSearcher implements BamRecordHandler {
    private static final Logger log = LoggerFactory.getLogger(QnameIndexer.class);

    private final Path bamFile;
    private final Path indexFolder;
    private final BamRecordReader recordReader;

    public QnameSearcher(BamRecordReader recordReader, Path indexFolder, Path bamFile) {
        this.bamFile = bamFile;
        this.indexFolder = indexFolder;
        this.recordReader = recordReader;
    }

    public void search(String qname) throws IOException {
        Files.list(indexFolder)
                .filter(x -> x.getFileName().toString().startsWith("qname") && x.getFileName().toString().endsWith(".fst"))
                .filter(Files::isRegularFile)
                .forEach(x -> search(qname, x));
    }

    public void search(String qname, Path indexFile) {
        PositiveIntOutputs outputs = PositiveIntOutputs.getSingleton();
        try {
            FST<Long> fst;
            log.info("Loading fst from {}", indexFile);
            fst = FST.read(indexFile, outputs);

            byte[] input = new byte[qname.length() + 1]; // add 1 byte for key instance
            byte[] bytes = qname.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(bytes, 0, input, 0, bytes.length);

            log.info("Searching in {}", indexFile);
            for (int k = 0; k < 256; k++) {
                input[input.length - 1] = (byte) k;

                Long value = Util.get(fst, new BytesRef(input));

                if (value != null) {
                    long pos = PositionPacker.INSTANCE.unpackBlockPos(value);
                    int offset = PositionPacker.INSTANCE.unpackOffset(value);

                    log.info("Found record at pos {}, offset {}", pos, offset);

                    recordReader.read(bamFile, pos, offset, this);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onRecord(long blockPos, int offset) {
    }

    @Override
    public void onQname(byte[] bytes) {
        String decoded = Ascii7Decoder.INSTANCE.decode(bytes, 0, bytes.length);
        log.info("qname " + decoded);
    }

    @Override
    public void onSequence(byte[] bytes, int fieldLen) {
        String decoded = SeqDecoder.INSTANCE.decode(bytes, 0, bytes.length);
        log.info("sequence " + decoded);
    }
}
