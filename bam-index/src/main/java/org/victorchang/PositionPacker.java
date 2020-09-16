package org.victorchang;

public final class PositionPacker {
    public static PositionPacker INSTANCE = new PositionPacker();

    private PositionPacker() {
    }

    public long pack(long blockPos, int offset) {
        if ((blockPos & 0xffffe00000000000L) != 0L) {
            throw new IllegalArgumentException("Block position must be less than 2^47");
        }
        if ((offset & 0xffff0000) != 0) {
            throw new IllegalArgumentException("Record number must be less than 2^16");
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
