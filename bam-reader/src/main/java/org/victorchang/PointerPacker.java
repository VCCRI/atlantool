package org.victorchang;

import htsjdk.samtools.util.BlockCompressedFilePointerUtil;

/**
 * Packs compressed offset {@code coffset} and uncompressed offset {@code uoffset} into a pointer {@code voffset}
 * which is 64 bit long. It assumes that 0 <= coffset < 2^48 and 0 <= uoffset < 2^16.
 */
public final class PointerPacker {
    public static PointerPacker INSTANCE = new PointerPacker();

    private PointerPacker() {
    }

    public long pack(long coffset, int uoffset) {
        return BlockCompressedFilePointerUtil.makeFilePointer(coffset, uoffset);
    }

    public long unpackCompressedOffset(long voffset) {
        return BlockCompressedFilePointerUtil.getBlockAddress(voffset);
    }

    public int unpackUnCompressedOffset(long voffset) {
        return BlockCompressedFilePointerUtil.getBlockOffset(voffset);
    }
}
