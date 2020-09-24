package org.victorchang;

public enum IndexVersion {
    VERSION0 {
        @Override
        public String fileName(String ext) {
            return "qname-version0." + ext ;
        }
    };
    public abstract String fileName(String ext);
}
