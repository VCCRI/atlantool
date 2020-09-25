package org.victorchang;

import com.google.common.io.LittleEndianDataInputStream;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.cram.io.CountingInputStream;
import htsjdk.samtools.util.BlockCompressedFilePointerUtil;
import htsjdk.samtools.util.BlockCompressedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;

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
        handler.onHeader(header);
        long recordCount = 0;
        try (BlockCompressedInputStream blockCompressedInputStream = new BlockCompressedInputStream(bamFile.toFile())) {

            CountingInputStream countingInputStream = new CountingInputStream(blockCompressedInputStream);
            LittleEndianDataInputStream dataInput = new LittleEndianDataInputStream(countingInputStream);
            assertMagic(dataInput);
            skipHeaderText(dataInput);
            skipReferences(dataInput);

            while (true) {
                long filePointer = blockCompressedInputStream.getFilePointer();
                long coffset = BlockCompressedFilePointerUtil.getBlockAddress(filePointer);
                int uoffset = BlockCompressedFilePointerUtil.getBlockOffset(filePointer);

                int recordLength;
                try {
                    recordLength = dataInput.readInt();
                } catch (EOFException ignored) {
                    break;
                }

                if (coffset >= bytesLimit) {
                    log.info("Reach {} bytes limit, skip the rest the file", bytesLimit);
                    break;
                }

                handler.onAlignmentPosition(coffset, uoffset);

                long start = countingInputStream.getCount();
                recordParser.parse(header, dataInput, recordLength, handler);
                long end = countingInputStream.getCount();

                int remaining = recordLength - ((int) (end - start));
                skipBytesFully(dataInput, remaining);

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