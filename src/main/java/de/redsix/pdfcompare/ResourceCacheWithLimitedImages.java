package de.redsix.pdfcompare;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.DefaultResourceCache;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceCacheWithLimitedImages extends DefaultResourceCache {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceCacheWithLimitedImages.class);
    private final Map<COSObject, SoftReference<PDXObject>> xobjects = new LinkedHashMap<COSObject, SoftReference<PDXObject>>() {

        @Override
        protected boolean removeEldestEntry(final Entry<COSObject, SoftReference<PDXObject>> eldest) {
            return size() > Environment.getNrOfImagesToCache();
        }
    };

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
        if (length > Environment.getMaxImageSize()) {
            LOG.trace("Not caching image with Size: {}", length);
            return;
        }
        this.xobjects.put(indirect, new SoftReference(xobject));
    }
}
