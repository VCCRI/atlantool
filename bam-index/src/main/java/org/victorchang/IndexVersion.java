package org.victorchang;

public abstract class IndexVersion {
    private IndexVersion() {
    }

    public static final IndexVersion LATEST = new IndexVersion() {
        @Override
        public String fileName(String ext) {
            return "qname-version" + version() + "." + ext ;
        }

        @Override
        public String version() {
            return "0";
        }

        @Override
        public String toString() {
            return version();
        }
    };

    public abstract String fileName(String ext);
    public abstract String version();
}
