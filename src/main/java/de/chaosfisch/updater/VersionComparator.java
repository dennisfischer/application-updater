package de.chaosfisch.updater;

import java.util.Comparator;
import java.util.regex.Pattern;

class VersionComparator implements Comparator<Version> {
	private static final Pattern COMPILE = Pattern.compile("\\.");

	@Override
	public int compare(final Version o1, final Version o2) {

		return compareVersionStrings(o1.getVersion(), o2.getVersion());
	}

	public int compareVersionStrings(final String version1, final String version2) {
		final String[] vals1 = COMPILE.split(version1);
		final String[] vals2 = COMPILE.split(version2);

		int i = 0;
		final int lengthVersion1 = vals1.length;
		final int lengthVersion2 = vals2.length;
		while (i < lengthVersion1 && i < lengthVersion2 && vals1[i].equals(vals2[i])) {
			i++;
		}

		if (i < lengthVersion1 && i < lengthVersion2) {
			final int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
			return 0 > diff ? -1 : 0 == diff ? 0 : 1;
		}

		return lengthVersion1 < lengthVersion2 ? -1 : lengthVersion1 == lengthVersion2 ? 0 : 1;
	}
}
