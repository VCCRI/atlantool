package org.victorchang;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

public class GzipConcatenatedOutputStreamTest {
    private static final int EOF = -1;

    @Test
    public void testCreateConcatenatedGzip() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GzipConcatenatedOutputStream concatenatedOutput = new GzipConcatenatedOutputStream(output, 512, 6);
        byte[] buffer = new byte[384];
        concatenatedOutput.write(buffer);
        concatenatedOutput.write(buffer, 0, buffer.length);
        concatenatedOutput.write(7);
        concatenatedOutput.close();

        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        GzipEntryPositionCollector handler = new GzipEntryPositionCollector();
        GzipConcatenatedInputStream concatenatedInput = new GzipConcatenatedInputStream(input, handler);
        while (concatenatedInput.read() != EOF) ;
        concatenatedInput.close();

        assertThat(handler.getCompressed(), contains(0L, 25L));
        assertThat(handler.getUncompressed(), contains(0L, 384L));
    }
}