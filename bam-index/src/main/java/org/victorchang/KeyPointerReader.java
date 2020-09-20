package org.victorchang;

import com.google.common.collect.Streams;
import com.google.common.io.LittleEndianDataInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Read a stream of {@link KeyPointer} from a concatenated gzip input.
 */
public class KeyPointerReader {
    private static final Logger log = LoggerFactory.getLogger(KeyPointerReader.class);
    private static final int FILE_BUFF_SIZE = 8192;

    public Stream<KeyPointer> read(InputStream inputStream) {
        return read(inputStream, 0, (compressedCount, uncompressedCount) -> {
        });
    }
    public Stream<KeyPointer> read(InputStream inputStream, int offset) {
        return read(inputStream, offset, (compressedCount, uncompressedCount) -> {
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    public Stream<KeyPointer> read(InputStream inputStream, int offset, GzipEntryEventHandler eventHandler) {
        InputStream concatenatedStream;
        try {
            concatenatedStream = new BufferedInputStream(
                    new GzipConcatenatedInputStream(inputStream, eventHandler), FILE_BUFF_SIZE);
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }

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
