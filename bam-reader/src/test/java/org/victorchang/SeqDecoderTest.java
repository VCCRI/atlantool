package org.victorchang;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SeqDecoderTest {
    @Test
    public void testDecode() {
        byte[] buffer = {40, 17, 34, 40, 17, 34, 40, 17, 34, 40, 17, 34, - 127, 16};
        assertThat(SeqDecoder.INSTANCE.decode(buffer, 0, 27), equalTo("CTAACCCTAACCCTAACCCTAACCTAA"));
    }
}