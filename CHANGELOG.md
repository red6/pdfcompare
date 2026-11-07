# Changelog

## 1.1.55

#### Added
- CHANGELOG.md added to the project
- Add "-x/--exclusions"-option to CLI so it's possible to pass an exclusions file on the CLI
- Add -excpwd and -actpwd option to CLI to allow setting a password for actual and expected PDFs
- "Exclusions" button opens a new area for defining exclusions. Exclusions can be created as a rectangle in the PDF display with the mouse. The coordinates can then be adjusted in the right area if necessary.
- The exclusions file can be loaded and saved. You can also load exclusions using drag & drop.
- With the Diffs button, the calculated differences are displayed as exclusions.
- The New button adds a new exclusions block for the current page.
- The rectangles in the PDF display can be clicked. A double click in the right area jumps to the rectangle in the PDF display. Dashed guidelines appear around the rectangles.
- You can now zoom using the mouse wheel, while CTRL is pressed.

## 1.1.56

#### Added
- Show return codes in CLI help text

## 1.1.57

#### Fixed
- Integrations Tests - expected documents addapted to new PdfBox version.

#### Security
- Upgrade PdfBox to version 2.0.22, which fixes a vulnerability

## 1.1.58

#### Added
- Add getPagesWithDifferences to CompareResult

## 1.1.59

#### Fixed
- Upgrade dependencies

## 1.1.60

#### Added
- Add getPageDiffsInPercent to CompareResult to hand out how different pages are

## 1.1.61

#### Upgrades
- Upgrade some dependencies versions

## 1.1.62

#### Upgrades
- Upgrade to use PDFBox 3.0.0 internally

## 1.1.63

#### Upgrades
- Upgrade logback.classic to 1.3.12

## 1.1.64

#### Upgrades
- Upgrade logback.classic to 1.3.14

## 1.1.65

#### Upgrades
- Upgrade PDFBox to 3.0.1

## 1.2.0

#### Added
- Add a config settings to configure the executorTimeoutInSeconds and changed the default from 900 seconds (15 Minutes) to 60 seconds. This is a small change,
that should not cause any compatibility issues.\
The actual timeout for the whole comparisons is overallTimeoutInMinutes + executorTimeoutInSeconds. These timeouts are a security measure, should a comparisons
run into a deadlock. The old defaults where 15 minutes overallTimeout + 15 minutes executorTimeout. This was reduced to 15 minutes + 1 minute. Comparisons that 
take between 16 minutes and 30 minutes succeeded before and would now fail. Then the overallTimeoutInMinutes should be increased.

## 1.2.1

#### Upgrades
- Upgrade slf4j to 2.0.12
- Upgrade logback to 1.5.3
- Upgrade typesafe.config to 1.4.3
- Upgrade commons-cli to 1.6.0

#### Fixed
- Close InputStreams properly after use

## 1.2.12

#### Security
- Exclusion files no longer follow HOCON includes. Any `include` (plain, `file(...)`, `url(...)` or `classpath(...)`) is rejected with a `ConfigException`, so an exclusion file can no longer make PdfCompare fetch a URL or read another file.
- Add the `maxPagePixels` setting (default 150000000, enough for A0 at 300 DPI). A page that would render to more pixels is rejected with a `RenderingException` before it is rendered. A page pair whose diff image would need more than twice the limit is rejected before the diff image is allocated. The limit caps each image, not the total heap use of a compare. At the default, one page pair can need about 1.8 GB while it is compared, or about 3 GB with `enableHorizontalCompareOutput`.

#### Fixed
- Temporary directories for PDFBox scratch files are removed when their document is closed. Before, every loaded document left a directory behind until the JVM shut down.
- The `tempDir` setting (`Environment.getTempDirectory()`) is now used for temporary files. It was documented but ignored. `FileUtils.setTempDirParent(...)` still takes precedence when set, and a `null` temp directory means `java.io.tmpdir`.
- A missing temporary directory now makes the compare fail with an `IOException`. Before, it was reported as a missing input file.

#### Upgrades
- Upgrade PDFBox to 3.0.8

## 1.2.14

#### Fixed
- `enableHorizontalCompareOutput` with `CompareResultWithMemoryOverflow` or `CompareResultWithPageOverflow` no longer drops the actual page of every pair that was swapped to disk.
- `enableHorizontalCompareOutput` respects exclusions. Differences inside an excluded area are faded like in the normal diff image instead of tinted.
- With `enableHorizontalCompareOutput`, a page that only one document has is written as the marked page next to a white page, so pairs stay aligned in a two-page view. Before, it was tinted over its whole area and paired with a solid page.
- When the expected document is missing, `CompareResultWithExpectedAndActual.getExpectedImage(...)` returns the blank page and `getActualImage(...)` the actual page. They were swapped before. The written result is unchanged in normal mode.
- `enableHorizontalCompareOutput` is about 5 times faster. Both pages are built in the same pass that finds the differences, and the unused diff image is no longer created.

#### Changed
- `ResultCollector.addPage(...)` takes `List<ImageWithDimension> resultPages` instead of a single diff image. Subclasses of `CompareResultImpl` that override `addPage` need the new signature.
- `CompareResultImpl.diffImages` is now a `Map<Integer, List<ImageWithDimension>>` with every result page of a compared page, and the protected field `diffImagesActualOverlay` is gone. The protected `addImagesToDocument(PDDocument, Map)` takes the new map type. Subclasses that read these fields or call that method need to change.
