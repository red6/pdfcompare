# PdfCompare  [![Build Status](https://travis-ci.org/red6/pdfcompare.svg?branch=master)](https://travis-ci.org/red6/pdfcompare) [![Maven Central Version](https://img.shields.io/maven-central/v/de.redsix/pdfcompare.svg)](http://search.maven.org/#search|gav|1|g:"de.redsix"%20AND%20a:"pdfcompare")
A simple Java library to compare two PDF files.
Files are rendered and compared pixel by pixel.

### Usage with Maven

Just include it as dependency. Please check for the most current version available:

```xml
<dependencies>
  <dependency>
    <groupId>de.redsix</groupId>
    <artifactId>pdfcompare</artifactId>
    <version>...</version> <!-- see current version in the maven central tag above -->
  </dependency>
</dependencies>
```

### Simple Usage

There is a simple interactive UI, when you start the Class de.redsix.pdfcompare.ui.DisplayMain.
But the focus of PdfCompare is on embedded usage as a library.

```java
new PdfComparator("expected.pdf", "actual.pdf").compare().writeTo("diffOutput.pdf");
```
This will produce an output PDF which may include markings for differences found.
Pixels that are equal are faded a bit. Pixels that differ are marked in red and green.
Red for pixels that where expected, but didn't come.
Green for pixels that are there, but where not expected.
And there are markings at the edge of the paper in magenta to find areas that differ quickly.
Ignored Areas are marked with a yellow background.
Pages that where expected, but did not come are marked with a red border.
Pages that appear, but where not expected are marked with a green border.

The compare-method returns a CompareResult, which can be queried:

```java
final CompareResult result = new PdfComparator("expected.pdf", "actual.pdf").compare();
if (result.isNotEqual()) {
    System.out.println("Differences found!");
}
if (result.isEqual()) {
    System.out.println("No Differences found!");
}
if (result.hasDifferenceInExclusion()) {
    System.out.println("Only Differences in excluded areas found!");
}
```
For convenience, writeTo also returns the equals status:
```java
boolean isEquals = new PdfComparator("expected.pdf", "actual.pdf").compare().writeTo("diffOutput.pdf");
if (!isEquals) {
    System.out.println("Differences found!");
}
```
The compare method can be called with filenames as Strings, Files, Paths or InputStreams.

It is also possible to define rectangular areas that are ignored during comparison. For that, a file needs to be created, which defines areas to ignore.
The file format is JSON (or actually a superset called [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md)) and has the following form:
```javascript
exclusions: [
    {
        page: 2
        x1: 300 // entries without a unit are in pixels, when Pdf is rendered at 300DPI
        y1: 1000
        x2: 550
        y2: 1300
    },
    {
        // page is optional. When not given, the exclusion applies to all pages.
        x1: 130.5mm // entries can also be given in units of cm, mm or pt (DTP-Point definied as 1/72 Inches)
        y1: 3.3cm
        x2: 190mm
        y2: 3.7cm
    },
    {
        page: 7
        // coordinates are optional. When not given, the whole page is excluded.
    }
]
```

Exclusions are provided in the code as follows:

```java
new PdfComparator("expected.pdf", "actual.pdf").withIgnore("ignore.conf").compare();
```

### Different CompareResult Implementations

There are a few different Implementations of CompareResults with different characteristics.
The can be used to control certain aspects of the system behaviour, in particular memory consumption.

#### Internals about memory consumption

It is good to know a few internals, when using the PdfCompare.
Here is in a nutshell, what PdfCompare does, when it compares two PDFs.

PdfCompare uses the Apache PdfBox Library to read and write Pdfs.

- The Two Pdfs to compare are opened with PdfBox.
- A page from each Pdf is read and rendered into a BufferedImage at 300dpi.
- A new empty BufferedImage is created to take the result of the comparion. It has the maximum size of the expected and the actual image.
- When the comparison is finished, the new BufferedImage, which holds the result of the comparison, is kept in memory in a CompareResult object. Holding on to the CompareResult means, that the images are also kept in memory. If memory consumption is a problem, a DiskUsingCompareResult can be used. This class does not store images in memory, but stores them in a temporary folder on disk.
- After all pages are compared, a new Pdf is created and the images are written page by page into the new Pdf.

So comparing large Pdfs can use up a lot of memory.
I didn't yet find a way to write the difference Pdf page by page incrementally with PdfBox, but there are some workarounds.

#### CompareResults with Overflow

There are currently two different CompareResults, that have different strategies for swapping pages to disk and thereby limiting memory consumption.
- CompareResultWithPageOverflow - stores a bunch of pages into a partial Pdf and merges the resulting Pdfs in the end. The default is to swap every 10 pages, which is a good balance between memory usage and performance.
- CompareResultWithMemoryOverflow - tries to keep as many images in memory as possible and swaps, when a critical amount of memory is consumed by the JVM. As a default, pages are swapped, when 70% of the maximum available heap is filled.

A different CompareResult implementation can be used as follows:

```java
new PdfComparator("expected.pdf", "actual.pdf", new CompareResultWithPageOverflow()).compare();
```

Also there are some internal settings for memory limits, that can be changed.
Just add a file called "application.conf" to the root of the classpath. This file can have some or all of the following settings to overwrite the defaults given here:

- imageCacheSizeCount=30

    How many images are cached by PdfBox
- maxImageSizeInCache=100000 

    A rough maximum size of images that are cached, to prevent very big images from being cached
- mergeCacheSizeMB=100

    When Pdfs are partially written and later merged, this is the memory cache that is configured for the PdfBox instance that does the merge.
- swapCacheSizeMB=100
    
    When Pdfs are partially written, this is the memory cache that is configured for the PdfBox instance that does the partial writes. 
- documentCacheSizeMB=200

    This is the cache size configured for the PdfBox instance, that loads the documents that are compared.
- parallelProcessing=false

    Disable alle parallel processing and process everything in a single thread.
- overallTimeoutInMinutes=15

    Set the overall timeout. This is a safety measure to detect possible deadlocks. Complex comparisons might take longer, so this value might have to be increased.

So in this default configuration, PdfBox should use up to 400MB of Ram for it's caches, before swapping to disk.
I have good experience with granting a 2GB heap space to the JVM.

### Acknowledgements

Big thanks to Chethan Rao <meetchethan@gmail.com> for helping me diagnose out of memory problems and providing
the idea of partial writes and merging of the generated PDFs.
