package org.victorchang;

import com.google.common.collect.Streams;
import com.google.common.io.LittleEndianDataInputStream;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.READ;

public class QnamePosReader {
    private static final int FILE_BUFF_SIZE = 8192;

    /**
     * reusable buffer to minimize memory allocation.
     */
    private final byte[] inputBuff;

    public QnamePosReader() {
        inputBuff = new byte[256 + 8 + 2];
    }

    @SuppressWarnings("UnstableApiUsage")
    public Stream<QnamePos> read(Path path) {
        FileChannel fileChannel;
        try {
            fileChannel = FileChannel.open(path, READ);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        InputStream inputStream = new BufferedInputStream(Channels.newInputStream(fileChannel), FILE_BUFF_SIZE);
        LittleEndianDataInputStream dataInput = new LittleEndianDataInputStream(inputStream);

        Iterator<QnamePos> iterator = new Iterator<>() {
            private QnamePos current = null;

            @Override
            public boolean hasNext() {
                if (current == null) {
                    try {
                        int recordLen;
                        try {
                            recordLen = dataInput.readShort();
                        } catch (EOFException ignored) {
                            return false;
                        }
                        dataInput.readFully(inputBuff, 0, recordLen);
                        long position = QnamePosPacker.INSTANCE.unpackPosition(inputBuff, 0, recordLen);
                        current = new QnamePos(position, inputBuff, recordLen - 8);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return true;
            }

            @Override
            public QnamePos next() {
                QnamePos ans = current;
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
