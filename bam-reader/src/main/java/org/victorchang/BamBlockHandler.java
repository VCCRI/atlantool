package org.victorchang;

import java.nio.ByteBuffer;

/**
 * A call back that is invoked by {@link BamFileReader}.
 */
public interface BamBlockHandler {
    /**
     * Call back for each GZIP block.
     *
     * @param byteBuffer a buffer that holds entire GZIP block
     * @param blockPos   the starting position of the block in a concatenate GZIP file
     */
    void onBlock(ByteBuffer byteBuffer, long blockPos);
}
