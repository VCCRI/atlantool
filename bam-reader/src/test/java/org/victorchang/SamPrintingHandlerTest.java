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
    public void testSamOutputWithoutHeader() throws Exception {
        URL example1 = ClassLoader.getSystemResource("bam/single-record");
        Path path = Paths.get(example1.toURI());

        BamFileReader fileReader = new DefaultBamFileReader(new SamtoolsBasedParser());

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SamPrintingHandler handler = new SamPrintingHandler(outputStream, false);
        fileReader.read(path, handler);
        handler.finish();

        assertThat(new String(outputStream.toByteArray()), equalTo(
                "SOLEXA-1GA-1_0047_FC62472:5:81:15648:19537#0\t16\tchr1\t10148\t25\t36M\t*\t0\t0\tCCCCAACCCTAACCCTAACCCTAACCCTAACCTAAC\tB8='35:@;+30;B@+CAFFFGGEGGGGGGGGEGGG\tX1:i:1\tMD:Z:32C3\tNM:i:1\n"));
    }

    @Test
    public void testSamOutputWithHeader() throws Exception {
        URL example1 = ClassLoader.getSystemResource("bam/single-record");
        Path path = Paths.get(example1.toURI());

        BamFileReader fileReader = new DefaultBamFileReader(new SamtoolsBasedParser());

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SamPrintingHandler handler = new SamPrintingHandler(outputStream, true);
        fileReader.read(path, handler);
        handler.finish();

        assertThat(new String(outputStream.toByteArray()), equalTo(
                "@HD\tVN:1.6\n" +
                        "@SQ\tSN:chr1\tLN:249250621\n" +
                        "@SQ\tSN:chr2\tLN:243199373\n" +
                        "@SQ\tSN:chr3\tLN:198022430\n" +
                        "@SQ\tSN:chr4\tLN:191154276\n" +
                        "@SQ\tSN:chr5\tLN:180915260\n" +
                        "@SQ\tSN:chr6\tLN:171115067\n" +
                        "@SQ\tSN:chr7\tLN:159138663\n" +
                        "@SQ\tSN:chr8\tLN:146364022\n" +
                        "@SQ\tSN:chr9\tLN:141213431\n" +
                        "@SQ\tSN:chr10\tLN:135534747\n" +
                        "@SQ\tSN:chr11\tLN:135006516\n" +
                        "@SQ\tSN:chr12\tLN:133851895\n" +
                        "@SQ\tSN:chr13\tLN:115169878\n" +
                        "@SQ\tSN:chr14\tLN:107349540\n" +
                        "@SQ\tSN:chr15\tLN:102531392\n" +
                        "@SQ\tSN:chr16\tLN:90354753\n" +
                        "@SQ\tSN:chr17\tLN:81195210\n" +
                        "@SQ\tSN:chr18\tLN:78077248\n" +
                        "@SQ\tSN:chr19\tLN:59128983\n" +
                        "@SQ\tSN:chr20\tLN:63025520\n" +
                        "@SQ\tSN:chr21\tLN:48129895\n" +
                        "@SQ\tSN:chr22\tLN:51304566\n" +
                        "@SQ\tSN:chrM\tLN:16571\n" +
                        "@SQ\tSN:chrX\tLN:155270560\n" +
                        "@PG\tID:samtools\tPN:samtools\tVN:1.10\tCL:samtools view -h 1G.bam\n" +
                        "@PG\tID:samtools.1\tPN:samtools\tPP:samtools\tVN:1.10\tCL:samtools view -b -h\n" +
                        "@PG\tID:samtools.2\tPN:samtools\tPP:samtools.1\tVN:1.10\tCL:samtools view -h example2.bam\n" +
                        "@PG\tID:samtools.3\tPN:samtools\tPP:samtools.2\tVN:1.10\tCL:samtools view -b\n" +
                        "SOLEXA-1GA-1_0047_FC62472:5:81:15648:19537#0\t16\tchr1\t10148\t25\t36M\t*\t0\t0\tCCCCAACCCTAACCCTAACCCTAACCCTAACCTAAC\tB8='35:@;+30;B@+CAFFFGGEGGGGGGGGEGGG\tX1:i:1\tMD:Z:32C3\tNM:i:1\n"));
    }

}
