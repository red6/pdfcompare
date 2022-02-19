package de.redsix.pdfcompare;

import static de.redsix.pdfcompare.PdfComparator.MARKER_WIDTH;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.redsix.pdfcompare.env.Environment;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.sun.org.apache.xalan.internal.xsltc.compiler.sym;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;


public class DiffImage {

    private static final Logger LOG = LoggerFactory.getLogger(DiffImage.class);
    /*package*/ static final int MARKER_RGB = color(230, 0, 230);
    private final ImageWithDimension expectedImage;
    private final ImageWithDimension actualImage;
    private final int page;
    private final Environment environment;
    private final Exclusions exclusions;
    private DataBuffer expectedBuffer;
    private DataBuffer actualBuffer;
    private int expectedImageWidth;
    private int expectedImageHeight;
    private int actualImageWidth;
    private int actualImageHeight;
    private BufferedImage resultImage;
    private int diffAreaX1, diffAreaY1, diffAreaX2, diffAreaY2;
    private final ResultCollector compareResult;
    private PageDiffCalculator diffCalculator;

    public DiffImage(final ImageWithDimension expectedImage, final ImageWithDimension actualImage, final int page,
            final Environment environment, final Exclusions exclusions, final ResultCollector compareResult) {
        this.expectedImage = expectedImage;
        this.actualImage = actualImage;
        this.page = page;
        this.environment = environment;
        this.exclusions = exclusions;
        this.compareResult = compareResult;
    }

    public BufferedImage getImage() {
        return resultImage;
    }

    public int getPage() {
        return page;
    }

    public void diffImages() {
    	
    	System.out.println("TEST JVM 1");
    	
        BufferedImage expectBuffImage = this.expectedImage.bufferedImage;
        BufferedImage actualBuffImage = this.actualImage.bufferedImage;
        expectedBuffer = expectBuffImage.getRaster().getDataBuffer();
        actualBuffer = actualBuffImage.getRaster().getDataBuffer();

        expectedImageWidth = expectBuffImage.getWidth();
        expectedImageHeight = expectBuffImage.getHeight();
        actualImageWidth = actualBuffImage.getWidth();
        actualImageHeight = actualBuffImage.getHeight();

        int resultImageWidth = Math.max(expectedImageWidth, actualImageWidth);
        int resultImageHeight = Math.max(expectedImageHeight, actualImageHeight);
        resultImage = new BufferedImage(resultImageWidth, resultImageHeight, actualBuffImage.getType());
        DataBuffer resultBuffer = resultImage.getRaster().getDataBuffer();

        diffCalculator = new PageDiffCalculator(resultImageWidth * resultImageHeight, environment.getAllowedDiffInPercent());

        int expectedElement;
        int actualElement;
        final PageExclusions pageExclusions = exclusions.forPage(page + 1);

        for (int y = 0; y < resultImageHeight; y++) {
            final int expectedLineOffset = y * expectedImageWidth;
            final int actualLineOffset = y * actualImageWidth;
            final int resultLineOffset = y * resultImageWidth;
            for (int x = 0; x < resultImageWidth; x++) {
                expectedElement = getExpectedElement(x, y, expectedLineOffset);               
                actualElement = getActualElement(x, y, actualLineOffset);
                //this.salvaSuFileImmagine(actualBuffer, expectedBuffer);
                int element = getElement(expectedElement, actualElement);
                if (pageExclusions.contains(x, y)) {
                    element = ImageTools.fadeExclusion(element);
                    if (expectedElement != actualElement) {
                        diffCalculator.diffFoundInExclusion();
                    }
                } else {
                    if (expectedElement != actualElement) {
                        extendDiffArea(x, y);
                        diffCalculator.diffFound();

                        LOG.trace("Difference found on page: {} at x: {}, y: {}", page + 1, x, y);
                        mark(resultBuffer, x, y, resultImageWidth);

                    }
                }
                resultBuffer.setElem(x + resultLineOffset, element);
            }
        }
        if (diffCalculator.differencesFound()) {
            diffCalculator.addDiffArea(new PageArea(page + 1, diffAreaX1, diffAreaY1, diffAreaX2, diffAreaY2));
            LOG.debug("Differences found at { page: {}, x1: {}, y1: {}, x2: {}, y2: {} }", page + 1, diffAreaX1, diffAreaY1, diffAreaX2,
                    diffAreaY2);
        }
        final float maxWidth = Math.max(expectedImage.width, actualImage.width);
        final float maxHeight = Math.max(expectedImage.height, actualImage.height);
        compareResult.addPage(diffCalculator, page, expectedImage, actualImage, new ImageWithDimension(resultImage, maxWidth, maxHeight));
    }

