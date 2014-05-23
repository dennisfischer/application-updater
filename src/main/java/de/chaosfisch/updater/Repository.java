package de.chaosfisch.updater;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.sun.javafx.PlatformUtil;
import de.chaosfisch.updater.unzip.UnZip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class Repository {

	private final String  dataDir;
	private final String  appDir;
	private final String  downloadsDir;
	private final URL     versionFileUrl;
	private final Version current;

	private final        UnZip                    unZip     = new UnZip();
	private final        List<RepositoryListener> listeners = new ArrayList<>(5);
	private static final Logger                   LOGGER    = LoggerFactory.getLogger(Repository.class);

	public Repository(final String dataDir, final String appDir, final URL versionFileUrl, final Version current) {
		this.dataDir = dataDir;
		this.appDir = appDir;
		downloadsDir = appDir + "/downloads/";
		this.versionFileUrl = versionFileUrl;
		this.current = current;

		final File downloads = new File(downloadsDir);
		if (!downloads.exists()) {
			downloads.mkdir();
		}
	}

	public Process forkJarProcess(final String cmd) {
		final String javaBin = String.format("%s/bin/java%s", System.getProperty("java.home"), PlatformUtil.isWindows() ?
																							   ".exe" :
																							   "");
		try {
			final ProcessBuilder processBuilder = new ProcessBuilder(javaBin, "-jar", cmd);
			processBuilder.redirectErrorStream(true);
			processBuilder.redirectOutput(File.createTempFile("sjyu-updater", ".txt"));
			return processBuilder.start();
		} catch (IOException e) {
			LOGGER.error("Couldn't start application", e);
		}
		return null;
	}

	public static void removeRecursive(Path path) throws IOException
	{
		Files.walkFileTree(path, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
					throws IOException
			{
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
			{
				// try to delete the file anyway, even if its attributes
				// could not be read, since delete-only access is
				// theoretically possible
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
			{
				if (exc == null)
				{
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
				else
				{
					// directory iteration failed; propagate exception
					throw exc;
				}
			}
		});
	}

	public void cleanUp() {
		final File[] files = Paths.get("").toAbsolutePath().toFile().listFiles(new VersionFilter());
		final ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));
		Collections.sort(fileList, new Comparator<File>() {

			private VersionComparator comparator;

			@Override
			public int compare(final File o1, final File o2) {
				if (null == comparator) {
					comparator = new VersionComparator();
				}
				return comparator.compareVersionStrings(o1.getName(), o2.getName());
			}
		});

		fileList.remove(fileList.size()-1);
		fileList.remove(fileList.size()-1);

		for(File file : fileList) {
			File dataFile = new File(dataDir + "/" + file.getName());
			System.out.println(file.getAbsolutePath());
			System.out.println(dataFile.getAbsolutePath());
			try {
				removeRecursive(file.toPath());
				removeRecursive(dataFile.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	enum Event {
		DOWNLOAD_FILE, DOWNLOAD_PROGRESS, BACKUP, UNZIP, DONE, MIGRATION, DOWNLOAD_START
	}

	public void fireEvent(final Repository.Event event, final Object data) {
		for (final RepositoryListener listener : listeners) {
			switch (event) {
				case DOWNLOAD_FILE:
					listener.downloadFile(String.valueOf(data));
					break;
				case DOWNLOAD_PROGRESS:
					if (data instanceof Double) {
						listener.downloadProgress((Double) data);
					}
					break;
				case DOWNLOAD_START:
					if (data instanceof Integer) {
						listener.downloadStart((Integer) data);
					}
					break;
				case BACKUP:
					listener.backup();
					break;
				case UNZIP:
					listener.unzip();
					break;
				case DONE:
					listener.done();
					break;
				case MIGRATION:
					listener.migration(String.valueOf(data));
					break;
			}
		}
	}

	public List<Version> downloadLatest() throws IOException {
		final Gson gson = new Gson();
		try (final InputStreamReader inputStreamReader = new InputStreamReader(versionFileUrl.openStream())) {
			final VersionGroup versionGroup = gson.fromJson(inputStreamReader, VersionGroup.class);
			return versionGroup.getNewerThan(current);
		}
	}

	public void upgradeInstallation(final List<Version> versions) throws IOException, InterruptedException {
		listeners.forEach(unZip::addListener);

		fireEvent(Repository.Event.DOWNLOAD_START, versions.size());

		for (final Version version : versions) {
			fireEvent(Repository.Event.DOWNLOAD_FILE, String.format("%s.zip", version.getVersion()));
			downloadVersion(version);
		}

		fireEvent(Repository.Event.BACKUP, null);
		final String versionName = versions.get(versions.size() - 1).getVersion();
		copyAppAndData(versionName);
		cleanApp(versions.get(versions.size() - 1), versionName);

		final File directory = new File(String.format("%s/%s", appDir, versionName));
		fireEvent(Repository.Event.UNZIP, null);
		for (final Version version : versions) {
			upgradeVersion(version, directory);
			migrateVersion(version, directory, versions.get(versions.size()-1).getVersion());
		}

		fireEvent(Repository.Event.DONE, null);
	}

	private void cleanApp(final Version version, final String versionName) {
		for (final String deleteName : version.getDeleted()) {
			final File file = new File(String.format("%s%s/%s", appDir, versionName, deleteName));
			if (file.exists()) {
				file.delete();
			}
		}
	}

	private void copyAppAndData(final String versionName) throws IOException {
		copyApp(versionName);
		copyData(versionName);
	}

	private void copyData(final String versionName) throws IOException {
		final File dataDirectory = new File(String.format("%s/%s", dataDir, current.getVersion()));
		final File updatedDataDirectory = new File(String.format("%s/%s", dataDir, versionName));
		if (!updatedDataDirectory.exists()) {
			Files.createDirectories(Paths.get(updatedDataDirectory.toURI()));
		}
		if (dataDirectory.exists()) {
			Files.walkFileTree(Paths.get(dataDirectory.toURI()), new Repository.CopyFileVisitor(Paths.get(updatedDataDirectory
					.toURI())));
		}
	}

	private void copyApp(final String versionName) throws IOException {
		final File appDirectory = new File(String.format("%s/%s", appDir, current.getVersion()));
		final File updatedAppDirectory = new File(String.format("%s/%s", appDir, versionName));

		if (!updatedAppDirectory.exists()) {
			Files.createDirectories(Paths.get(updatedAppDirectory.toURI()));
		}

		if (appDirectory.exists()) {
			Files.walkFileTree(Paths.get(appDirectory.toURI()), new Repository.CopyFileVisitor(Paths.get(updatedAppDirectory
					.toURI())));
		}
	}

	private void downloadVersion(final Version version) throws IOException {
		final File file = new File(String.format("%s/%s.zip", downloadsDir, version.getVersion()));

		if (file.exists()) {
			final HashCode hc = com.google.common.io.Files.hash(file, Hashing.sha1());
			if (hc.toString().equals(version.getSha1())) {
				return;
			}
		}

		final URL zipPackage = URI.create(version.getPackageName()).toURL();
		final URLConnection connection = zipPackage.openConnection();
		connection.connect();
		final int lengthOfFile = connection.getContentLength();
		try (final InputStream input = new BufferedInputStream(zipPackage.openStream(), 8192);
			 final OutputStream output = new FileOutputStream(file)) {
			int total = 0;
			final byte[] data = new byte[65536];
			int count;
			while (-1 != (count = input.read(data))) {
				total += count;
				fireEvent(Repository.Event.DOWNLOAD_PROGRESS, total / (double) lengthOfFile);
				output.write(data, 0, count);
			}
			output.flush();
		}
	}

	private void migrateVersion(final Version version, final File directory, final String newVersion) throws InterruptedException {
		final File file = new File(String.format("%s/migrations/%s.jar", directory, version.getVersion()));
		if (file.exists()) {
			fireEvent(Repository.Event.MIGRATION, version.getVersion());

			final Process process = forkJarProcess(file.getAbsolutePath() + " " + newVersion);
			final int exitCode = process.waitFor();
			if (0 != exitCode) {
				LOGGER.error("Migration failed with exit code {}", exitCode);
				throw new RuntimeException("Migration failed");
			}
		}
	}

	private void upgradeVersion(final Version version, final File directory) throws IOException {
		unZip.unzip(String.format("%s/%s.zip", downloadsDir, version.getVersion()), directory.getAbsolutePath());
	}

	public void addListener(final RepositoryListener listener) {
		listeners.add(listener);
	}

	private static class CopyFileVisitor extends SimpleFileVisitor<Path> {
		private final Path targetPath;
		private       Path sourcePath;

		CopyFileVisitor(final Path targetPath) {
			this.targetPath = targetPath;
		}

		@Override
		public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
			if (null == sourcePath) {
				sourcePath = dir;
			} else {
				if (!Files.exists(targetPath.resolve(sourcePath.relativize(dir)))) {
					Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
				}
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
			if (!Files.exists(targetPath.resolve(sourcePath.relativize(file)))) {
				Files.copy(file, targetPath.resolve(sourcePath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
			}
			return FileVisitResult.CONTINUE;
		}
	}
}
