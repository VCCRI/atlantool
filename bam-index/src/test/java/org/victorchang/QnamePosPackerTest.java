package org.victorchang;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class QnamePosPackerTest {

    @Test
    public void testPackUnpack() {
        byte[] buffer = new byte[256 + 8 + 2];

        String qname = "SOLEXA-1GA-1_4_FC20ENL:7:172:55:704";
        long position = PositionPacker.INSTANCE.pack(36300895L, 59353);
        byte[] qnameBuffer = Ascii7Coder.INSTANCE.encode(qname);

        int len = QnamePosPacker.INSTANCE.pack(buffer, qnameBuffer, qnameBuffer.length, position);

        assertThat(QnamePosPacker.INSTANCE.unpackPosition(buffer, 2, len - 2), equalTo(position));

        StringBuilder builder = new StringBuilder();
        QnamePosPacker.INSTANCE.unpackQname(buffer, 2, len - 2, b -> builder.append((char)b));

        builder.deleteCharAt(builder.length() - 1);
        assertThat(builder.toString(), equalTo(qname));
    }
}