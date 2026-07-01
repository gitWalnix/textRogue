package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.input.LanternaInputHandler;
import ca.ninja.thenix.textrogue.render.LanternaRenderer;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        // Lanterna 3.1.x has no working native Windows console terminal (the
        // WindowsTerminal class it reflects for doesn't exist in the jar), so its
        // headless path falls back to CygwinTerminal, which requires stty.exe and
        // fails on stock Windows. Force the Swing terminal emulator instead.
        Screen screen = new DefaultTerminalFactory().setPreferTerminalEmulator(true).createScreen();
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
