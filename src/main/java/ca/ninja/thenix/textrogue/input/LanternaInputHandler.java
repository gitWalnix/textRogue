package ca.ninja.thenix.textrogue.input;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import java.io.IOException;

public class LanternaInputHandler implements InputHandler {
    private final Screen screen;

    public LanternaInputHandler(Screen screen) {
        this.screen = screen;
    }

    @Override
    public Command pollCommand() {
        try {
            KeyStroke key = screen.pollInput();
            if (key == null) return Command.UNKNOWN;
            return switch (key.getKeyType()) {
                case Character -> {
                    char c = Character.toLowerCase(key.getCharacter());
                    yield switch (c) {
                        case 'w' -> Command.MOVE_UP;
                        case 's' -> Command.MOVE_DOWN;
                        case 'a' -> Command.MOVE_LEFT;
                        case 'd' -> Command.MOVE_RIGHT;
                        case 'q' -> Command.QUIT;
                        default  -> Command.UNKNOWN;
                    };
                }
                case ArrowUp    -> Command.MOVE_UP;
                case ArrowDown  -> Command.MOVE_DOWN;
                case ArrowLeft  -> Command.MOVE_LEFT;
                case ArrowRight -> Command.MOVE_RIGHT;
                case EOF, Escape -> Command.QUIT;
                default -> Command.UNKNOWN;
            };
        } catch (IOException e) {
            return Command.QUIT;
        }
    }
}
