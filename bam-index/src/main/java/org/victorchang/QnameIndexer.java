package org.victorchang;

import java.io.IOException;
import java.nio.file.Path;

public class QnameIndexer {
    private final BamFileReader fileReader;
    private final int maxRecord;

    public QnameIndexer(BamFileReader fileReader, int maxRecord) {
        this.fileReader = fileReader;
        this.maxRecord = maxRecord;
    }

    public void createIndex(Path indexFolder, Path bamFile) throws IOException {
        FstFactory fstFactory = new FstFactory(indexFolder);
        QnameCollector collector = new QnameCollector(fstFactory::create, maxRecord);

        fileReader.read(bamFile, collector);
        collector.flush();
    }
}
