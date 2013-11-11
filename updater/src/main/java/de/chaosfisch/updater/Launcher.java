package de.chaosfisch.updater;

import de.chaosfisch.updater.gui.GUI;
import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class Launcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
	private final String        updateFile;
	private final String        dataDir;
	private final String        appDir;
	private final String        currentVersion;
	private final Repository    repository;
	private       List<Version> versions;
	private       GUI           gui;

	private Launcher(final String updateFile, final String dataDir, final String appDir, final String currentVersion) throws MalformedURLException {
		this.updateFile = updateFile;
		this.dataDir = dataDir;
		this.appDir = appDir;
		this.currentVersion = currentVersion;
		final URL versionFileURL = URI.create(updateFile).toURL();
		repository = new Repository(dataDir, appDir, versionFileURL, new Version(currentVersion));
	}

	private boolean hasUpdate() throws IOException {
		versions = repository.downloadLatest();
		return !versions.isEmpty();
	}

	private void startUpdater() {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				GUI.setLauncher(Launcher.this);
				Application.launch(GUI.class, dataDir, appDir, currentVersion);
			}
		});
		thread.start();
	}

	public String getUpdateFile() {
		return updateFile;
	}

	public String getDataDir() {
		return dataDir;
	}

	public String getAppDir() {
		return appDir;
	}

	public String getCurrentVersion() {
		return currentVersion;
	}

	public Repository getRepository() {
		return repository;
	}

	public List<Version> getVersions() {
		return Collections.unmodifiableList(versions);
	}

	public Process startVersion(final String version) {
		final String currentDir = Paths.get("").toAbsolutePath().toString();
		final String cmd = String.format("\"%s/%s/SimpleJavaYoutubeUploader.jar\"", currentDir, version);

		if (!Files.exists(Paths.get(cmd.substring(1, cmd.length() - 1)))) {
			return null;
		}

		return repository.forkJarProcess(cmd);
	}

	public static void main(final String... args) throws Exception {
		try {
			final String current = getCurrrent();

			final Launcher launcher = new Launcher("http://dev.chaosfisch.com/updates/updates.json", System.getProperty("user.home") + "/SimpleJavaYoutubeUploader/", Paths
					.get("")
					.toAbsolutePath()
					.toString(), current);
			launcher.startUpdater();

			final Process process = launcher.startVersion(current);

			if (launcher.hasUpdate()) {
				LOGGER.info("Newer version existing!");
				if (null != process) {
					process.destroy();
				}
				launcher.goGUI();
			} else {
				LOGGER.info("Up-To-Date");
				System.exit(0);
			}
		} catch (IOException e) {
			LOGGER.warn("Exception in Launcher", e);
		}
	}

	private void goGUI() throws Exception {
		gui.go();
	}

	private static String getCurrrent() {
		final File[] files = Paths.get("").toAbsolutePath().toFile().listFiles(new Launcher.VersionFilter());
		final List<File> fileList = Arrays.asList(files);
		if (fileList.isEmpty()) {
			return "v3.0.0.0";
		}
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

		return fileList.get(fileList.size() - 1).getName();
	}

	public void setGUI(final GUI gui) {
		this.gui = gui;
	}

	private static class VersionFilter implements FileFilter {
		private final Pattern pattern = Pattern.compile("^(.*)[\\\\|/]v\\d+.\\d+.\\d+.\\d+$");

		VersionFilter() {
		}

		@Override
		public boolean accept(final File pathname) {
			return pathname.isDirectory() && pattern.matcher(pathname.toString()).find();
		}
	}
}
