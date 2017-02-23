# PdfCompare  [![Build Status](https://travis-ci.org/red6/pdfcompare.svg?branch=master)](https://travis-ci.org/red6/pdfcompare)
A simple Java library to compare two PDF files.
Files are rendered and compared pixel by pixel.

### Usage with Maven

Just include it as dependency. Please check for the most current version available:

```xml
<dependencies>
  <dependency>
    <groupId>de.redsix</groupId>
    <artifactId>pdfcompare</artifactId>
    <version>1.1.4</version>
  </dependency>
</dependencies>
```

### Usage
```java
new PdfComparator().compare("expected.pdf", "actual.pdf").writeTo("diffOutput.pdf");
```
This will produce an output PDF, when the two files differ or nothing, when they are equal.
Pixels that differ are marked in red or green. And there are markings at the edge of the paper to find areas that differ quickly.

The compare-method returns a CompareResult, which can be queried:

```java
final CompareResult result = new PdfComparator().compare("expected.pdf", "actual.pdf");
if (result.isNotEqual()) {
    System.out.println("Differences found!");
}
if (result.isEqual()) {
    // do nothing
}
```
For convenience, writeTo also returns the equals status:
```java
boolean isEquals = new PdfComparator().compare("expected.pdf", "actual.pdf").writeTo("diffOutput.pdf");
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
        page: 1 // page is optional. When not given applies, the exclusion applies to all pages.
        x1: 130
        y1: 3000
        x2: 190
        y2: 3500
    },
    {
        page: 2
        x1: 300
        y1: 1000
        x2: 550
        y2: 1300
    }
]
```