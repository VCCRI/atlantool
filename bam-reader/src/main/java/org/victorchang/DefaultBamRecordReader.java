package org.victorchang;


import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.READ;

public class DefaultBamRecordReader implements BamRecordReader {
    private static final int BUFFER_SIZE = 8192;

    private final GzipBlockAssembler assembler;
    private final BamBlockReader blockReader;

    public DefaultBamRecordReader(GzipBlockAssembler assembler, BamBlockReader blockReader) {
        this.assembler = assembler;
        this.blockReader = blockReader;
    }

    @Override
    public void read(Path path, long blockPos, int recordNum, BamRecordHandler recordHandler) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        try (FileChannel fileChannel = FileChannel.open(path, READ)) {
            fileChannel.position(blockPos);
            InputStream inputStream = Channels.newInputStream(fileChannel);

            GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(inputStream, false);
            assembler.reset();
            int len;
            while ((len = gzipInputStream.read(buffer)) > 0) {
                assembler.append(buffer, len);
            }
            if (assembler.length() > 0) {
                blockReader.read(assembler.buffer(), new TargetRecordHandler(recordHandler, recordNum), blockPos == 0);
            }
        }
    }

    private static class TargetRecordHandler implements BamRecordHandler {
        private final BamRecordHandler delegate;
        private final int targetRecord;

        private int currentRecord;

        private TargetRecordHandler(BamRecordHandler delegate, int targetRecord) {
            this.delegate = delegate;
            this.targetRecord = targetRecord;
        }

        @Override
        public void onRecord(ByteBuffer byteBuffer, int byteLen, int recordNum) {
            this.currentRecord = recordNum;
            if (currentRecord == targetRecord) {
                delegate.onRecord(byteBuffer, byteLen, recordNum);
            }
        }

        @Override
        public void onQname(ByteBuffer byteBuffer, int byteLen) {
            if (currentRecord == targetRecord) {
                delegate.onQname(byteBuffer, byteLen);
            }
        }

        @Override
        public void onSequence(ByteBuffer byteBuffer, int fieldLen) {
            if (currentRecord == targetRecord) {
                delegate.onSequence(byteBuffer, fieldLen);
            }
        }
    }
}