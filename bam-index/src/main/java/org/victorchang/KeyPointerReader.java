package org.victorchang;

import com.google.common.collect.Streams;
import com.google.common.io.LittleEndianDataInputStream;
import htsjdk.samtools.util.BlockCompressedInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Read a stream of {@link KeyPointer} from a concatenated gzip input.
 */
public class KeyPointerReader {

    public Stream<KeyPointer> read(InputStream inputStream) {
        return read(inputStream, 0);
    }

    @SuppressWarnings("UnstableApiUsage")
    public Stream<KeyPointer> read(InputStream inputStream, int offset) {
        InputStream concatenatedStream = new BlockCompressedInputStream(inputStream);

        LittleEndianDataInputStream dataInput = new LittleEndianDataInputStream(concatenatedStream);
        while (offset > 0) {
            try {
                offset -= dataInput.skipBytes(offset);
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        }

        Iterator<KeyPointer> iterator = new Iterator<>() {
            /**
             * reusable buffer to minimize memory allocation.
             */
            private final byte[] inputBuff = new byte[256 + 8 + 2];
            private KeyPointer current = null;

            @Override
            public boolean hasNext() {
                if (current == null) {
                    try {
                        int entryLen;
                        try {
                            entryLen = dataInput.readShort();
                        } catch (EOFException ignored) {
                            return false;
                        }
                        dataInput.readFully(inputBuff, 0, entryLen - 8);
                        long pointer = dataInput.readLong();
                        current = new KeyPointer(pointer, inputBuff, entryLen - 8);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return true;
            }

            @Override
            public KeyPointer next() {
                KeyPointer ans = current;
                current = null;
                return ans;
            }
        };
        return Streams.stream(iterator).onClose(() -> {
            try {
                dataInput.close();
            } catch (IOException ignored) {
            }
        });
    }
}
