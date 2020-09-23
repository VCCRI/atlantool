package org.victorchang;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A pair of an arbitrary byte array key and a 64 bits pointer.
 */
public class KeyPointer implements Comparable<KeyPointer> {
    private final byte[] key;
    private final long pointer;

    public KeyPointer(long coffset, int uoffset, byte[] key, int keyLen) {
        this.key = new byte[keyLen];
        System.arraycopy(key, 0, this.key, 0, keyLen);
        this.pointer = PointerPacker.INSTANCE.pack(coffset, uoffset);
    }

    public KeyPointer(long pointer, byte[] key, int keyLen) {
        this.pointer = pointer;
        this.key = new byte[keyLen];
        System.arraycopy(key, 0, this.key, 0, keyLen);
    }

    public long getPointer() {
        return pointer;
    }

    public byte[] getKey() {
        return key;
    }

    @Override
    public int compareTo(KeyPointer that) {
        return Arrays.compareUnsigned(this.key, that.key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KeyPointer)) return false;
        KeyPointer that = (KeyPointer) o;
        return getPointer() == that.getPointer() &&
                Arrays.equals(getKey(), that.getKey());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getPointer());
        result = 31 * result + Arrays.hashCode(getKey());
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ")
                .add("coffset=" + PointerPacker.INSTANCE.unpackCompressedOffset(pointer))
                .add("uoffset=" + PointerPacker.INSTANCE.unpackUncompressedOffset(pointer))
                .add("key=" + Ascii7Coder.INSTANCE.decode(key, 0, key.length))
                .toString();
    }
}
