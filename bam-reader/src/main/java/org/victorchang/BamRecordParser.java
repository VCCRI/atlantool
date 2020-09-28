package org.victorchang;

import java.io.DataInput;
import java.io.IOException;
import java.util.function.Consumer;

public interface BamRecordParser<T> {
    void parse(DataInput dataInput, int recordLength, Consumer<T> consumer) throws IOException;
}
