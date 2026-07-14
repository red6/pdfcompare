package de.redsix.pdfcompare;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.redsix.pdfcompare.env.SimpleEnvironment;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Map;

class CompareResultImplTest {

    @Test
    public void addEqualPagesAreStored() {
        CompareResultImpl compareResult = new CompareResultImpl();
        compareResult.setEnvironment(new SimpleEnvironment());
        ImageWithDimension image = new ImageWithDimension(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY), 0.0f, 0.0f);
        compareResult.addPage(new PageDiffCalculator(0, 0), 1, image, image, image);
        assertThat(compareResult.hasImages(), is(true));
        compareResult.addPage(new PageDiffCalculator(new PageArea(2)), 2, image, image, image);
        assertThat(compareResult.hasImages(), is(true));
        assertThat(compareResult.getNumberOfPages(), is(2));
        assertThat(compareResult.diffImages.size(), is(2));
    }

    @Test
    public void addEqualPagesAreNotStored() {
        CompareResultImpl compareResult = new CompareResultImpl();
        compareResult.setEnvironment(new SimpleEnvironment().setAddEqualPagesToResult(false));
        ImageWithDimension image = new ImageWithDimension(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY), 0.0f, 0.0f);
        compareResult.addPage(new PageDiffCalculator(0, 0), 1, image, image, image);
        assertThat(compareResult.hasImages(), is(false));
        compareResult.addPage(new PageDiffCalculator(new PageArea(2)), 2, image, image, image);
        assertThat(compareResult.hasImages(), is(true));
        assertThat(compareResult.getNumberOfPages(), is(1));
        assertThat(compareResult.diffImages.size(), is(1));
    }

    @Test
    public void withHorizontalCompareOutputDifferingPagesAreStoredAsMergedImage() {
        CompareResultImpl compareResult = new CompareResultImpl();
        compareResult.setEnvironment(new SimpleEnvironment().setEnableHorizontalCompareOutput(true));
        ImageWithDimension expectedImage = new ImageWithDimension(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), 10.0f, 10.0f);
        ImageWithDimension actualImage   = new ImageWithDimension(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), 10.0f, 10.0f);
        ImageWithDimension diffImage     = new ImageWithDimension(new BufferedImage(6,  10, BufferedImage.TYPE_INT_RGB), 6.0f,  10.0f);

        compareResult.addPage(new PageDiffCalculator(new PageArea(1)), 1, expectedImage, actualImage, diffImage);

        assertThat(compareResult.isNotEqual(), is(true));
        assertThat(compareResult.hasImages(), is(true));
        assertThat(compareResult.getNumberOfPages(), is(1));
        // merged image width must equal expectedWidth(10) + diffWidth(6)
        assertThat(compareResult.diffImages.get(1).width, is(16.0f));
    }

    @Test
    public void withHorizontalCompareOutputEqualPagesAreStoredAsMergedImage() {
        CompareResultImpl compareResult = new CompareResultImpl();
        // addEqualPagesToResult defaults to true via reference.conf
        compareResult.setEnvironment(new SimpleEnvironment().setEnableHorizontalCompareOutput(true));
        ImageWithDimension expectedImage = new ImageWithDimension(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), 10.0f, 10.0f);
        ImageWithDimension actualImage   = new ImageWithDimension(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), 10.0f, 10.0f);
        ImageWithDimension diffImage     = new ImageWithDimension(new BufferedImage(6,  10, BufferedImage.TYPE_INT_RGB), 6.0f,  10.0f);

        compareResult.addPage(new PageDiffCalculator(0, 0), 1, expectedImage, actualImage, diffImage);

        assertThat(compareResult.isEqual(), is(true));
        assertThat(compareResult.hasImages(), is(true));
        assertThat(compareResult.getNumberOfPages(), is(1));
        // merged image width must equal expectedWidth(10) + diffWidth(6)
        assertThat(compareResult.diffImages.get(1).width, is(16.0f));
    }

    @Test
    public void withHorizontalCompareOutputEqualPagesAreNotStoredWhenAddEqualPagesToResultIsDisabled() {
        CompareResultImpl compareResult = new CompareResultImpl();
        compareResult.setEnvironment(new SimpleEnvironment()
                .setEnableHorizontalCompareOutput(true)
                .setAddEqualPagesToResult(false));
        ImageWithDimension image = new ImageWithDimension(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY), 0.0f, 0.0f);

        compareResult.addPage(new PageDiffCalculator(0, 0), 1, image, image, image);

        assertThat(compareResult.isEqual(), is(true));
        assertThat(compareResult.hasImages(), is(false));
        assertThat(compareResult.getNumberOfPages(), is(0));
    }

    @Test
    public void mapsDiffPercentagesCorrectly() {
        CompareResultImpl compareResult = new CompareResultImpl();
        compareResult.setEnvironment(new SimpleEnvironment());
        ImageWithDimension image = new ImageWithDimension(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY), 0.0f, 0.0f);

        PageDiffCalculator pageWithDiff = new PageDiffCalculator(5, 0);
        pageWithDiff.diffFound();
        PageDiffCalculator pageWithoutDiff = new PageDiffCalculator(10, 0);

        compareResult.addPage(pageWithDiff, 1, image, image, image);
        compareResult.addPage(pageWithoutDiff, 2, image, image, image);

        Map<Integer, Double> result = compareResult.getPageDiffsInPercent();
        assertThat(result, hasEntry(1, 20.0));
        assertThat(result, hasEntry(2, 0.0));
    }
}