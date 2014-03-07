package de.chaosfisch.updater;

import de.chaosfisch.updater.unzip.UnZipListenerAdapter;

public class RepositoryListenerAdapter extends UnZipListenerAdapter implements RepositoryListener {

	@Override
	public void downloadFile(final String fileName) {
	}

	@Override
	public void downloadProgress(final double data) {
	}

	@Override
	public void downloadStart(final int fileCount) {
	}

	@Override
	public void unzip() {
	}

	@Override
	public void backup() {
	}

	@Override
	public void done() {
	}

	@Override
	public void migration(final String version) {
	}
}
