package de.redsix.pdfcompare;

import de.redsix.pdfcompare.env.DefaultEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.*;
import java.awt.image.BufferedImage;

import static de.redsix.pdfcompare.DiffImage.MARKER_RGB;
import static de.redsix.pdfcompare.DiffImage.color;
import static de.redsix.pdfcompare.ImageTools.EXCLUDED_BACKGROUND_RGB;
import static de.redsix.pdfcompare.ImageTools.blankImage;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class DiffImageTest {

    @Mock
    private ResultCollector resultMock;
    @Captor
    private ArgumentCaptor<ImageWithDimension> captor;
    private final BufferedImage expected = blankImage(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB));
    private final BufferedImage actual = blankImage(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB));
    private final ImageWithDimension expectedImage = new ImageWithDimension(expected, 1.0f, 1.0f);
    private final ImageWithDimension actualImage = new ImageWithDimension(actual, 1.0f, 1.0f);
    private final Exclusions exclusions = new Exclusions(DefaultEnvironment.create()).add(new PageArea(35, 35, 37, 37));

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void equalImagesAreEqualAndEqualPixelsAreDimmed() {
        expected.setRGB(23, 23, Color.BLACK.getRGB());
        actual.setRGB(23, 23, Color.BLACK.getRGB());
        final BufferedImage resultImage = createAndAssertDiffImage(false, false);
        assertThat(resultImage.getRGB(23, 23), is(color(153, 153, 153)));
    }

    @Test
    public void unexpectedPixelsAreColoredRed() {
        actual.setRGB(23, 23, Color.BLACK.getRGB());
        final BufferedImage resultImage = createAndAssertDiffImage(true, false);
        assertThat(resultImage.getRGB(23, 23), is(color(210, 0, 0)));

        assertMarker(resultImage, 23, 23);
    }

    @Test
    public void expectedPixelsNotPresentAreColoredGreen() {
        expected.setRGB(23, 23, Color.BLACK.getRGB());
        final BufferedImage resultImage = createAndAssertDiffImage(true, false);
        assertThat(resultImage.getRGB(23, 23), is(color(0, 180, 0)));

        assertMarker(resultImage, 23, 23);
    }

    @Test
    public void exclusionsAreColoredYellowAndDimmed() {
        actual.setRGB(36, 36, Color.BLACK.getRGB());
        final BufferedImage resultImage = createAndAssertDiffImage(false, true);

        assertThat(resultImage.getRGB(35, 35), is(EXCLUDED_BACKGROUND_RGB));
        assertThat(resultImage.getRGB(36, 35), is(EXCLUDED_BACKGROUND_RGB));
        assertThat(resultImage.getRGB(37, 35), is(EXCLUDED_BACKGROUND_RGB));

        assertThat(resultImage.getRGB(35, 36), is(EXCLUDED_BACKGROUND_RGB));
        assertThat(resultImage.getRGB(36, 36), is(color(237, 153, 153)));
        assertThat(resultImage.getRGB(37, 36), is(EXCLUDED_BACKGROUND_RGB));

        assertThat(resultImage.getRGB(35, 37), is(EXCLUDED_BACKGROUND_RGB));
        assertThat(resultImage.getRGB(36, 37), is(EXCLUDED_BACKGROUND_RGB));
        assertThat(resultImage.getRGB(37, 37), is(EXCLUDED_BACKGROUND_RGB));

        // No marker for differences in exclusion
        for (int i = 0; i < 20; i++) {
            assertThat(resultImage.getRGB(23, i), is(Color.WHITE.getRGB()));
            assertThat(resultImage.getRGB(i, 23), is(Color.WHITE.getRGB()));
        }
    }

    private BufferedImage createAndAssertDiffImage(final boolean hasDifferences, final boolean hasDifferencesInExclusion) {
        final DiffImage diffImage = new DiffImage(
                expectedImage,
                actualImage,
                1, DefaultEnvironment.create(), exclusions, resultMock);
        diffImage.diffImages();
        PageDiffCalculator pdc = new PageDiffCalculator(0, 0);
        if (hasDifferences) pdc.diffFound();
        if (hasDifferencesInExclusion) pdc.diffFoundInExclusion();
        verify(resultMock).addPage(eq(pdc), eq(1), eq(expectedImage), eq(actualImage), captor.capture());
        return captor.getValue().bufferedImage;
    }

    private void assertMarker(final BufferedImage resultImage, final int x, final int y) {
        for (int i = 0; i < 20; i++) {
            assertThat(resultImage.getRGB(x, i), is(MARKER_RGB));
            assertThat(resultImage.getRGB(i, y), is(MARKER_RGB));
        }
    }
}