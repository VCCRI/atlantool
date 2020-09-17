package org.victorchang;

import com.google.common.collect.Iterators;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QnameIndexer {
    private final BamFileReader bamFileReader;
    private final QnamePosWriter qnamePosWriter;
    private final QnamePosReader qnamePosReader;
    private final int maxRecord;

    public QnameIndexer(BamFileReader bamFileReader,
                        QnamePosWriter qnamePosWriter,
                        QnamePosReader qnamePosReader,
                        int maxRecord) {
        this.bamFileReader = bamFileReader;
        this.qnamePosWriter = qnamePosWriter;
        this.qnamePosReader = qnamePosReader;
        this.maxRecord = maxRecord;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void createIndex(Path indexFolder, Path bamFile) throws IOException {
        SortedQnameFileFactory qnameFileFactory = new SortedQnameFileFactory(indexFolder, qnamePosWriter);

        QnamePosCollector collector = new QnamePosCollector(qnameFileFactory::create, maxRecord);
        bamFileReader.read(bamFile, collector);
        collector.flush();

        List<Path> files = Files.list(indexFolder)
                .filter(this::isSortedQname)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());

        List<Stream<QnamePos>> sorted = files.stream()
                .map(qnamePosReader::read)
                .collect(Collectors.toList());

        Iterator<QnamePos> merged = Iterators.mergeSorted(sorted.stream()
                .map(BaseStream::iterator)
                .collect(Collectors.toList()), QnamePos::compareTo);

        QnameFstBuilder fstBuilder = new QnameFstBuilder(indexFolder, maxRecord);
        while (merged.hasNext()) {
            QnamePos qnamePos = merged.next();
            fstBuilder.add(qnamePos);
        }
        fstBuilder.flush();

        Path rangesPath = indexFolder.resolve("ranges.sorted");
        List<QnamePos> ranges = fstBuilder.getQnameRanges();
        qnamePosWriter.create(rangesPath, ranges.toArray(new QnamePos[0]), ranges.size());

        sorted.forEach(BaseStream::close);

        for (Path path : files) {
            Files.delete(path);
        }
    }

    private boolean isSortedQname(Path path) {
        return path.getFileName().toString().startsWith("qname") &&
                path.getFileName().toString().endsWith(".sorted");
    }
}
