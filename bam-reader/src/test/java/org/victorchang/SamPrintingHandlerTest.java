package org.victorchang;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class SamPrintingHandlerTest {

    @Test
    public void testSamOutput() throws Exception {
        URL example1 = ClassLoader.getSystemResource("bam/single-record");
        Path path = Paths.get(example1.toURI());

        BamFileReader fileReader = new DefaultBamFileReader(new SamtoolsBasedParser());

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SamPrintingHandler handler = new SamPrintingHandler(outputStream);
        fileReader.read(path, handler);
        handler.finish();

        assertThat(new String(outputStream.toByteArray()), equalTo(
                "SOLEXA-1GA-1_0047_FC62472:5:81:15648:19537#0\t16\tchr1\t10148\t25\t36M\t*\t0\t0\tCCCCAACCCTAACCCTAACCCTAACCCTAACCTAAC\tB8='35:@;+30;B@+CAFFFGGEGGGGGGGGEGGG\tX1:i:1\tMD:Z:32C3\tNM:i:1\n"));
    }

}
