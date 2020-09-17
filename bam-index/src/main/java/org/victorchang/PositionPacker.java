package org.victorchang;

/**
 * Packs block position and offset into a positive long required by Lucene FST implementation. It assumes that
 * 0 <= position < 2^47 and 0 <= offset < 2^16.
 */
public final class PositionPacker {
    public static PositionPacker INSTANCE = new PositionPacker();

    private PositionPacker() {
    }

    public long pack(long blockPos, int offset) {
        if ((blockPos & 0xffffe00000000000L) != 0L) {
            throw new IllegalArgumentException("Block position must be less than 2^47");
        }
        if ((offset & 0xffff0000) != 0) {
            throw new IllegalArgumentException("Offset must be less than 2^16");
        }
        return (blockPos | (long)offset << 47);
    }

    public long unpackBlockPos(long pos) {
        return (pos & 0x0000effffffffffL);
    }

    public int unpackOffset(long pos) {
        return (int)(pos >> 47 & 0xffff);
    }
}
