package org.victorchang;

import com.google.common.io.LittleEndianDataInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardOpenOption.READ;

public class DefaultBamFileReader implements BamFileReader {
    private static final Logger log = LoggerFactory.getLogger(DefaultBamFileReader.class);

    private static final int BUFF_SIZE = 8192;
    private static final byte[] MAGIC = {'B', 'A', 'M', 1};

    private final BamRecordParser recordParser;

    public DefaultBamFileReader(BamRecordParser recordParser) {
        this.recordParser = recordParser;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void read(Path bamFile, BamRecordHandler handler) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(bamFile, READ)) {
            InputStream compressedStream = new BufferedInputStream(Channels.newInputStream(fileChannel), BUFF_SIZE);

            PositionFinder positionFinder = new PositionFinder();
            CountingInputStream uncompressedStream = new CountingInputStream(
                    new BufferedInputStream(
                            new GzipCompressorInputStream(compressedStream, positionFinder), BUFF_SIZE));

            LittleEndianDataInputStream dataInput = new LittleEndianDataInputStream(uncompressedStream);
            assertMagic(dataInput);
            skipHeaderText(dataInput);
            skipReferences(dataInput);

            while (true) {
                int recordLength;
                try {
                    recordLength = dataInput.readInt();
                } catch (EOFException ignored) {
                    break;
                }

                GzipEntryPosition position = positionFinder.getPosition(uncompressedStream.getBytesRead() - 4);

                if (position == null) {
                    throw new IllegalStateException("can't find start of Gzip entry");
                }

                int offset = (int) (uncompressedStream.getBytesRead() - position.getUncompressed() - 4);

                handler.onRecord(position.getCompressed(), offset);

                dataInput.mark(recordLength);

                recordParser.parse(dataInput, handler);

                dataInput.reset();
                while (recordLength > 0) {
                    recordLength -= dataInput.skipBytes(recordLength);
                }
            }
        }
    }

    private static class GzipEntryPosition {
        private final long compressed;
        private final long uncompressed;

        public GzipEntryPosition(long compressed, long uncompressed) {
            this.compressed = compressed;
            this.uncompressed = uncompressed;
        }

        public long getCompressed() {
            return compressed;
        }

        public long getUncompressed() {
            return uncompressed;
        }
    }

    private static class PositionFinder implements GzipEntryEventHandler {
        private final List<GzipEntryPosition> positions;

        public PositionFinder() {
            this.positions = new ArrayList<>();
        }

        public GzipEntryPosition getPosition(long uncompressed) {
            for (int i = positions.size() - 1; i >= 0; i--) {
                GzipEntryPosition current = positions.get(i);
                if (uncompressed >= current.getUncompressed()) {
                    return current;
                }
            }

            return null;
        }

        @Override
        public void onStart(long compressedCount, long uncompressedCount) {
            positions.add(new GzipEntryPosition(compressedCount, uncompressedCount));
        }
    }

    private void assertMagic(DataInput dataInput) throws IOException {
        byte[] buffer = new byte[4];
        dataInput.readFully(buffer);

        if (buffer[0] != MAGIC[0] || buffer[1] != MAGIC[1] || buffer[2] != MAGIC[2] || buffer[3] != MAGIC[3]) {
            throw new IllegalStateException("Invalid BAM magic");
        }
    }

    private void skipHeaderText(DataInput dataInput) throws IOException {
        int len = dataInput.readInt();
        dataInput.skipBytes(len);
    }

    private void skipReferences(DataInput dataInput) throws IOException {
        int refCount = dataInput.readInt();
        for (int i = 0; i < refCount; i++) {
            int len = dataInput.readInt();
            dataInput.skipBytes(len + 4);
        }
    }
}