package org.victorchang;

/**
 * Packs compressed offset {@code coffset} and uncompressed offset {@code uoffset} into a pointer {@code voffset}
 * which is 64 bit long. It assumes that 0 <= coffset < 2^48 and 0 <= uoffset < 2^16.
 */
public final class PointerPacker {
    public static PointerPacker INSTANCE = new PointerPacker();

    private PointerPacker() {
    }

    public long pack(long coffset, int uoffset) {
        if ((coffset & 0xffff000000000000L) != 0L) {
            throw new IllegalArgumentException("Block position must be less than 2^48");
        }
        if ((uoffset & 0xffff0000) != 0) {
            throw new IllegalArgumentException("Offset must be less than 2^16");
        }
        return (coffset | (long)uoffset << 48);
    }

    public long unpackCompressedOffset(long voffset) {
        return (voffset & 0x0000fffffffffffL);
    }

    public int unpackUnCompressedOffset(long voffset) {
        return (int)(voffset >> 48 & 0xffff);
    }
}
