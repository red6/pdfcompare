package de.redsix.pdfcompare;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collection;

public class PageExclusions {

	private final Collection<PageArea> exclusions = newArrayList();
	private final PageExclusions delegate;

	public PageExclusions() {
		delegate = null;
	}

	public PageExclusions(final PageExclusions delegate) {
		this.delegate = delegate;
	}

	public void add(final PageArea exclusion) {
		exclusions.add(exclusion);
	}

	public boolean contains(final int x, final int y) {
		for (PageArea exclusion : exclusions) {
			if (exclusion.contains(x, y)) {
				return true;
			}
		}
		if (delegate != null) {
			return delegate.contains(x, y);
		}
		return false;
	}

}
