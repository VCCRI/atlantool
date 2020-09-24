package it.org.victorchang;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static java.lang.ProcessBuilder.Redirect.INHERIT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class IndexAndSearchIntegrationTest {

    private static final String BAM_PATH = "target/test-classes/example.bam";
    private static final String JAR_PATH = "target/atlantool.jar";

    @BeforeClass
    public static void beforeClass() throws Exception {
        index();
    }

    @Test
    public void searchExistingQname() throws Exception {
        final List<String> result = search("SOLEXA-1GA-1_0047_FC62472:5:96:2774:14669#0");
        assertThat(result.size(), is(1));
        assertThat(result, contains(
                "SOLEXA-1GA-1_0047_FC62472:5:96:2774:14669#0\t0\tchr1\t56244\t25\t36M\t*\t0\t0\tGGCCAACTCCCTGCAACTTATTTCTGCCTAGATTCT\t4:DB<8@AA=B?D?DG=BB@BB;7B,@?8=BDDD:G\tX0:i:1\tMD:Z:36\tNM:i:0"
        ));
    }

    @Test
    public void searchMissingQname() throws Exception {
        final List<String> result = search("SOME-MISSING-QNAME");
        assertThat(result, empty());
    }

    private List<String> search(String qname) throws Exception {
        final File output = File.createTempFile("atlantool", ".redirect");
        final int searchExitCode = new ProcessBuilder()
                .command("java", "-jar", JAR_PATH, "view", BAM_PATH, "-n", qname)
                .redirectOutput(output)
                .redirectError(INHERIT)
                .start()
                .waitFor();
        assertThat(searchExitCode, equalTo(0));
        return Files.readAllLines(output.toPath());
    }

    private static void index() throws Exception {
        final int indexExitCode = new ProcessBuilder()
                .command("java", "-jar", JAR_PATH, "index", BAM_PATH, "--force")
                .inheritIO()
                .start()
                .waitFor();
        assertThat(indexExitCode, equalTo(0));
    }

}
