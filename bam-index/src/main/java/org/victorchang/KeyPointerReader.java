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
        InputStream blockCompressedInputStream = new BlockCompressedInputStream(inputStream);

        LittleEndianDataInputStream dataInput = new LittleEndianDataInputStream(blockCompressedInputStream);
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
            private final byte[] inputBuff = new byte[256];
            private KeyPointer current = null;

            @Override
            public boolean hasNext() {
                if (current == null) {
                    try {
                        int keyLen;
                        try {
                            keyLen = dataInput.readUnsignedByte();
                        } catch (EOFException ignored) {
                            return false;
                        }
                        dataInput.readFully(inputBuff, 0, keyLen);
                        long pointer = dataInput.readLong();
                        current = new KeyPointer(pointer, inputBuff, keyLen);
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
