# QNAME search

Tool for:

* Indexing records in large BAM files by their QNAME (run once)
* Efficiently retrieving records by their QNAME (use index many times)

## Index file format

There are two index files, `qname.0` and `qname.1`.
Both are in BGZF format as described in [SAMv1.pdf].
The data is in the following format (numbers are in little endian):

```
xx xx   2 bytes: length of entry (N)
xx...   N-8 bytes: QNAME (key)
xx xx xx xx xx xx xx xx: 8 bytes: virtual offset (pointer)
```

The pointer is encoded as `coffset | uoffset << 48`.

### `qname.0`

This file contains all the QNAME to virtual offset mappings, sorted by QNAME.
The offset is the position of the corresponding record in the BAM file.

### `qname.1`

This file contains a subset of QNAMEs. The pointer is an offset into
`qname.0` for where the first record with that QNAME is stored. Because
the file is sorted, that means records starting from that position have
a QNAME that is equal or greater.

## Search

Given the above index files, a search for `input` is performed like this:

1. Iterate through `qname.1` to find the last record where `QNAME <= input`.
2. Starting from the offset from 1, iterate through `qname.0` to find `QNAME == input` records.
   Stop when we hit `QNAME > input` (we won't find more records).
3. Using the offsets from 2, look up the records in the BAM file.


[SAMv1.pdf]: http://samtools.github.io/hts-specs/SAMv1.pdf
