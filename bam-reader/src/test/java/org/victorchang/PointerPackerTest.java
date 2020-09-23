package org.victorchang;

import htsjdk.samtools.util.BlockCompressedFilePointerUtil;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class PointerPackerTest {

    @Test
    public void testPackUnpack() {
        roundtrip(36300895L, 59353);
        roundtrip(0, 0);
        roundtrip(BlockCompressedFilePointerUtil.MAX_BLOCK_ADDRESS, BlockCompressedFilePointerUtil.MAX_OFFSET);
    }

    private static void roundtrip(long coffset, int uoffset) {
        long pos = PointerPacker.INSTANCE.pack(coffset, uoffset);

        assertThat(PointerPacker.INSTANCE.unpackCompressedOffset(pos), equalTo(coffset));
        assertThat(PointerPacker.INSTANCE.unpackUncompressedOffset(pos), equalTo(uoffset));
    }
}