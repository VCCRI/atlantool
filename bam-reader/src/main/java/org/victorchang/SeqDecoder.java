package org.victorchang;

import java.nio.charset.StandardCharsets;

public final class SeqDecoder {
    public static final SeqDecoder INSTANCE = new SeqDecoder();

    private static final byte[] LETTERS = {'=','A','C','M','G','R','S','V','T','W','Y','H','K','D','B','N'};

    private SeqDecoder() {
    }

    public String decode(byte[] buffer, int offset, int seqLen) {
        byte[] ascii = new byte[seqLen];
        for (int i = 0; i < (seqLen + 1) /2; i++) {
            ascii[2*i] = LETTERS[(buffer[offset + i] & 0xFF) >> 4];
            if (2*i + 1 < seqLen) {
                ascii[2 * i + 1] = LETTERS[buffer[offset + i] & 0xF];
            }
        }
        return new String(ascii, 0, seqLen, StandardCharsets.US_ASCII);
    }
}
