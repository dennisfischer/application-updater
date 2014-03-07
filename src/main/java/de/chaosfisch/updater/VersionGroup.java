package de.chaosfisch.updater;

import java.util.*;

public class VersionGroup {
	private final List<Version> versions = new ArrayList<>(50);

	public VersionGroup() {
	}

	public VersionGroup(final Collection<Version> versions) {
		setVersions(versions);
	}

	public List<Version> getVersions() {
		return new ArrayList<>(versions);
	}

	public void setVersions(final Collection<Version> versions) {
		this.versions.clear();
		this.versions.addAll(versions);
	}

	public List<Version> getNewerThan(final Version current) {
		Collections.sort(versions, new VersionComparator());

		if (versions.contains(current)) {
			return versions.subList(versions.indexOf(current) + 1, versions.size());
		}

		return new ArrayList<>(0);
	}

}
