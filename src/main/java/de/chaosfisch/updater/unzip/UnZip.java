package de.chaosfisch.updater.unzip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class UnZip {

	enum Event {
		UNZIP_FILE, UNZIP_PROGRESS, UNZIP_START, UNZIP_END
	}

	private final Collection<UnZipListener> unZipListenerList = new ArrayList<>(5);

	public void addListener(final UnZipListener unZipListener) {
		unZipListenerList.add(unZipListener);
	}

	public void fireEvent(final UnZip.Event event, final Object data) {
		for (final UnZipListener listener : unZipListenerList) {
			switch (event) {
				case UNZIP_FILE:
					listener.unzipFile(String.valueOf(data));
					break;
				case UNZIP_PROGRESS:
					listener.unzipProgres((Float) data);
					break;
				case UNZIP_START:
					listener.unzipStart(String.valueOf(data));
					break;
				case UNZIP_END:
					listener.unzipEnd(String.valueOf(data));
					break;
			}
		}
	}

	/**
	 * Unzip it
	 *
	 * @param zipFileName
	 * 		input zip file
	 * @param outputFolder
	 * 		zip file output folder
	 */
	public void unzip(final String zipFileName, final String outputFolder) throws IOException {

		fireEvent(UnZip.Event.UNZIP_START, zipFileName);

		final File folder = new File(outputFolder);
		if (!folder.exists()) {
			folder.mkdirs();
		}

		final int total;
		try (final ZipFile zipFile = new ZipFile(zipFileName)) {
			total = zipFile.size();
		}

		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFileName))) {
			//get the zipped file list entry
			ZipEntry ze = zis.getNextEntry();

			final byte[] buffer = new byte[1024];
			int processed = 0;
			while (null != ze) {

				final String fileName = ze.getName();
				final File newFile = new File(String.format("%s/%s", outputFolder, fileName));

				fireEvent(UnZip.Event.UNZIP_FILE, fileName);

				processed++;
				fireEvent(UnZip.Event.UNZIP_PROGRESS, processed / (float) total);

				if (ze.isDirectory()) {
					newFile.mkdirs();
				}
				if (!newFile.isDirectory()) {
					try (FileOutputStream fos = new FileOutputStream(newFile)) {
						int len;
						while (0 < (len = zis.read(buffer))) {
							fos.write(buffer, 0, len);
						}
					}
				}
				ze = zis.getNextEntry();
			}

			zis.closeEntry();
		}

		fireEvent(UnZip.Event.UNZIP_END, zipFileName);
	}
}