package de.chaosfisch.updater;

import de.chaosfisch.updater.unzip.UnZipListener;

public interface RepositoryListener extends UnZipListener {
	void downloadFile(String fileName);

	void downloadProgress(double data);

	void downloadStart(int fileCount);

	void backup();

	void unzip();

	void done();

	void migration(String version);
}
