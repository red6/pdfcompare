package de.redsix.pdfcompare;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.val;

public class FileUtils {

	private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);
	private static Collection<File> tempDirs = new ConcurrentLinkedQueue<File>();
	private static volatile boolean shutdownRegistered;
	private static File tempDirParent;

	public static void setTempDirParent(final File tempDirParentPath) {
		tempDirParent = tempDirParentPath;
	}

	private static synchronized void addShutdownHook() {
		if (!shutdownRegistered) {
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					removeTempDirs();
				}
			}));
			shutdownRegistered = true;
		}
	}

	private static void removeTempDirs() {
		for (val dir : tempDirs) {
			FileUtils.removeTempDir(dir);
		}
	}

	public static File createTempDir(final String prefix) throws IOException {
		val tempDir = createTempDir(tempDirParent, prefix);
		tempDirs.add(tempDir);
		addShutdownHook();
		return tempDir;
	}

	private static File createTempDir(final File tempDirParent, final String prefix) throws IOException {
		File x;
		if (tempDirParent != null) {
			x = File.createTempFile(prefix, "", tempDirParent);
		} else {
			x = File.createTempFile(prefix, "");
		}
		x.delete();
		x.mkdir();
		return x;
	}

	public static void removeTempDir(final File tempDir) {
		try {
			tempDirs.remove(tempDir);
			org.apache.commons.io.FileUtils.forceDelete(tempDir);
		} catch (IOException e) {
			LOG.warn("Error removing temporary directory: {}", tempDir, e);
		}
	}

	public static List<File> getPaths(final File dir, final String glob) throws IOException {
		List<File> paths = new ArrayList<File>();
		for (val path : org.apache.commons.io.FileUtils.listFiles(dir, new RegexFileFilter(glob),
				TrueFileFilter.TRUE)) {
			paths.add(path);
		}
		Collections.sort(paths);
		return paths;
	}
}
