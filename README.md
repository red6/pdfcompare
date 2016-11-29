# PdfCompare
A simple Java library to compare two PDF files.
Files are rendered and compared pixel by pixel.

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
