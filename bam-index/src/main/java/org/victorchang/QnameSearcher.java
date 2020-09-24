package org.victorchang;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTextWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.file.StandardOpenOption.READ;
import static java.util.Collections.emptyList;

public class QnameSearcher {
    private static final Logger log = LoggerFactory.getLogger(QnameSearcher.class);

    private final KeyPointerReader keyPointerReader;
    private final BamRecordReader recordReader;
    private final BamRecordHandler handler;

    public QnameSearcher(KeyPointerReader keyPointerReader, BamRecordReader recordReader, BamRecordHandler handler) {
        this.keyPointerReader = keyPointerReader;
        this.recordReader = recordReader;
        this.handler = handler;
    }

    public int search(Path bamFile, Path indexFolder, String qname) throws IOException {
        final List<Long> pointersForQname = getPointersForQname(indexFolder, qname);
        for (Long pointer : pointersForQname) {
            recordReader.read(bamFile, pointer, handler);
        }
        return pointersForQname.size();
    }

    List<Long> getPointersForQname(Path indexFolder, String qname) throws IOException {
        Path pathLevel1 = indexFolder.resolve("qname.1");
        FileChannel channelLevel1 = FileChannel.open(pathLevel1, READ);
        InputStream inputStreamLevel1 = Channels.newInputStream(channelLevel1);

        byte[] input = Ascii7Coder.INSTANCE.encode(qname);
        Iterable<KeyPointer> indexLevel1 = () -> keyPointerReader.read(inputStreamLevel1).iterator();

        KeyPointer key = new KeyPointer(0, input, input.length);
        KeyPointer start = key;
        for (KeyPointer x : indexLevel1) {
            if (x.compareTo(key) >= 0) {
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
            return emptyList();
        }
        channelLevel0.position(coffset);
        InputStream inputStreamLevel0 = Channels.newInputStream(channelLevel0);

        List<Long> pointers = new ArrayList<>();
        Iterable<KeyPointer> indexLevel0 =  () -> keyPointerReader.read(inputStreamLevel0, uoffset).iterator();
        for (KeyPointer x : indexLevel0) {
            if (Arrays.equals(x.getKey(), input)) {
                pointers.add(x.getPointer());
            }
            if (Arrays.compareUnsigned(x.getKey(), input) > 0) {
                break;
            }
        }

        inputStreamLevel0.close();

        return pointers;
    }

    public static class DebuggingHandler implements BamRecordHandler {

        @Override
        public void onHeader(SAMFileHeader header) {
        }

        @Override
        public void onAlignmentPosition(long blockPos, int offset) {
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

        @Override
        public void onAlignmentRecord(SAMRecord record) {
            final ByteArrayOutputStream inMemoryStream = new ByteArrayOutputStream();
            final SAMTextWriter writer = new SAMTextWriter(inMemoryStream);
            writer.writeAlignment(record);
            writer.finish();
            log.debug("SAM alignment record (below)");
            log.debug(new String(inMemoryStream.toByteArray()));
        }
    }
}

