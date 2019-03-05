package de.redsix.pdfcompare;

import static com.google.common.collect.Maps.newHashMap;
import static de.redsix.pdfcompare.PdfComparator.DPI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;

import lombok.Cleanup;
import lombok.val;

public class Exclusions {

	private static final float CM_TO_PIXEL = 1 / 2.54f * DPI;
	private static final float MM_TO_PIXEL = CM_TO_PIXEL / 10;
	private static final float PT_TO_PIXEL = 300f / 72f;
	private static final Pattern NUMBER = Pattern.compile("([0-9.]+)(cm|mm|pt)");
	private static final ConfigParseOptions configParseOptions = ConfigParseOptions.defaults()
			.setSyntax(ConfigSyntax.CONF).setAllowMissing(true);
	private final Map<Integer, PageExclusions> exclusionsPerPage = newHashMap();
	private final PageExclusions exclusionsForAllPages = new PageExclusions();

	public Exclusions add(final PageArea exclusion) {
		if (exclusion.page < 0) {
			exclusionsForAllPages.add(exclusion);
		} else {
			if (exclusionsPerPage.get(exclusion.page) == null) {
				val x = new PageExclusions(exclusionsForAllPages);
				x.add(exclusion);
				exclusionsPerPage.put(exclusion.page, x);
			} 
		}
		return this;
	}

	public PageExclusions forPage(final int page) {
		val p = Integer.valueOf(page);
		if (exclusionsPerPage.containsKey(p))
			return exclusionsPerPage.get(p);
		return exclusionsForAllPages;
	}

	public void readExclusions(final String filename) {
		if (filename != null) {
			readExclusions(new File(filename));
		}
	}

	public void readExclusions(final File file) {
		if (file != null && file.exists()) {
			final Config config = ConfigFactory.parseFile(file, configParseOptions);
			readFromConfig(config);
		}
	}

	public void readExclusions(InputStream inputStream) {
		if (inputStream != null) {
			try {
				@Cleanup
				val inputStreamReader = new InputStreamReader(inputStream, Charsets.UTF_8);
				readExclusions(inputStreamReader);
			} catch (IOException e) {

			}
		}
	}

	public void readExclusions(Reader reader) {
		if (reader != null) {
			final Config config = ConfigFactory.parseReader(reader, configParseOptions);
			readFromConfig(config);
		}
	}

	private void readFromConfig(final Config load) {
		final List<? extends ConfigObject> exclusions = load.getObjectList("exclusions");
		for (val co : exclusions) {
			val c = co.toConfig();
			PageArea pa;
			if (!c.hasPath("x1") && !c.hasPath("y1") && !c.hasPath("x2") && !c.hasPath("y2")) {
				pa = new PageArea(c.getInt("page"));
			} else if (c.hasPath("page")) {
				pa = new PageArea(c.getInt("page"), toPix(c, "x1"), toPix(c, "y1"), toPix(c, "x2"), toPix(c, "y2"));
			} else {
				pa = new PageArea(toPix(c, "x1"), toPix(c, "y1"), toPix(c, "x2"), toPix(c, "y2"));
			}
			this.add(pa);
		}
	}

	private int toPix(final Config c, final String key) {
		try {
			return c.getInt(key);
		} catch (ConfigException.WrongType e) {
			final String valueStr = c.getString(key);
			final Matcher matcher = NUMBER.matcher(valueStr);
			if (matcher.matches()) {
				float factor = 0;
				if ("mm".equals(matcher.group(2))) {
					factor = MM_TO_PIXEL;
				} else if ("cm".equals(matcher.group(2))) {
					factor = CM_TO_PIXEL;
				} else if ("pt".equals(matcher.group(2))) {
					factor = PT_TO_PIXEL;
				}
				return Math.round(factor * Float.parseFloat(matcher.group(1)));
			} else {
				throw new RuntimeException("Exclusion can't be read. String not parsable to a number: " + valueStr);
			}
		}
	}
}
