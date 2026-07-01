package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.input.LanternaInputHandler;
import ca.ninja.thenix.textrogue.render.LanternaRenderer;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Screen screen = new DefaultTerminalFactory().createScreen();
        screen.startScreen();
        LanternaInputHandler input = new LanternaInputHandler(screen);
        LanternaRenderer renderer = new LanternaRenderer(screen);
        try {
            new Game(input, renderer).start();
        } finally {
            renderer.close();
        }
    }
}
