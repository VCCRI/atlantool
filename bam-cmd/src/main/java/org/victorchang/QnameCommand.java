package org.victorchang;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

@Command(
        subcommands = {
                IndexCommand.class,
                ViewCommand.class
        },
        name = "atlantool",
        header = "A command line tool for viewing records in a BAM file by QNAME",
        description = "This tool provides option to create an index for a BAM file based on QNAME and then search records based on QNAME.",
        versionProvider = CommitVersionProvider.class
)
public class QnameCommand {
    @CommandLine.Option(names = { "-V", "--version" }, versionHelp = true,
            description = "Print version information and exit")
    private boolean versionRequested;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new QnameCommand()).execute(args);
        System.exit(exitCode);
    }

    static Path getDefaultIndexPath(Path bamPath) {
        final String fileName = bamPath.getFileName().toString();
        final String parent = Optional
                .ofNullable(bamPath.getParent())
                .map(Path::toString)
                .orElse("");
        return Path.of(parent, fileName + ".atlantool-index");
    }
}

class CommitVersionProvider implements CommandLine.IVersionProvider {

    private final static String BASE_VERSION = "1.0";

    @Override
    public String[] getVersion() {
        String commitId = "Unknown";
        String buildTime = "Unknown";
        Properties p = new Properties();
        try {
            final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("git.properties");
            p.load(resourceAsStream);
            commitId = p.getProperty("git.commit.id.abbrev");
            buildTime = p.getProperty("git.build.time");
        } catch (Exception ignored) {
        }
        return new String[] {
                "Version: " + BASE_VERSION,
                "Index version: " + IndexVersion.LATEST.version(),
                "Release: " + commitId,
                "Release Date: " + buildTime
        };
    }
}
