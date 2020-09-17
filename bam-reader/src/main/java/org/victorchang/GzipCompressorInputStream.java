/*
 * Copy from org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream and modify to correct file
 * position in a concatenated gzip file.
 */
package org.victorchang;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.CharsetNames;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class GzipCompressorInputStream extends CompressorInputStream implements InputStreamStatistics {

    // Header flags
    // private static final int FTEXT = 0x01; // Uninteresting for us
    private static final int FHCRC = 0x02;
    private static final int FEXTRA = 0x04;
    private static final int FNAME = 0x08;
    private static final int FCOMMENT = 0x10;
    private static final int FRESERVED = 0xE0;

    private final CountingInputStream countingStream;

    // Compressed input stream, possibly wrapped in a
    // BufferedInputStream, always wrapped in countingStream above
    private final InputStream in;

    // Buffer to hold the input data
    private final byte[] buf = new byte[8192];

    // Amount of data in buf.
    private int bufUsed;

    // Decompressor
    private Inflater inf = new Inflater(true);

    // CRC32 from uncompressed data
    private final CRC32 crc = new CRC32();

    // True once everything has been decompressed
    private boolean endReached = false;

    // used in no-arg read method
    private final byte[] oneByte = new byte[1];

    private final GzipParameters parameters = new GzipParameters();

    private final GzipEntryEventHandler eventHandler;

    /**
     * Constructs a new input stream that decompresses concatenated gzip-compressed data with event handler.
     */
    public GzipCompressorInputStream(final InputStream inputStream,
                                     final GzipEntryEventHandler eventHandler)
            throws IOException {
        this.eventHandler = eventHandler;
        if (inputStream.markSupported()) {
            countingStream = new CountingInputStream(inputStream);
        } else {
            countingStream =  new CountingInputStream(new BufferedInputStream(inputStream));
        }
        in = countingStream;

        init(true);
    }

    public GzipParameters getMetaData() {
        return parameters;
    }

    private boolean init(final boolean isFirstMember) throws IOException {
        eventHandler.onStart(getCompressedCount(), getUncompressedCount());

        // Check the magic bytes without a possibility of EOFException.
        final int magic0 = in.read();

        // If end of input was reached after decompressing at least
        // one .gz member, we have reached the end of the file successfully.
        if (magic0 == -1 && !isFirstMember) {
            return false;
        }

        if (magic0 != 31 || in.read() != 139) {
            throw new IOException(isFirstMember
                    ? "Input is not in the .gz format"
                    : "Garbage after a valid .gz stream");
        }

        // Parsing the rest of the header may throw EOFException.
        final DataInput inData = new DataInputStream(in);
        final int method = inData.readUnsignedByte();
        if (method != Deflater.DEFLATED) {
            throw new IOException("Unsupported compression method "
                    + method + " in the .gz header");
        }

        final int flg = inData.readUnsignedByte();
        if ((flg & FRESERVED) != 0) {
            throw new IOException(
                    "Reserved flags are set in the .gz header");
        }

        parameters.setModificationTime(ByteUtils.fromLittleEndian(inData, 4) * 1000);
        switch (inData.readUnsignedByte()) { // extra flags
            case 2:
                parameters.setCompressionLevel(Deflater.BEST_COMPRESSION);
                break;
            case 4:
                parameters.setCompressionLevel(Deflater.BEST_SPEED);
                break;
            default:
                // ignored for now
                break;
        }
        parameters.setOperatingSystem(inData.readUnsignedByte());

        // Extra field, ignored
        if ((flg & FEXTRA) != 0) {
            int xlen = inData.readUnsignedByte();
            xlen |= inData.readUnsignedByte() << 8;

            // This isn't as efficient as calling in.skip would be,
            // but it's lazier to handle unexpected end of input this way.
            // Most files don't have an extra field anyway.
            while (xlen-- > 0) {
                inData.readUnsignedByte();
            }
        }

        // Original file name
        if ((flg & FNAME) != 0) {
            parameters.setFilename(new String(readToNull(inData),
                    CharsetNames.ISO_8859_1));
        }

        // Comment
        if ((flg & FCOMMENT) != 0) {
            parameters.setComment(new String(readToNull(inData),
                    CharsetNames.ISO_8859_1));
        }

        // Header "CRC16" which is actually a truncated CRC32 (which isn't
        // as good as real CRC16). I don't know if any encoder implementation
        // sets this, so it's not worth trying to verify it. GNU gzip 1.4
        // doesn't support this field, but zlib seems to be able to at least
        // skip over it.
        if ((flg & FHCRC) != 0) {
            inData.readShort();
        }

        // Reset
        inf.reset();
        crc.reset();

        return true;
    }

    private static byte[] readToNull(final DataInput inData) throws IOException {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            int b = 0;
            while ((b = inData.readUnsignedByte()) != 0x00) { // NOPMD NOSONAR
                bos.write(b);
            }
            return bos.toByteArray();
        }
    }

    @Override
    public int read() throws IOException {
        return read(oneByte, 0, 1) == -1 ? -1 : oneByte[0] & 0xFF;
    }

    @Override
    public int read(final byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (endReached) {
            return -1;
        }

        int size = 0;

        while (len > 0) {
            if (inf.needsInput()) {
                // Remember the current position because we may need to
                // rewind after reading too much input.
                in.mark(buf.length);

                bufUsed = in.read(buf);
                if (bufUsed == -1) {
                    throw new EOFException();
                }

                inf.setInput(buf, 0, bufUsed);
            }

            int ret;
            try {
                ret = inf.inflate(b, off, len);
            } catch (final DataFormatException e) { // NOSONAR
                throw new IOException("Gzip-compressed data is corrupt");
            }

            crc.update(b, off, ret);
            off += ret;
            len -= ret;
            size += ret;
            count(ret);

            if (inf.finished()) {
                // We may have read too many bytes. Rewind the read
                // position to match the actual amount used.
                in.reset();

                final int skipAmount = bufUsed - inf.getRemaining();
                if (IOUtils.skip(in, skipAmount) != skipAmount) {
                    throw new IOException();
                }

                bufUsed = 0;

                final DataInput inData = new DataInputStream(in);

                // CRC32
                final long crcStored = ByteUtils.fromLittleEndian(inData, 4);

                if (crcStored != crc.getValue()) {
                    throw new IOException("Gzip-compressed data is corrupt "
                            + "(CRC32 error)");
                }

                // Uncompressed size modulo 2^32 (ISIZE in the spec)
                final long isize = ByteUtils.fromLittleEndian(inData, 4);

                if (isize != (inf.getBytesWritten() & 0xffffffffL)) {
                    throw new IOException("Gzip-compressed data is corrupt"
                            + "(uncompressed size mismatch)");
                }

                // See if this is the end of the file.
                if (!init(false)) {
                    inf.end();
                    inf = null;
                    endReached = true;
                    return size == 0 ? -1 : size;
                }
            }
        }

        return size;
    }

    @Override
    public void close() throws IOException {
        if (inf != null) {
            inf.end();
            inf = null;
        }

        if (this.in != System.in) {
            this.in.close();
        }
    }

    @Override
    public long getCompressedCount() {
        return countingStream.getBytesRead();
    }
}
