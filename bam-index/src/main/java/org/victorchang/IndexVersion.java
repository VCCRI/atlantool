package org.victorchang;

public abstract class IndexVersion {
    private IndexVersion() {
    }

    public static final IndexVersion LATEST = new IndexVersion() {
        @Override
        public String fileName(String type) {
            return "qname.v" + version() + "." + type +  ".bgz";
        }

        /**
         * Increase this number when modifying the index format.
         */
        @Override
        public int version() {
            return 1;
        }

        @Override
        public String toString() {
            return "v" + version();
        }
    };

    public abstract String fileName(String ext);
    public abstract int version();
}
