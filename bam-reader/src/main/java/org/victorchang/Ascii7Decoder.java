package org.victorchang;

import java.nio.charset.StandardCharsets;

public final class Ascii7Decoder {
    public static final Ascii7Decoder INSTANCE = new Ascii7Decoder();

    private Ascii7Decoder() {
    }

    public String decode(byte[] buffer, int offset, int len) {
        while (len > 0 && buffer[len - 1] == 0) { // ignore terminated null
            len--;
        }
        return new String(buffer, offset, len, StandardCharsets.US_ASCII);
    }
}
