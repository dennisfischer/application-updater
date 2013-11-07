package de.chaosfisch.updater.gui.controller;

import de.chaosfisch.updater.Launcher;
import de.chaosfisch.updater.RepositoryListenerAdapter;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class GUIController {
	@FXML
	private Button updateButton;

	@FXML
	private Button checkLaterButton;

	@FXML
	private Button skipButton;

	@FXML
	private ResourceBundle resources;

	@FXML
	private URL location;

	@FXML
	private WebView details;

	@FXML
	private ProgressBar progressBar;

	@FXML
	private Label statusLabel;

	private Launcher launcher;
	private static final Logger LOGGER = LoggerFactory.getLogger(GUIController.class);

	@FXML
	void checkLater(final ActionEvent event) {
		launcher.startVersion(launcher.getCurrentVersion());
	}

	@FXML
	void skipUpdate(final ActionEvent event) {
		launcher.startVersion(launcher.getCurrentVersion());
	}

	@FXML
	void updateApplication(final ActionEvent event) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				updateButton.setDisable(true);
				checkLaterButton.setDisable(true);
				skipButton.setDisable(true);
			}
		});

		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					launcher.getRepository().upgradeInstallation(launcher.getVersions());
					launcher.startVersion(launcher.getVersions().get(launcher.getVersions().size() - 1).getVersion());
				} catch (Exception e) {
					LOGGER.error("Exception", e);
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							statusLabel.setText("Error occurred - see updaterlog.html");
							progressBar.setProgress(1);
							final ColorAdjust adjust = new ColorAdjust();
							adjust.setHue(0.85);
							progressBar.setEffect(adjust);
						}
					});
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	@FXML
	void initialize() {
		assert null != details : "fx:id=\"details\" was not injected: check your FXML file 'Launcher.fxml'.";
		assert null != progressBar : "fx:id=\"progressBar\" was not injected: check your FXML file 'Launcher.fxml'.";
		assert null != statusLabel : "fx:id=\"statusLabel\" was not injected: check your FXML file 'Launcher.fxml'.";
		assert null != updateButton : "fx:id=\"updateButton\" was not injected: check your FXML file 'Launcher.fxml'.";
		assert null != checkLaterButton : "fx:id=\"checkLaterButton\" was not injected: check your FXML file 'Launcher.fxml'.";
		assert null != skipButton : "fx:id=\"skipButton\" was not injected: check your FXML file 'Launcher.fxml'.";
	}

	public void setReleaseNotes(final String data) {
		details.getEngine().loadContent(data);
	}

	public void setLauncher(final Launcher launcher) {
		this.launcher = launcher;

		launcher.getRepository().addListener(new GUIController.GUIRepositoryListener());
	}

	private class GUIRepositoryListener extends RepositoryListenerAdapter {

		private double fileCount;
		private double currentFile;

		GUIRepositoryListener() {
		}

		@Override
		public void unzipFile(final String fileName) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					statusLabel.setText(String.format("Unpacking %s.", fileName));
				}
			});
		}

		@Override
		public void unzipProgres(final float data) {
			progress(data);
		}

		@Override
		public void unzipEnd(final String fileName) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					statusLabel.setText(String.format("Unzipping %s done.", fileName));
				}
			});
		}

		@Override
		public void unzipStart(final String fileName) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					currentFile++;
					statusLabel.setText(String.format("Unzipping %s...", fileName));
				}
			});
		}

		@Override
		public void backup() {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					statusLabel.setText("Creating backups");
					progressBar.setProgress(-1);
				}
			});
		}

		@Override
		public void unzip() {
			currentFile = 0;

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					progressBar.setProgress(0);
				}
			});
		}

		@Override
		public void downloadFile(final String fileName) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					currentFile++;
					statusLabel.setText(String.format("Downloading %s.", fileName));
				}
			});
		}

		@Override
		public void downloadProgress(final double data) {
			progress(data);
		}

		@Override
		public void downloadStart(final int fileCount) {
			this.fileCount = fileCount;
			currentFile = 0;

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					progressBar.setProgress(0);
				}
			});
		}

		@Override
		public void done() {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					statusLabel.setText("Update successful!");
				}
			});
		}

		@Override
		public void migration(final String version) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					statusLabel.setText(String.format("Migrating from version %s!", version));
				}
			});
		}

		private void progress(final double data) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					progressBar.setProgress((currentFile - 1) / fileCount + 1 / fileCount * data);
				}
			});
		}
	}
}
