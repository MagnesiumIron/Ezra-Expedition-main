package io.github.devsimulator;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main (String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("DevSimulator");
        config.setWindowedMode(960, 640);
        config.setForegroundFPS(60);

        new Lwjgl3Application(new Main(), config);
    }
}
