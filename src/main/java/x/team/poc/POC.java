package x.team.poc;

import java.awt.Color;

import de.redsix.pdfcompare.CompareResult;
import de.redsix.pdfcompare.PdfComparator;
import de.redsix.pdfcompare.env.SimpleEnvironment;

public class POC {

	public static void main(String[] args) {
		try {

			PdfComparator compare = new PdfComparator("input/expected.pdf", "input/actual.pdf")
					.withEnvironment(new SimpleEnvironment().setActualColor(Color.red)
			        .setExpectedColor(Color.white).setAddEqualPagesToResult(true)
			        .setEnableHorizontalCompareOutput(true));
			
			CompareResult result = compare.compare();
			result	.writeTo("output/diffOutput");
			if (result.isNotEqual()) {
				System.out.println("Ho trovato differenza!");
			}
			if (result.isEqual()) {
				System.out.println("Non ho trovato nessuna differenza!");
			}
			result.getDifferences();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
