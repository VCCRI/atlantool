package org.victorchang;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class PointerPackerTest {

    @Test
    public void testPackUnpack() {
        long pos = PointerPacker.INSTANCE.pack(36300895L, 59353);

        assertThat(PointerPacker.INSTANCE.unpackCompressedOffset(pos), equalTo(36300895L));
        assertThat(PointerPacker.INSTANCE.unpackUnCompressedOffset(pos), equalTo(59353));
    }
}