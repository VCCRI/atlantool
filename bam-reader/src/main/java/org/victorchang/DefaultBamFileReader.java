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

import static java.nio.file.StandardOpenOption.READ;

public class DefaultBamFileReader implements BamFileReader {
    private static final Logger log = LoggerFactory.getLogger(DefaultBamFileReader.class);

    private static final int FILE_BUFF_SIZE = 8192;
    private static final byte[] MAGIC = {'B', 'A', 'M', 1};

    private final BamRecordParser recordParser;

    public DefaultBamFileReader(BamRecordParser recordParser) {
        this.recordParser = recordParser;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void read(Path bamFile, BamRecordHandler handler) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(bamFile, READ)) {
            InputStream compressedStream = new BufferedInputStream(Channels.newInputStream(fileChannel), FILE_BUFF_SIZE);

            GzipEntryPositionFinder positionFinder = new GzipEntryPositionFinder();
            CountingInputStream uncompressedStream = new CountingInputStream(
                    new BufferedInputStream(
                            new GzipCompressorInputStream(compressedStream, positionFinder), FILE_BUFF_SIZE));

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

                GzipEntryPosition position = positionFinder.find(uncompressedStream.getBytesRead() - 4);

                if (position == null) {
                    throw new IllegalStateException("Can't find start of a gzip entry");
                }

                long offset = uncompressedStream.getBytesRead() - position.getUncompressed() - 4;

                if (offset < 0 || offset >= (1 << 16)) {
                    throw new IllegalStateException("offset must be in the range of [0,2^16)");
                }
                handler.onRecord(position.getCompressed(), (int)offset);

                dataInput.mark(recordLength);

                recordParser.parse(dataInput, handler);

                dataInput.reset();
                while (recordLength > 0) {
                    recordLength -= dataInput.skipBytes(recordLength);
                }
            }
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