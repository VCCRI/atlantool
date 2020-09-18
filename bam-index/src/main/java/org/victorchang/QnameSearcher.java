package org.victorchang;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.apache.lucene.util.fst.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class QnameSearcher implements BamRecordHandler {
    private static final Logger log = LoggerFactory.getLogger(QnameIndexer.class);

    private final QnamePosReader qnamePosReader;
    private final BamRecordReader recordReader;

    private final Map<Path, FST<Long>> fstCache;

    public QnameSearcher(QnamePosReader qnamePosReader, BamRecordReader recordReader) {
        this.qnamePosReader = qnamePosReader;
        this.recordReader = recordReader;
        fstCache = new HashMap<>();
    }

    public int search(Path bamFile, Path indexFolder, String qname) {
        Path rangesPath = indexFolder.resolve("ranges.sorted");

        TreeMap<QnamePos, Path> indexFiles = new TreeMap<>();

        int fileCount = 0;
        Iterable<QnamePos> ranges = () -> qnamePosReader.read(rangesPath).iterator();
        for (QnamePos start : ranges) {
            indexFiles.put(start, indexFolder.resolve("qname" + fileCount + ".fst"));
            fileCount++;
        }

        byte[] input = Ascii7Coder.INSTANCE.encode(qname);
        int found = 0;
        for (int k = 0; k < 256; k++) {
            input[input.length - 1] = (byte) k;
            Map.Entry<QnamePos, Path> from = indexFiles.floorEntry(new QnamePos(0, input, input.length));
            if (from == null) {
                break;
            }
            if (search(input, bamFile, from.getValue())) {
                found++;
            } else {
                break;
            }
        }
        return found;
    }

    private boolean search(byte[] input, Path bamFile, Path indexFile) {
        FST<Long> fst = fstCache.computeIfAbsent(indexFile, file -> {
            log.info("Loading fst from {}", file);
            try {
                return FST.read(indexFile, PositiveIntOutputs.getSingleton());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            log.info("Searching in {}", indexFile);

            Long value = Util.get(fst, new BytesRef(input));

            if (value != null) {
                long pos = PositionPacker.INSTANCE.unpackBlockPos(value);
                int offset = PositionPacker.INSTANCE.unpackOffset(value);

                log.info("Found record at pos {}, offset {}", pos, offset);

                recordReader.read(bamFile, pos, offset, this);
                return true;
            }

            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onRecord(long blockPos, int offset) {
    }

    @Override
    public void onQname(byte[] qnameBuffer, int qnameLen) {
        String decoded = Ascii7Coder.INSTANCE.decode(qnameBuffer, 0, qnameLen);
        log.info("qname " + decoded);
    }

    @Override
    public void onSequence(byte[] seqBuffer, int seqLen) {
        String decoded = SeqDecoder.INSTANCE.decode(seqBuffer, 0, seqLen);
        log.info("sequence " + decoded);
    }
}
