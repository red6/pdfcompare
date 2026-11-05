package de.redsix.pdfcompare;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.typesafe.config.ConfigFactory;
import de.redsix.pdfcompare.env.ConfigFileEnvironment;
import de.redsix.pdfcompare.env.DefaultEnvironment;
import de.redsix.pdfcompare.env.Environment;
import de.redsix.pdfcompare.env.SimpleEnvironment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.RenderDestination;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class PagePixelLimitTest {

    // 3000pt at 300 DPI is 12500 x 12500 = 156,250,000 pixels, just above the default limit.
    private static final PDRectangle HUGE_PAGE = new PDRectangle(3000, 3000);

    @Test
    public void pageAboveDefaultLimitIsRejected() {
        final RenderingException e = assertThrows(RenderingException.class, () -> render(new PDPage(HUGE_PAGE), DefaultEnvironment.create()));
        assertThat(e.getMessage(), containsString("156250000"));
        assertThat(e.getMessage(), containsString("150000000"));
    }

    @Test
    public void a4PageIsRendered() {
        assertDoesNotThrow(() -> render(new PDPage(PDRectangle.A4), DefaultEnvironment.create()));
    }

    @Test
    public void raisedLimitLetsHugePageThrough() {
        assertDoesNotThrow(() -> render(new PDPage(HUGE_PAGE), new SimpleEnvironment().setMaxPagePixels(160_000_000L)));
    }

    @Test
    public void limitIsExactAgainstTheImagePdfBoxAllocates() throws IOException {
        final long renderedPixels;
        try (PDDocument document = new PDDocument()) {
            document.addPage(croppedPage());
            final BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 300);
            renderedPixels = (long) image.getWidth() * image.getHeight();
        }
        final SimpleEnvironment environment = new SimpleEnvironment().setDPI(300);

        assertDoesNotThrow(() -> render(croppedPage(), environment.setMaxPagePixels(renderedPixels)));
        assertThrows(RenderingException.class, () -> render(croppedPage(), environment.setMaxPagePixels(renderedPixels - 1)));
    }

    @Test
    public void configWithoutTheSettingFallsBackToTheBundledDefault() {
        assertThat(new ConfigFileEnvironment(ConfigFactory.empty()).getMaxPagePixels(), is(DefaultEnvironment.create().getMaxPagePixels()));
    }

    // The crop box is smaller than the media box and scales to 1250.83 x 1667.92 pixels at 300 DPI,
    // so using the media box or rounding up would both change the pixel count.
    private static PDPage croppedPage() {
        final PDPage page = new PDPage(PDRectangle.A4);
        page.setCropBox(new PDRectangle(50, 50, 300.2f, 400.3f));
        return page;
    }

    private static void render(final PDPage page, final Environment environment) throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(page);
            PdfComparator.renderPageAsImage(document, new TinyImageRenderer(document), 0, environment);
        }
    }

    /** Stands in for real rendering, so a huge page never allocates its full image. */
    private static class TinyImageRenderer extends PDFRenderer {

        TinyImageRenderer(final PDDocument document) {
            super(document);
        }

        @Override
        public BufferedImage renderImage(final int pageIndex, final float scale, final ImageType imageType, final RenderDestination destination) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        }
    }
}
