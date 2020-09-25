# QNAME search

Tool for:

* Indexing records in large BAM files by their QNAME (run once)
* Efficiently retrieving records by their QNAME (use index many times)

When there are multiple records having the same QNAME, such as for read pairs and supplementary alignments, this tool retrieves all records having the requested QNAME.

## Usage

The tool can be accessed as JAR file (`java -jar atlantool.jar`) or as a 
native Linux executable (`atlantool-linux-x64`). 
Check out [releases section] to get the latest build.

The command line tool provides two sub commands: `index` and `view` for the operations
mentioned above. The basic usage format is:
```
$ atlantool-linux-x64 index <bam-path>

$ atlantool-linux-x64 view <bam-path> -n <qname-to-search>
or
$ atlantool-linux-x64 view <bam-path> -f <file-containing-qnames>
```

There are advanced options available for each sub command, detailed help
can be seen by executing the sub command.

### Detailed example
#### Build the index

The following command indexes `1G.bam` file and places index files near BAM file. Please note, that the process takes time on large BAM files.
```shell script
$ atlantool-linux-x64 index 1G.bam --thread-count=8
```

#### Search by QNAME

After the index has been built successfully, search requests can be executed on a QNAME string.
```shell script
$ atlantool-linux-x64 view 1G.bam -n SOLEXA-1GA-1_0047_FC62472:5:52:15203:7914#0
SOLEXA-1GA-1_0047_FC62472:5:52:15203:7914#0	0	chr1	10158	25	36M	*	0	0	AACCCTAACCCTAACCCTAACCTAACCCTAACCCTA	ED?EEGDG?EEGGG4B@ABB@BD:49+=:=@;=;;D	X0:i:1	MD:Z:36	NM:i:0
```

The output follows SAM specification, and it should be recognised by `samtools`.
```shell script
$ atlantool-linux-x64 view 1G.bam -n SOLEXA-1GA-1_0047_FC62472:5:52:15203:7914#0 -h | samtools view
SOLEXA-1GA-1_0047_FC62472:5:52:15203:7914#0	0	chr1	10158	25	36M	*	0	0	AACCCTAACCCTAACCCTAACCTAACCCTAACCCTA	ED?EEGDG?EEGGG4B@ABB@BD:49+=:=@;=;;D	X0:i:1	MD:Z:36	NM:i:0
```

## Performance

Indexing time depends on the size and the number of records in BAM file. At the moment the indexing time for a 140 GB BAM file of 1.2 billion records using 8 threads is around 2 hours. It generates index files of 12 GB. Query time is sub second.

## Index file format

There are two index files, `qname.index.bgz` and `qname.data.bgz`.
Both are in BGZF format as described in [SAMv1.pdf].
The data is in the following format (numbers are in little endian):

```
xx xx   2 bytes: length of entry (N)
xx...   N-8 bytes: QNAME (key)
xx xx xx xx xx xx xx xx: 8 bytes: virtual offset (pointer)
```

The pointer is encoded as `(coffset << 16) | uoffset`.

### `qname.data.bgz`

This file contains all the QNAME to virtual offset mappings, sorted by QNAME.
The offset is the position of the corresponding record in the BAM file.

### `qname.index.bgz`

This file contains a subset of QNAMEs. The pointer is an offset into
`qname.data.bgz` for where the first record with that QNAME is stored. Because
the file is sorted, that means records starting from that position have
a QNAME that is equal or greater.

## Search

Given the above index files, a search for `input` is performed like this:

1. Iterate through `qname.index.bgz` to find the last record where `QNAME <= input`.
2. Starting from the offset from 1, iterate through `qname.data.bgz` to find `QNAME == input` records.
   Stop when we hit `QNAME > input` (we won't find more records).
3. Using the offsets from 2, look up the records in the BAM file.


[SAMv1.pdf]: http://samtools.github.io/hts-specs/SAMv1.pdf
[releases section]: https://github.com/VCCRI/atlantool/releases
