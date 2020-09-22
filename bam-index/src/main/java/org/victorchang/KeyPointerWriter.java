package org.victorchang;


import com.google.common.io.LittleEndianDataOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Write a stream of {@link KeyPointer} into a concatenated gzip output.
 */
public class KeyPointerWriter {
    public List<KeyPointer> write(OutputStream outputStream, Stream<KeyPointer> stream) throws IOException {
        return write(outputStream, stream, Long.MAX_VALUE);
    }

    @SuppressWarnings("UnstableApiUsage")
    public List<KeyPointer> write(OutputStream outputStream, Stream<KeyPointer> stream, long blockSize) throws IOException {
        GzipConcatenatedOutputStream concatenatedStream =
                new GzipConcatenatedOutputStream(outputStream, 1 << 16);

        LittleEndianDataOutputStream dataOutput = new LittleEndianDataOutputStream(concatenatedStream);

        List<KeyPointer> metadata = new ArrayList<>();

        long count = 0;
        KeyPointer lastItem = null;
        Iterable<KeyPointer> iterable = stream::iterator;
        for (KeyPointer x : iterable) {
            lastItem = x;
            if (count >= blockSize) {
                long coffset = concatenatedStream.getCompressedCount();
                int uoffset = (int) concatenatedStream.getUncompressedCount();
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
            long coffset = concatenatedStream.getCompressedCount();
            int uoffset = (int) concatenatedStream.getUncompressedCount();
            metadata.add(new KeyPointer(coffset, uoffset, lastItem.getKey(), lastItem.getKey().length));
        }
        return metadata;
    }

}
