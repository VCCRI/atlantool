package org.victorchang;

import org.apache.commons.compress.utils.CountingOutputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class GzipConcatenatedOutputStream extends OutputStream {
    private static final int FILE_BUFF_SIZE = 8192;

    private final OutputStream outputStream;
    private final int uncompressedSize;

    private CountingOutputStream uncompressedStream;
    private CountingOutputStream compressedStream;
    private long compressedCount;

    public GzipConcatenatedOutputStream(OutputStream outputStream, int uncompressedSize) throws IOException {
        this.outputStream = outputStream;
        this.uncompressedSize = uncompressedSize;
        compressedCount = 0;
        nextEntry();
    }

    @Override
    public void write(int b) throws IOException {
        if (uncompressedStream.getBytesWritten() + 1 > uncompressedSize) {
            nextEntry();
        }
        uncompressedStream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (uncompressedStream.getBytesWritten() + b.length > uncompressedSize) {
            nextEntry();
        }
        uncompressedStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (uncompressedStream.getBytesWritten() + len > uncompressedSize) {
            nextEntry();
        }
        uncompressedStream.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        uncompressedStream.close();
        compressedCount += compressedStream.getBytesWritten();
        outputStream.close();
    }

    public long getCompressedCount() {
        return compressedCount;
    }

    public long getUncompressedCount() {
        return uncompressedStream.getBytesWritten();
    }

    private void nextEntry() throws IOException {
        if (uncompressedStream != null) {
            uncompressedStream.close();
            compressedCount += compressedStream.getBytesWritten();
        }

        compressedStream = new CountingOutputStream(outputStream) {
            @Override
            public void close() throws IOException {
                // don't close underlying output stream so we can continue writing to it
                flush();
            }
        };
        OutputStream gzipOutputStream = new GZIPOutputStream(compressedStream);
        uncompressedStream = new CountingOutputStream(new BufferedOutputStream(gzipOutputStream, FILE_BUFF_SIZE));
    }
}
