package org.victorchang;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.apache.lucene.util.fst.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class QnameSearcher implements BamRecordHandler {
    private static final Logger log = LoggerFactory.getLogger(QnameIndexer.class);

    private final Path indexFile;
    private final Path bamFile;
    private final BamRecordReader recordReader;

    public QnameSearcher(Path bamFile, Path indexFile, BamRecordReader recordReader) {
        this.indexFile = indexFile;
        this.bamFile = bamFile;
        this.recordReader = recordReader;
    }

    public void search(String qname) throws IOException {
        PositiveIntOutputs outputs = PositiveIntOutputs.getSingleton();
        FST<Long> fst = FST.read(indexFile, outputs);

        byte[] input = new byte[qname.length() + 1]; // add null terminated
        byte[] bytes = qname.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, input, 0, bytes.length);

        Long value = Util.get(fst, new BytesRef(input));

        if (value != null) {
            long pos = PositionPacker.INSTANCE.unpackBlockPos(value);
            int offset = PositionPacker.INSTANCE.unpackOffset(value);

            log.info(String.format("pos %d, offset %d", pos, offset));

            recordReader.read(bamFile, pos, offset, this);
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
