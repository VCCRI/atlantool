package org.victorchang;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class QnamePosWriter {
    private static final int FILE_BUFF_SIZE = 8192;

    public Path create(Path path, QnamePosBuffer buffer) {
        try (FileChannel fileChannel = FileChannel.open(path, CREATE, WRITE, TRUNCATE_EXISTING)) {
            OutputStream gzipOutputStream = new GZIPOutputStream(Channels.newOutputStream(fileChannel));
            OutputStream outputStream = new BufferedOutputStream(gzipOutputStream, FILE_BUFF_SIZE);
            buffer.writeSorted(outputStream);
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return path;
    }
}
