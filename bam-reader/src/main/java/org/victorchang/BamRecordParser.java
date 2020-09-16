package org.victorchang;

import java.io.DataInput;
import java.io.IOException;

public interface BamRecordParser {
    void parse(DataInput dataInput, BamRecordHandler handler) throws IOException;
}
