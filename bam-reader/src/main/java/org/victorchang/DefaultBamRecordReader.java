package org.victorchang;

import com.google.common.io.LittleEndianDataInputStream;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.BlockCompressedFilePointerUtil;
import htsjdk.samtools.util.BlockCompressedInputStream;

import java.io.IOException;
import java.nio.file.Path;

public class DefaultBamRecordReader implements BamRecordReader {

    private final BamRecordParser recordParser;

    public DefaultBamRecordReader(BamRecordParser recordParser) {
        this.recordParser = recordParser;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void read(Path bamFile, long coffset, int uoffset, BamRecordHandler recordHandler) throws IOException {
        final SAMFileHeader header = SamReaderFactory.make().getFileHeader(bamFile);
        recordHandler.onHeader(header);

        try (BlockCompressedInputStream blockCompressedInputStream = new BlockCompressedInputStream(bamFile.toFile())) {
            blockCompressedInputStream.seek(BlockCompressedFilePointerUtil.makeFilePointer(coffset, uoffset));

            LittleEndianDataInputStream dataInput = new LittleEndianDataInputStream(blockCompressedInputStream);
            final int recordLength = dataInput.readInt();// record length
            recordParser.parse(header, dataInput, recordLength, recordHandler);
        }
    }

    @Override
    public void read(Path bamFile, long pointer, BamRecordHandler recordHandler) throws IOException {
        long coffset = BlockCompressedFilePointerUtil.getBlockAddress(pointer);
        int uoffset = BlockCompressedFilePointerUtil.getBlockOffset(pointer);
        read(bamFile, coffset, uoffset, recordHandler);
    }
}