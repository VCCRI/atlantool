package org.victorchang;


import com.google.common.io.LittleEndianDataInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.READ;

public class DefaultBamRecordReader implements BamRecordReader {
    private static final int BUFF_SIZE = 8192;

    private final BamRecordParser recordParser;

    public DefaultBamRecordReader(BamRecordParser recordParser) {
        this.recordParser = recordParser;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void read(Path bamFile, long blockPos, int offset, BamRecordHandler recordHandler) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(bamFile, READ).position(blockPos)) {
            InputStream compressedStream = new BufferedInputStream(Channels.newInputStream(fileChannel), BUFF_SIZE);

            CountingInputStream uncompressedStream = new CountingInputStream(
                    new BufferedInputStream(
                            new GzipCompressorInputStream(compressedStream, (compressed, uncompressed) -> {}), BUFF_SIZE));

            LittleEndianDataInputStream dataInput = new LittleEndianDataInputStream(uncompressedStream);
            while (offset > 0) {
                offset -= dataInput.skipBytes(offset);
            }
            dataInput.readInt(); // record length
            recordParser.parse(dataInput, recordHandler);
        }
    }
}