package org.victorchang;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class QnamePosWriter {
    private static final int FILE_BUFF_SIZE = 8192;

    /**
     * reusable buffer to minimize memory allocation.
     */
    private final byte[] outputBuff;

    public QnamePosWriter() {
        outputBuff = new byte[256 + 8 + 2];
    }

    public Path create(Path path, QnamePos[] buffer, int recordCount) {
        try (FileChannel fileChannel = FileChannel.open(path, CREATE, WRITE, TRUNCATE_EXISTING)) {
            OutputStream outputStream = new BufferedOutputStream(Channels.newOutputStream(fileChannel), FILE_BUFF_SIZE);
            for (int i = 0; i < recordCount; i++) {
                QnamePos current = buffer[i];
                byte[] qnameBuff = current.getQname();
                long position = current.getPosition();
                int len = QnamePosPacker.INSTANCE.pack(outputBuff, qnameBuff, qnameBuff.length, position);
                outputStream.write(outputBuff, 0, len);
            }
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return path;
    }
}
