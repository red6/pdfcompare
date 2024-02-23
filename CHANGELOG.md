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
