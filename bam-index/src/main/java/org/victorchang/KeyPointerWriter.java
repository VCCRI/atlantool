package org.victorchang;


import com.google.common.io.LittleEndianDataOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.Deflater;

/**
 * Write a stream of {@link KeyPointer} into a concatenated gzip output.
 */
public class KeyPointerWriter {

    private final int compressionLevel;

    public KeyPointerWriter(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    public KeyPointerWriter() {
        this(Deflater.DEFAULT_COMPRESSION);
    }

    public List<KeyPointer> write(OutputStream outputStream, Stream<KeyPointer> stream) throws IOException {
        return write(outputStream, stream, Long.MAX_VALUE);
    }

    @SuppressWarnings("UnstableApiUsage")
    public List<KeyPointer> write(OutputStream outputStream, Stream<KeyPointer> stream, long blockSize) throws IOException {
        GzipConcatenatedOutputStream concatenatedStream =
                new GzipConcatenatedOutputStream(outputStream, 1 << 16, compressionLevel);

        LittleEndianDataOutputStream dataOutput = new LittleEndianDataOutputStream(concatenatedStream);

        List<KeyPointer> metadata = new ArrayList<>();

        long count = 0;
        KeyPointer lastItem = null;
        long coffset = 0;
        int uoffset = 0;
        Iterable<KeyPointer> iterable = stream::iterator;
        for (KeyPointer x : iterable) {
            lastItem = x;
            if (count >= blockSize) {
                coffset = concatenatedStream.getCompressedCount();
                uoffset = (int) concatenatedStream.getUncompressedCount();
                if (uoffset < 1 << 16) { // if uoffset == 1 << 16 we will write next item
                    metadata.add(new KeyPointer(coffset, uoffset, x.getKey(), x.getKey().length));
                    count = 0;
                }
            }
            dataOutput.writeShort(x.getKey().length + 8);
            dataOutput.write(x.getKey());
            dataOutput.writeLong(x.getPointer());
            count++;
        }
        dataOutput.close();

        if (count > 0) {
            metadata.add(new KeyPointer(coffset, uoffset, lastItem.getKey(), lastItem.getKey().length));
        }

        return metadata;
    }

}
