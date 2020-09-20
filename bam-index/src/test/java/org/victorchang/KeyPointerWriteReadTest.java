package org.victorchang;


import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class KeyPointerWriteReadTest {
    @Test
    public void testWriteRead() throws IOException {
        KeyPointerWriter writer = new KeyPointerWriter();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] qname = Ascii7Coder.INSTANCE.encode("SOLEXA-1GA-1_4_FC20ENL:7:172:55:704");

        List<KeyPointer> written = IntStream.range(0, 2048)
                .mapToObj(x -> {
                    qname[qname.length - 1] = (byte) (x % 256);
                    return new KeyPointer(36300895L + x, 59353, qname, qname.length);
                })
                .collect(Collectors.toList());

        writer.write(output, written.stream());
        output.close();

        KeyPointerReader reader = new KeyPointerReader();
        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());

        List<KeyPointer> read = reader.read(input).collect(Collectors.toList());
        input.close();

        assertThat(read, equalTo(written));
    }
}