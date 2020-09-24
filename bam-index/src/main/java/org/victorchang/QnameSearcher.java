package org.victorchang;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.READ;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class QnameSearcher {

    private final KeyPointerReader keyPointerReader;
    private final BamRecordReader recordReader;
    private final BamRecordHandler handler;

    public QnameSearcher(KeyPointerReader keyPointerReader, BamRecordReader recordReader, BamRecordHandler handler) {
        this.keyPointerReader = keyPointerReader;
        this.recordReader = recordReader;
        this.handler = handler;
    }

    public int search(Path bamFile, Path indexFolder, Set<String> qnames) throws IOException {
        final List<Long> pointersForQname = getPointersForQname(indexFolder, qnames);
        for (Long pointer : pointersForQname) {
            recordReader.read(bamFile, pointer, handler);
        }
        return pointersForQname.size();
    }

    List<Long> getPointersForQname(Path indexFolder, Set<String> qnames) throws IOException {
        final Map<byte[], KeyPointer> qnameToPointer = getQnameToPointerMap(indexFolder, qnames);

        // Map from a pointer -> set of qnames in that level
        final Map<Long, Set<byte[]>> pointerToQname = qnameToPointer.entrySet().stream()
                .collect(toMap(e -> e.getValue().getPointer(), e -> Set.of(e.getKey()), this::concatSet));

        return pointerToQname.entrySet().stream()
                .flatMap(e -> getPointers(e.getValue(), indexFolder, e.getKey()).stream())
                .collect(toList());
    }

    /**
     * Returns a map from the qname -> Key pointer location in the index file.
     */
    private Map<byte[], KeyPointer> getQnameToPointerMap(Path indexFolder, Set<String> qnames) throws IOException {
        Path pathLevel1 = indexFolder.resolve(IndexVersion.LATEST.fileName("index"));
        try (InputStream inputStreamLevel1 = Channels.newInputStream(FileChannel.open(pathLevel1, READ))) {
            final List<KeyPointer> keyPointers = qnames.stream()
                    .map(Ascii7Coder.INSTANCE::encode)
                    .map(input -> new KeyPointer(0, input, input.length))
                    .collect(toList());

            final Map<byte[], KeyPointer> keyPointerMap = keyPointers.stream()
                    .collect(toMap(KeyPointer::getKey, Function.identity()));

            final Iterable<KeyPointer> indexPointers =  () -> keyPointerReader.read(inputStreamLevel1).iterator();
            for (KeyPointer indexPointer : indexPointers) {
                boolean updated = false;
                for (KeyPointer keyPointer : keyPointers) {
                    if (indexPointer.compareTo(keyPointer) < 0) {
                        keyPointerMap.put(keyPointer.getKey(), indexPointer);
                        updated = true;
                    }
                }
                if (!updated) {
                    return keyPointerMap;
                }
            }
            return keyPointerMap;
        }
    }

    private List<Long> getPointers(Set<byte[]> qnames, Path indexFolder, long keyPointer) {
        try {
            Path pathLevel0 = indexFolder.resolve(IndexVersion.LATEST.fileName("data"));
            FileChannel channelLevel0 = FileChannel.open(pathLevel0, READ);

            long compressedOffset = PointerPacker.INSTANCE.unpackCompressedOffset(keyPointer);
            int unCompressedOffset = PointerPacker.INSTANCE.unpackUnCompressedOffset(keyPointer);

            if (compressedOffset >= channelLevel0.size()) {
                return emptyList();
            }

            channelLevel0.position(compressedOffset);
            try (InputStream inputStream = Channels.newInputStream(channelLevel0)) {
                Iterable<KeyPointer> indexLevel0 = () -> keyPointerReader.read(inputStream, unCompressedOffset).iterator();

                Set<byte[]> remainingQnames = new TreeSet<>(Arrays::compareUnsigned);
                remainingQnames.addAll(qnames);

                List<Long> pointers = new ArrayList<>();
                for (KeyPointer indexPointer : indexLevel0) {
                    boolean keysLeft = false;
                    for (Iterator<byte[]> it = remainingQnames.iterator(); it.hasNext(); ) {
                        final int keyComparison = Arrays.compareUnsigned(indexPointer.getKey(), it.next());
                        if (keyComparison == 0) {
                            // Found a match
                            pointers.add(indexPointer.getPointer());
                            keysLeft = true;
                            // No other qnames would match because they are unique
                            break;
                        } else if (keyComparison < 0) {
                            keysLeft = true;
                            // No other qnames would match because they are all greater
                            break;
                        } else {
                            // We are done with the current key
                            it.remove();
                        }
                    }
                    if (!keysLeft) break;
                }
                return pointers;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> Set<T> concatSet(Set<T> s1, Set<T> s2) {
        return Stream.concat(s1.stream(), s2.stream())
                .collect(Collectors.toSet());
    }
}

