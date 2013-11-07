package de.chaosfisch.updater.unzip;

public interface UnZipListener {

	void unzipFile(String fileName);

	void unzipProgres(float data);

	void unzipEnd(final String fileName);

	void unzipStart(final String fileName);
}
