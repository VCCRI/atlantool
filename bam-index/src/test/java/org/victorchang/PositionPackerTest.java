package org.victorchang;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class PositionPackerTest {

    @Test
    public void testPackUnpack() {
        long pos = PositionPacker.INSTANCE.pack(36300895L, 59353);

        assertThat(PositionPacker.INSTANCE.unpackBlockPos(pos), equalTo(36300895L));
        assertThat(PositionPacker.INSTANCE.unpackRecordNum(pos), equalTo(59353));
    }
}