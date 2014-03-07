package de.chaosfisch.updater;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

public class VersionFilter implements FileFilter {
	private final Pattern pattern = Pattern.compile("^(.*)[\\\\|/]v\\d+.\\d+.\\d+.\\d+$");

	VersionFilter() {
	}

	@Override
	public boolean accept(final File pathname) {
		return pathname.isDirectory() && pattern.matcher(pathname.toString()).find();
	}
}
