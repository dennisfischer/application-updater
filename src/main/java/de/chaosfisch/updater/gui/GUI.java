package de.chaosfisch.updater.gui;

import de.chaosfisch.updater.Launcher;
import de.chaosfisch.updater.Version;
import de.chaosfisch.updater.gui.controller.GUIController;
import insidefx.undecorator.Undecorator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class GUI extends Application {

	private static Launcher launcher;
	private static final Logger LOGGER = LoggerFactory.getLogger(GUI.class);
	private Stage stage;

	public static void setLauncher(final Launcher launcher) {
		GUI.launcher = launcher;
	}

	@Override
	public void start(final Stage stage) {
		if (null == launcher) {
			LOGGER.error("Launcher not set!");
			return;
		}
		launcher.setGUI(this);
		this.stage = stage;
	}

	public void go() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				final URL location = getClass().getResource("/de/chaosfisch/updater/view/Launcher.fxml");
				final FXMLLoader fxmlLoader = new FXMLLoader(location);
				fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
				try {
					fxmlLoader.load();
				} catch (IOException e) {
					LOGGER.error("failed loading fxml");
				}

				final GUIController controller = fxmlLoader.getController();
				controller.setReleaseNotes(generateReleaseNotes().toString());
				controller.setLauncher(launcher);
				final GridPane root = fxmlLoader.getRoot();
				final Scene scene = new Scene(root, 500.0, 400.0);
				root.toFront();

				// Transparent scene and stage

				// Set minimum size
				stage.setMinWidth(500.0);
				stage.setMinHeight(400.0);

				stage.setTitle("Simple Java YouTube Uploader Updater");
				stage.getIcons().add(new Image(getClass().getResourceAsStream("/de/chaosfisch/uploader/resources/images/film.png")));
				stage.setScene(scene);
				stage.show();
			}
		});
	}

	private StringBuilder generateReleaseNotes() {
		final StringBuilder data = new StringBuilder();
		data.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3" + "" +
				".org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
		data.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n");
		data.append("<head><title></title><style type=\"text/css\">");
		data.append(" .version { padding:4px 4px 4px 4px; background: #4775A3; font-family: Tahoma; font-size: 14px; " + "font-weight: bold; }\n");
		data.append(".info { font-size: 15px; }");
		data.append("</style></head><body>");
		data.append(generateEntries(launcher.getVersions()).toString());
		data.append("</body></html>");
		return data;
	}

	private StringBuilder generateEntries(final List<Version> versions) {
		final StringBuilder stringBuilder = new StringBuilder();
		for (final Version version : versions) {
			stringBuilder.append(" <div class=\"entry\">\n");
			stringBuilder.append(" <p class=\"version\">")
					.append("Version: ")
					.append(version.getVersion())
					.append("</p>\n");
			stringBuilder.append(" <p class=\"info\">").append(version.getReleaseNotes()).append("</p>\n");
			stringBuilder.append(" </div>\n");
		}
		return stringBuilder;
	}
}
