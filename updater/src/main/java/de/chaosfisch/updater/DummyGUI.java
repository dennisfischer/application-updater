package de.chaosfisch.updater;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class DummyGUI extends Application {
	@Override
	public void start(final Stage stage) throws Exception {
		Platform.exit();
	}
}
