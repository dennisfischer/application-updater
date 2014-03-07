package de.chaosfisch.updater;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Version {
	private String sha1;
	private String version;
	private String releaseNotes;
	private String packageName;
	private final List<String> deleted = new ArrayList<>(50);

	public Version() {
	}

	public Version(final String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(final String version) {
		this.version = version;
	}

	public String getReleaseNotes() {
		return releaseNotes;
	}

	public void setReleaseNotes(final String releaseNotes) {
		this.releaseNotes = releaseNotes;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(final String packageName) {
		this.packageName = packageName;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Version)) {
			return false;
		}

		final Version version1 = (Version) obj;

		return version.equals(version1.version);
	}

	@Override
	public int hashCode() {
		return version.hashCode();
	}

	public List<String> getDeleted() {
		return new ArrayList<>(deleted);
	}

	public void setDeleted(final Collection<String> deleted) {
		this.deleted.clear();
		this.deleted.addAll(deleted);
	}

	public String getSha1() {
		return sha1;
	}

	public void setSha1(final String sha1) {
		this.sha1 = sha1;
	}
}
