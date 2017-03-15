package de.redsix.pdfcompare;

import java.io.IOException;

import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.ResourceCache;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

public class DummyResourceCache implements ResourceCache {

    @Override
    public PDFont getFont(final COSObject indirect) throws IOException {
        return null;
    }

    @Override
    public PDColorSpace getColorSpace(final COSObject indirect) throws IOException {
        return null;
    }

    @Override
    public PDExtendedGraphicsState getExtGState(final COSObject indirect) {
        return null;
    }

    @Override
    public PDShading getShading(final COSObject indirect) throws IOException {
        return null;
    }

    @Override
    public PDAbstractPattern getPattern(final COSObject indirect) throws IOException {
        return null;
    }

    @Override
    public PDPropertyList getProperties(final COSObject indirect) {
        return null;
    }

    @Override
    public PDXObject getXObject(final COSObject indirect) throws IOException {
        return null;
    }

    @Override
    public void put(final COSObject indirect, final PDFont font) throws IOException {}

    @Override
    public void put(final COSObject indirect, final PDColorSpace colorSpace) throws IOException {}

    @Override
    public void put(final COSObject indirect, final PDExtendedGraphicsState extGState) {}

    @Override
    public void put(final COSObject indirect, final PDShading shading) throws IOException {}

    @Override
    public void put(final COSObject indirect, final PDAbstractPattern pattern) throws IOException {}

    @Override
    public void put(final COSObject indirect, final PDPropertyList propertyList) {}

    @Override
    public void put(final COSObject indirect, final PDXObject xobject) throws IOException {}
}
