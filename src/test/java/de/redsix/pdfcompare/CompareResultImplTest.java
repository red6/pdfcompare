package de.redsix.pdfcompare;

import de.redsix.pdfcompare.env.SimpleEnvironment;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class CompareResultImplTest {

    @Test
    public void addEqualPagesAreStored() {
        CompareResultImpl compareResult = new CompareResultImpl();
        compareResult.setEnvironment(new SimpleEnvironment());
        ImageWithDimension image = new ImageWithDimension(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY), 0.0f, 0.0f);
        compareResult.addPage(new PageDiffCalculator(false, false), 1, image, image, image);
        assertThat(compareResult.hasImages(), is(true));
        compareResult.addPage(new PageDiffCalculator(true, false), 2, image, image, image);
        assertThat(compareResult.hasImages(), is(true));
        assertThat(compareResult.getNumberOfPages(), is(2));
        assertThat(compareResult.diffImages.size(), is(2));
    }

    @Test
    public void addEqualPagesAreNotStored() {
        CompareResultImpl compareResult = new CompareResultImpl();
        compareResult.setEnvironment(new SimpleEnvironment().setAddEqualPagesToResult(false));
        ImageWithDimension image = new ImageWithDimension(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY), 0.0f, 0.0f);
        compareResult.addPage(new PageDiffCalculator(false, false), 1, image, image, image);
        assertThat(compareResult.hasImages(), is(false));
        compareResult.addPage(new PageDiffCalculator(true, false), 2, image, image, image);
        assertThat(compareResult.hasImages(), is(true));
        assertThat(compareResult.getNumberOfPages(), is(1));
        assertThat(compareResult.diffImages.size(), is(1));
    }
}