package de.redsix.pdfcompare;

import static de.redsix.pdfcompare.ImageTools.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import java.awt.*;

public class ImageToolsTest {

    @Test
    public void fadeElementMakesDarkerPixelsLighter() {
        int actual = fadeElement(new Color(0, 0, 0).getRGB());
        assertThat(actual, is(new Color(153, 153, 153).getRGB()));
        actual = fadeElement(new Color(255, 255, 255).getRGB());
        assertThat(actual, is(new Color(255, 255, 255).getRGB()));
        actual = fadeElement(new Color(0, 0, 255).getRGB());
        assertThat(actual, is(new Color(153, 153, 255).getRGB()));
        actual = fadeElement(new Color(180, 60, 130).getRGB());
        assertThat(actual, is(new Color(225, 177, 205).getRGB()));
    }

    @Test
    public void fadeExclusionOfDarkPixlesIsARegularFade() {
        int actual = fadeExclusion(new Color(0, 0, 0).getRGB());
        assertThat(actual, is(new Color(153, 153, 153).getRGB()));
    }

    @Test
    public void fadeExclusionOfLightPixlesMakesItYellow() {
        int actual = fadeExclusion(new Color(250, 250, 250).getRGB());
        assertThat(actual, is(EXCLUDED_BACKGROUND_RGB));
    }
}