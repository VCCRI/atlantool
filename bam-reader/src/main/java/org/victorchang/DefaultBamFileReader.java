package org.victorchang;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.READ;

public class DefaultBamFileReader implements BamFileReader {
    private static final int BUFFER_SIZE = 8192;

    private final GzipBlockAssembler blockAssembler;
    private final BamBlockReader blockReader;

    public DefaultBamFileReader(GzipBlockAssembler blockAssembler, BamBlockReader blockReader) {
        this.blockAssembler = blockAssembler;
        this.blockReader = blockReader;
    }

    @Override
    public void read(Path path, BamFileHandler handler) throws IOException {
        long blockPos = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        try (FileChannel fileChannel = FileChannel.open(path, READ)) {
            InputStream inputStream = Channels.newInputStream(fileChannel);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            do {
                GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(bufferedInputStream, false);
                blockAssembler.reset();
                int len;
                while ((len = gzipInputStream.read(buffer)) > 0) {
                    blockAssembler.append(buffer, len);
                }
                if (blockAssembler.length() > 0) {
                    handler.onBlock(blockAssembler.buffer(), blockPos);
                    blockReader.read(blockAssembler.buffer(), handler, blockPos == 0);
                }
                blockPos += gzipInputStream.getCompressedCount();
            } while (blockAssembler.length() > 0);
        }
    }
}