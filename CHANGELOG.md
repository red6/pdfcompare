# Changelog

## 1.1.55

#### Added
- CHANGELOG.md added to the project
- Add "-x/--exclusions"-option to CLI so it's possible to pass an exclusions file on the CLI
- Add -excpwd and -actpwd parameters to CLI to allow setting a password for actual and expected PDFs
- "Exclusions" button opens a new area for defining exclusions. Exclusions can be created as a rectangle in the PDF display with the mouse. The coordinates can then be adjusted in the right area if necessary.
- The exclusions file can be loaded and saved. In the current state, only the exclusions are saved, i.e. other options would be lost. You can also load using drag & drop.
- With the Diffs button, the calculated differences are displayed as exclusions.
- The New button adds a new exclusions block for the current page.
- The rectangles in the PDF display can be clicked. A double click in the right area jumps to the rectangle in the PDF display. Dashed guidelines appear around the rectangles.
- You can now zoom in using the mouse wheel.
