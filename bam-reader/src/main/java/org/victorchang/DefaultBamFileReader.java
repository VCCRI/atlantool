package org.victorchang;

import com.google.common.io.LittleEndianDataInputStream;
import htsjdk.samtools.BAMFileReader;
import htsjdk.samtools.DefaultSAMRecordFactory;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.zip.InflaterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.EOFException;
import java.io.FileInputStream;
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

    @Override
    public long read(Path bamFile, BamRecordHandler handler) throws IOException {
        return read(bamFile, handler, Long.MAX_VALUE);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public long read(Path bamFile, BamRecordHandler handler, long bytesLimit) throws IOException {
        final SAMFileHeader header = readSamHeader(bamFile);
        long recordCount = 0;
        try (FileChannel fileChannel = FileChannel.open(bamFile, READ)) {
            InputStream compressedStream = new BufferedInputStream(Channels.newInputStream(fileChannel), FILE_BUFF_SIZE);

            GzipEntryPositionFinder positionFinder = new GzipEntryPositionFinder();
            CountingInputStream uncompressedStream = new CountingInputStream(
                    new BufferedInputStream(
                            new GzipConcatenatedInputStream(compressedStream, positionFinder), FILE_BUFF_SIZE));

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

                if (position.getCompressed() >= bytesLimit) {
                    log.info("Reach {} bytes limit, skip the rest the file", bytesLimit);
                    break;
                }

                long uoffset = uncompressedStream.getBytesRead() - position.getUncompressed() - 4;

                if (uoffset < 0 || uoffset >= (1 << 16)) {
                    throw new IllegalStateException("Offset must be in the range of [0,2^16)");
                }
                handler.onAlignmentPosition(position.getCompressed(), (int) uoffset);

                dataInput.mark(recordLength);

                recordParser.parse(header, dataInput, recordLength, handler);

                dataInput.reset();
                skipBytesFully(dataInput, recordLength);
                recordCount++;
            }
        }
        log.info("Read {} records", recordCount);
        return recordCount;
    }

    private SAMFileHeader readSamHeader(Path bamFile) throws IOException {
        return SamReaderFactory.make().getFileHeader(bamFile);
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
        skipBytesFully(dataInput, len);
    }

    private void skipReferences(DataInput dataInput) throws IOException {
        int refCount = dataInput.readInt();
        for (int i = 0; i < refCount; i++) {
            int len = dataInput.readInt() + 4;
            skipBytesFully(dataInput, len);
        }
    }

    private void skipBytesFully(DataInput dataInput, int len) throws IOException {
        while (len > 0) {
            len -= dataInput.skipBytes(len);
        }
    }
}