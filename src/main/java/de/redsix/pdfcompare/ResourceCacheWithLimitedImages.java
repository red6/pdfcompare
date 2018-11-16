package de.redsix.pdfcompare;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.redsix.pdfcompare.env.Environment;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.DefaultResourceCache;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceCacheWithLimitedImages extends DefaultResourceCache {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceCacheWithLimitedImages.class);
    private final Environment environment;
    private final Map<COSObject, SoftReference<PDXObject>> xobjects = new LinkedHashMap<COSObject, SoftReference<PDXObject>>() {

        @Override
        protected boolean removeEldestEntry(final Entry<COSObject, SoftReference<PDXObject>> eldest) {
            return size() > environment.getNrOfImagesToCache();
        }
    };

    public ResourceCacheWithLimitedImages(Environment environment) {
        this.environment = environment;
    }

    @Override
    public PDXObject getXObject(COSObject indirect) throws IOException {
        SoftReference<PDXObject> xobject = this.xobjects.get(indirect);
        if (xobject != null) {
            return xobject.get();
        }
        return null;
    }

    @Override
    public void put(COSObject indirect, PDXObject xobject) throws IOException {
        final int length = xobject.getStream().getLength();
        if (length > environment.getMaxImageSize()) {
            LOG.trace("Not caching image with Size: {}", length);
            return;
        }
        if (xobject instanceof PDImageXObject) {
            PDImageXObject imageObj = (PDImageXObject) xobject;
            if (imageObj.getWidth() * imageObj.getHeight() > environment.getMaxImageSize()) {
                return;
            }
        }
        this.xobjects.put(indirect, new SoftReference<>(xobject));
    }
}
