package org.victorchang;

import com.google.common.io.LittleEndianDataOutputStream;
import htsjdk.samtools.util.BlockCompressedOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.Deflater;

import static htsjdk.samtools.util.BlockCompressedFilePointerUtil.getBlockAddress;

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
    public List<KeyPointer> write(OutputStream outputStream, Stream<KeyPointer> stream, long approximateBlockSize) throws IOException {
        BlockCompressedOutputStream blockCompressedOutputStream = new BlockCompressedOutputStream(outputStream, (File) null, compressionLevel);

        LittleEndianDataOutputStream dataOutput = new LittleEndianDataOutputStream(blockCompressedOutputStream);

        List<KeyPointer> metadata = new ArrayList<>();

        long count = 0;
        KeyPointer lastItem = null;
        long lastFilePointer = 0;
        Iterable<KeyPointer> iterable = stream::iterator;
        for (KeyPointer x : iterable) {

            if (count >= approximateBlockSize) {
                // We have enough records for a metadata pointer for the index now. The check below is to try to align
                // the pointer towards the start of a block. Why? Because that means we skip over less gzipped content
                // to get to our pointer location. If the pointer was towards the end of a block, we'd do more
                // unnecessary decompression.
                long filePointer = blockCompressedOutputStream.getFilePointer();
                if (getBlockAddress(filePointer) != getBlockAddress(lastFilePointer)) {
                    metadata.add(new KeyPointer(filePointer, x.getKey(), x.getKey().length));
                    count = 0;
                }
            }

            lastItem = x;
            lastFilePointer = blockCompressedOutputStream.getFilePointer();

            dataOutput.writeByte(x.getKey().length);
            dataOutput.write(x.getKey());
            dataOutput.writeLong(x.getPointer());
            count++;
        }
        dataOutput.close();

        if (count > 0) {
            metadata.add(new KeyPointer(lastFilePointer, lastItem.getKey(), lastItem.getKey().length));
        }

        return metadata;
    }

}
