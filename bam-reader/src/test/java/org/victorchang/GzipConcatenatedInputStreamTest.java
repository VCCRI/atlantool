package org.victorchang;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.READ;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

public class GzipConcatenatedInputStreamTest {
    private static final Logger log = LoggerFactory.getLogger(BamFileReaderTest.class);

    private static final int BUFF_SIZE = 1 << 15;
    private static final int EOF = -1;

    @Test
    public void testExample1() throws IOException, URISyntaxException {
        URL example1 = ClassLoader.getSystemResource("bam/example1");
        Path path = Paths.get(example1.toURI());

        GzipEntryPositionCollector testHandler = new GzipEntryPositionCollector();
        long start = System.nanoTime();

        try (FileChannel fileChannel = FileChannel.open(path, READ)) {
            InputStream markSupportedInputStream = new BufferedInputStream(Channels.newInputStream(fileChannel), BUFF_SIZE);
            InputStream gzipCompressorInputStream = new BufferedInputStream(
                    new GzipConcatenatedInputStream(markSupportedInputStream, testHandler), BUFF_SIZE);

            byte[] buff = new byte[BUFF_SIZE];
            while (gzipCompressorInputStream.read(buff) != EOF);
        }
        long finish = System.nanoTime();
        log.info("duration " + (finish - start) / 1_000_000 + "ms");

        assertThat(testHandler.getCompressed().size(), equalTo(1870));
        assertThat(testHandler.getUncompressed().size(), equalTo(1870));
        assertThat(testHandler.getCompressed().subList(0, 4), contains(0L, 222L, 22720L, 45918L));
        assertThat(testHandler.getUncompressed().subList(0, 4), contains(0L, 337L, 65862L, 131274L));
    }

}