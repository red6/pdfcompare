package de.redsix.pdfcompare;

import static de.redsix.pdfcompare.DiffImage.MARKER_RGB;
import static de.redsix.pdfcompare.DiffImage.color;
import static de.redsix.pdfcompare.ImageTools.EXCLUDED_BACKGROUND_RGB;
import static de.redsix.pdfcompare.ImageTools.blankImage;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import de.redsix.pdfcompare.env.DefaultEnvironment;
import de.redsix.pdfcompare.env.Environment;
import de.redsix.pdfcompare.env.SimpleEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.*;
import java.awt.image.BufferedImage;

public class DiffImageTest {

    @Mock
    private ResultCollector resultMock;
    @Captor
    private ArgumentCaptor<ImageWithDimension> captor;
    @Captor
    private ArgumentCaptor<PageDiffCalculator> pageDiffCalculatorCaptor;
    private final BufferedImage expected = blankImage(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB));
    private final BufferedImage actual = blankImage(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB));
    private final ImageWithDimension expectedImage = new ImageWithDimension(expected, 1.0f, 1.0f);
    private final ImageWithDimension actualImage = new ImageWithDimension(actual, 1.0f, 1.0f);
    private final Exclusions exclusions = new Exclusions(DefaultEnvironment.create()).add(new PageArea(35, 35, 37, 37));
    private PageDiffCalculator pageDiffCalculator;
    private BufferedImage resultImage;

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void equalImagesAreEqualAndEqualPixelsAreDimmed() {
        expected.setRGB(23, 23, Color.BLACK.getRGB());
        actual.setRGB(23, 23, Color.BLACK.getRGB());
        final BufferedImage resultImage = createAndAssertDiffImage();
        assertThat(pageDiffCalculator.differencesFound(), is(false));
        assertThat(resultImage.getRGB(23, 23), is(color(153, 153, 153)));
    }

    @Test
    public void unexpectedPixelsAreColoredRed() {
        actual.setRGB(23, 23, Color.BLACK.getRGB());
        final BufferedImage resultImage = createAndAssertDiffImage();
        assertThat(pageDiffCalculator.differencesFound(), is(true));
        assertThat(resultImage.getRGB(23, 23), is(color(210, 0, 0)));
        assertMarker(resultImage, 23, 23);
    }

    @Test
    public void expectedPixelsNotPresentAreColoredGreen() {
        expected.setRGB(23, 23, Color.BLACK.getRGB());
        final BufferedImage resultImage = createAndAssertDiffImage();
        assertThat(pageDiffCalculator.differencesFound(), is(true));
        assertThat(resultImage.getRGB(23, 23), is(color(0, 180, 0)));

        assertMarker(resultImage, 23, 23);
    }

    @Test
    public void exclusionsAreColoredYellowAndDimmed() {
        actual.setRGB(36, 36, Color.BLACK.getRGB());
        final BufferedImage resultImage = createAndAssertDiffImage();
        assertThat(pageDiffCalculator.differencesFound(), is(false));
        assertThat(pageDiffCalculator.differencesFoundInExclusion(), is(true));
        assertThat(pageDiffCalculator.getDiffArea(), nullValue());

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

    @Test
    public void diffAreaCapturesAllDifferences() {
        actual.setRGB(26, 26, Color.BLACK.getRGB());
        actual.setRGB(31, 27, Color.BLACK.getRGB());
        createAndAssertDiffImage();
        assertThat(pageDiffCalculator.getDiffArea().getPage(), is(2));
        assertThat(pageDiffCalculator.getDiffArea().getX1(), is(26));
        assertThat(pageDiffCalculator.getDiffArea().getY1(), is(26));
        assertThat(pageDiffCalculator.getDiffArea().getX2(), is(31));
        assertThat(pageDiffCalculator.getDiffArea().getY2(), is(27));
    }

    @Test
    public void differencesBelowAllowedPercentGivesNoDifference() {
        actual.setRGB(26, 26, Color.BLACK.getRGB());
        actual.setRGB(31, 27, Color.BLACK.getRGB());
        createAndAssertDiffImage(new SimpleEnvironment().setAllowedDiffInPercent(10));
        assertThat(pageDiffCalculator.differencesFound(), is(false));
        assertThat(pageDiffCalculator.getDiffArea(), nullValue());
        assertMarker(resultImage, 26, 26);
        assertMarker(resultImage, 31, 27);
    }

    private BufferedImage createAndAssertDiffImage() {
        return createAndAssertDiffImage(DefaultEnvironment.create());
    }

    private BufferedImage createAndAssertDiffImage(Environment env) {
        final DiffImage diffImage = new DiffImage(
                expectedImage,
                actualImage,
                1, env, exclusions, resultMock);
        diffImage.diffImages();
        verify(resultMock).addPage(pageDiffCalculatorCaptor.capture(), eq(1), eq(expectedImage), eq(actualImage), captor.capture());
        pageDiffCalculator = pageDiffCalculatorCaptor.getValue();
        resultImage = captor.getValue().bufferedImage;
        return resultImage;
    }

    private void assertMarker(final BufferedImage resultImage, final int x, final int y) {
        for (int i = 0; i < 20; i++) {
            assertThat(resultImage.getRGB(x, i), is(MARKER_RGB));
            assertThat(resultImage.getRGB(i, y), is(MARKER_RGB));
        }
    }
}