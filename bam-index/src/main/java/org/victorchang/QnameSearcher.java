package org.victorchang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Arrays;

import static java.nio.file.StandardOpenOption.READ;

public class QnameSearcher implements BamRecordHandler {
    private static final Logger log = LoggerFactory.getLogger(QnameIndexer.class);

    private final KeyPointerReader keyPointerReader;
    private final BamRecordReader recordReader;

    public QnameSearcher(KeyPointerReader keyPointerReader, BamRecordReader recordReader) {
        this.keyPointerReader = keyPointerReader;
        this.recordReader = recordReader;
    }

    public int search(Path bamFile, Path indexFolder, String qname) throws IOException {
        Path pathLevel1 = indexFolder.resolve("qname.1");
        FileChannel channelLevel1 = FileChannel.open(pathLevel1, READ);
        InputStream inputStreamLevel1 = Channels.newInputStream(channelLevel1);

        byte[] input = Ascii7Coder.INSTANCE.encode(qname);
        Iterable<KeyPointer> indexLevel1 = () -> keyPointerReader.read(inputStreamLevel1).iterator();

        KeyPointer key = new KeyPointer(0, input, input.length);
        KeyPointer start = key;
        for (KeyPointer x : indexLevel1) {
            if (x.compareTo(key) > 0) {
                break;
            }
            start = x;
        }
        inputStreamLevel1.close();

        Path pathLevel0 = indexFolder.resolve("qname.0");
        FileChannel channelLevel0 = FileChannel.open(pathLevel0, READ);
        long coffset = PointerPacker.INSTANCE.unpackCompressedOffset(start.getPointer());
        int uoffset = PointerPacker.INSTANCE.unpackUnCompressedOffset(start.getPointer());

        if (coffset >= channelLevel0.size()) {
            return 0;
        }
        channelLevel0.position(coffset);
        InputStream inputStreamLevel0 = Channels.newInputStream(channelLevel0);

        int found = 0;
        Iterable<KeyPointer> indexLevel0 =  () -> keyPointerReader.read(inputStreamLevel0, uoffset).iterator();
        for (KeyPointer x : indexLevel0) {
            if (Arrays.equals(x.getKey(), input)) {
                log.info("Found record at {}", x);
                recordReader.read(bamFile, x.getPointer(), this);
                found++;
            }
            if (Arrays.compareUnsigned(x.getKey(), input) > 0) {
                break;
            }
        }

        inputStreamLevel0.close();

        return found;
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
