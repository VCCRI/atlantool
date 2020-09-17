package org.victorchang;

import java.nio.charset.StandardCharsets;

public final class Ascii7Coder {
    public static final Ascii7Coder INSTANCE = new Ascii7Coder();

    private Ascii7Coder() {
    }

    public String decode(byte[] buffer, int offset, int len) {
        return new String(buffer, offset, len - 1, StandardCharsets.US_ASCII);
    }

    public byte[] encode(String qname) {
        byte[] encoded = new byte[qname.length() + 1]; // add 1 byte for \x0 terminated
        byte[] bytes = qname.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, encoded, 0, bytes.length);
        return encoded;
    }
}
