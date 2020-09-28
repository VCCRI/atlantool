package org.victorchang;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReaderFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class BamFileReaderTest {
    private static final Logger log = LoggerFactory.getLogger(BamFileReaderTest.class);

    @Test
    public void testReadSingleRecordSample() throws IOException, URISyntaxException {
        URL example1 = ClassLoader.getSystemResource("bam/single-record");
        Path path = Paths.get(example1.toURI());

        BamFileReader fileReader = new DefaultBamFileReader();

        SAMFileHeader header = SamReaderFactory.make().getFileHeader(path);
        LastRecordSelector<SAMRecord> handler = new LastRecordSelector<>(new SamRecordParser(header));
        fileReader.read(path, handler);

        assertThat(handler.getLast().getReadString(), equalTo("CCCCAACCCTAACCCTAACCCTAACCCTAACCTAAC"));
        assertThat(handler.getLast().getReadName(), equalTo("SOLEXA-1GA-1_0047_FC62472:5:81:15648:19537#0"));
    }

    @Test
    public void testReadSample1() throws IOException, URISyntaxException {
        URL example1 = ClassLoader.getSystemResource("bam/example1");
        Path path = Paths.get(example1.toURI());

        BamFileReader fileReader = new DefaultBamFileReader();

        RecordCounter handler = new RecordCounter();
        long start = System.nanoTime();
        fileReader.read(path, handler);
        long finish = System.nanoTime();

        assertThat(handler.getRecordCount(), equalTo(918571L));
        log.info(String.format("#records %d, duration %d ms", handler.getRecordCount(),  (finish - start) / 1000_000));
    }

    @Test
    public void testReadSample2() throws IOException, URISyntaxException {
        URL example2 = ClassLoader.getSystemResource("bam/example2");
        Path path = Paths.get(example2.toURI());

        BamFileReader fileReader = new DefaultBamFileReader();

        RecordCounter handler = new RecordCounter();
        long start = System.nanoTime();
        fileReader.read(path, handler);
        long finish = System.nanoTime();

        assertThat(handler.getRecordCount(), equalTo(999234L));
        log.info(String.format("#records %d, duration %d ms", handler.getRecordCount(),  (finish - start) / 1000_000));
    }

    @Test
    public void testReadSample3() throws IOException, URISyntaxException {
        URL example3 = ClassLoader.getSystemResource("bam/example3");
        Path path = Paths.get(example3.toURI());

        BamFileReader fileReader = new DefaultBamFileReader();

        RecordCounter handler = new RecordCounter();
        long start = System.nanoTime();
        fileReader.read(path, handler);
        long finish = System.nanoTime();

        assertThat(handler.getRecordCount(), equalTo(1215970L));
        log.info(String.format("#records %d, duration %d ms", handler.getRecordCount(),  (finish - start) / 1000_000));
    }

}