package de.redsix.pdfcompare;

import static de.redsix.pdfcompare.PdfComparator.DPI;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exclusions collect rectangular areas of the document, that shall be ignored during comparison.
 * Each area is specified through a {@link PageArea} object.
 *
 * Exclusions can be read from a file in JSON format (or actually a superset called <a href="https://github.com/lightbend/config/blob/master/HOCON.md">HOCON</a>) which has the following form:
 * <pre>
 * exclusions: [
 *     {
 *         page: 2
 *         x1: 300 // entries without a unit are in pixels, when Pdf is rendered at 300DPI
 *         y1: 1000
 *         x2: 550
 *         y2: 1300
 *     },
 *     {
 *         // page is optional. When not given, the exclusion applies to all pages.
 *         x1: 130.5mm // entries can also be given in units of cm, mm or pt (DTP-Point defined as 1/72 Inches)
 *         y1: 3.3cm
 *         x2: 190mm
 *         y2: 3.7cm
 *     },
 *     {
 *         page: 7
 *         // coordinates are optional. When not given, the whole page is excluded.
 *     }
 * ]</pre>
 *
 */
public class Exclusions {

    private static final Logger LOG = LoggerFactory.getLogger(Exclusions.class);
    private static final float CM_TO_PIXEL = 1 / 2.54f * DPI;
    private static final float MM_TO_PIXEL = CM_TO_PIXEL / 10;
    private static final float PT_TO_PIXEL = 300f / 72f;
    private static final Pattern NUMBER = Pattern.compile("([0-9.]+)(cm|mm|pt)");
    private static final ConfigParseOptions configParseOptions = ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF).setAllowMissing(true);
    private final Map<Integer, PageExclusions> exclusionsPerPage = new HashMap<>();
    private final PageExclusions exclusionsForAllPages = new PageExclusions();

    public Exclusions add(final PageArea exclusion) {
        if (exclusion.page < 0) {
            exclusionsForAllPages.add(exclusion);
        } else {
            exclusionsPerPage.computeIfAbsent(exclusion.page, k -> new PageExclusions(exclusionsForAllPages)).add(exclusion);
        }
        return this;
    }

    public PageExclusions forPage(final int page) {
        return exclusionsPerPage.getOrDefault(page, exclusionsForAllPages);
    }

    public void readExclusions(final String filename) {
        Objects.requireNonNull(filename, "filename must not be null");
        readExclusions(new File(filename));
    }

    public void readExclusions(final Path path) {
        Objects.requireNonNull(path, "path must not be null");
        if (Files.exists(path)) {
            readExclusions(path.toFile());
        } else {
            LOG.info("Ignore-file at '{}' not found. Continuing without ignores.", path);
        }
    }

    public void readExclusions(final File file) {
        Objects.requireNonNull(file, "file must not be null");
        if (file.exists()) {
            final Config config = ConfigFactory.parseFile(file, configParseOptions);
            readFromConfig(config);
        } else {
            LOG.info("Ignore-file at '{}' not found. Continuing without ignores.", file);
        }
    }

    public void readExclusions(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
            try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                readExclusions(inputStreamReader);
            } catch (IOException e) {
                LOG.warn("Could not read ignores from InputStream. Continuing without ignores.", e);
            }
    }

    public void readExclusions(Reader reader) {
        Objects.requireNonNull(reader, "reader must not be null");
        final Config config = ConfigFactory.parseReader(reader, configParseOptions);
        readFromConfig(config);
    }

    private void readFromConfig(final Config load) {
        final List<? extends ConfigObject> exclusions = load.getObjectList("exclusions");
        exclusions.stream().map(co -> {
            final Config c = co.toConfig();
            if (!c.hasPath("x1") && !c.hasPath("y1") && !c.hasPath("x2") && !c.hasPath("y2")) {
                return new PageArea(c.getInt("page"));
            }
            if (c.hasPath("page")) {
                return new PageArea(c.getInt("page"), toPix(c, "x1"), toPix(c, "y1"), toPix(c, "x2"), toPix(c,"y2"));
            }
            return new PageArea(toPix(c, "x1"), toPix(c, "y1"), toPix(c, "x2"), toPix(c,"y2"));
        }).forEach(e -> add(e));
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

    public void forEach(final Consumer<PageArea> exclusionConsumer) {
        exclusionsForAllPages.forEach(exclusionConsumer);
        exclusionsPerPage.values().forEach(pe -> pe.forEach(exclusionConsumer));
    }
}