    private void salvaSuFileImmagine(DataBuffer actualBuffer2, DataBuffer expectedBuffer2) {
		System.out.println("********************* DATA BUFFER ACTUAL ******************************");
		String actualBuffer="";
		System.out.println("Inizio a leggere actualbuffer...");
		int value=0;
		for(int i=0;i<actualBuffer2.getSize();i++) {
			value=actualBuffer2.getElem(i);

			actualBuffer=actualBuffer.concat(String.valueOf(value));
			if(i%100==0) {
				//System.out.println(i+""+value);
			}
		}
		this.writeTextFile("", "ACTUAL.txt",actualBuffer);
		System.out.println("Ho creato il file:"+new File("ACTUAL.txt").getAbsolutePath());
		
		System.out.println("********************* DATA BUFFER EXPECTED ******************************");
		String expectedBuffer="";
		System.out.println("Inizio a leggere actualbuffer...");
		value=0;
		for(int i=0;i<expectedBuffer2.getSize();i++) {
			value=expectedBuffer2.getElem(i);
			expectedBuffer=expectedBuffer.concat(String.valueOf(value));
			if(i%100==0) {
				//System.out.println(i+""+value);	
			}
		}
		this.writeTextFile("", "EXPECTED.txt",expectedBuffer);
		System.out.println("Ho creato il file:"+new File("EXPECTED.txt").getAbsolutePath());
	}
    
    
	public String writeTextFile(String pathOutput, String fileNameOutput, String text) {
		BufferedWriter writer     = null;
		File           fileOutput = null;
		try {
			File dir = new File(pathOutput);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			fileOutput = new File(dir + File.separator + fileNameOutput);
			writer     = new BufferedWriter(new FileWriter(fileOutput.getAbsoluteFile()));
			writer.write(text);
			System.out.println("Created file: " + fileOutput.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("STACKTRACE"+e.getMessage());
			return ("ERROR_WRITE_FILE");
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("STACKTRACE"+e.getMessage());				
			}
		}
		return ("DONE");
	}


	private void extendDiffArea(final int x, final int y) {
        if (!diffCalculator.differencesFound()) {
            diffAreaX1 = x;
            diffAreaY1 = y;
        }
        diffAreaX1 = Math.min(diffAreaX1, x);
        diffAreaX2 = Math.max(diffAreaX2, x);
        diffAreaY1 = Math.min(diffAreaY1, y);
        diffAreaY2 = Math.max(diffAreaY2, y);
    }

    private int getElement(final int expectedElement, final int actualElement) {
        if (expectedElement != actualElement) {
            int expectedIntensity = calcCombinedIntensity(expectedElement);
            int actualIntensity = calcCombinedIntensity(actualElement);
            if (expectedIntensity > actualIntensity) {
                Color color = environment.getActualColor();
                return color(levelIntensity(expectedIntensity, color.getRed()), color.getGreen(), color.getBlue());
            } else {
                Color color = environment.getExpectedColor();
                return color(color.getRed(), levelIntensity(actualIntensity, color.getGreen()), color.getBlue());
            }
        } else {
            return ImageTools.fadeElement(expectedElement);
        }
    }

    private int getExpectedElement(final int x, final int y, final int expectedLineOffset) {
        if (x < expectedImageWidth && y < expectedImageHeight) {
            return expectedBuffer.getElem(x + expectedLineOffset);
        }
        return 0;
    }

    private int getActualElement(final int x, final int y, final int actualLineOffset) {
        if (x < actualImageWidth && y < actualImageHeight) {
            return actualBuffer.getElem(x + actualLineOffset);
        }
        return 0;
    }

    /**
     * Levels the color intensity to at least 50 and at most maxIntensity.
     *
     * @param darkness     color component to level
     * @param maxIntensity highest possible intensity cut off
     * @return A value that is at least 50 and at most maxIntensity
     */
    private static int levelIntensity(final int darkness, final int maxIntensity) {
        return Math.min(maxIntensity, Math.max(50, darkness));
    }

    /**
     * Calculate the combined intensity of a pixel and normalize it to a value of at most 255.
     *
     * @param element a pixel encoded as an integer
     * @return the intensity of all colors combined cut off at a maximum of 255
     */
    private static int calcCombinedIntensity(final int element) {
        final Color color = new Color(element);
        return Math.min(255, (color.getRed() + color.getGreen() + color.getRed()) / 3);
    }

    private static void mark(final DataBuffer image, final int x, final int y, final int imageWidth) {
        final int yOffset = y * imageWidth;
        for (int i = 0; i < MARKER_WIDTH; i++) {
            image.setElem(x + i * imageWidth, MARKER_RGB);
            image.setElem(i + yOffset, MARKER_RGB);
        }
    }

    public static int color(final int r, final int g, final int b) {
        return new Color(r, g, b).getRGB();
    }

    @Override
    public String toString() {
        return "DiffImage{" +
                "page=" + page +
                '}';
    }
}
