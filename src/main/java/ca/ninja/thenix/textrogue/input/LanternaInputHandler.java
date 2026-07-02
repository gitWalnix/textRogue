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
            // Key-repeat while a key is held down can queue many keystrokes per
            // tick; drain the whole queue and keep only the latest command so
            // the backlog never outlives the key press.
            Command result = Command.UNKNOWN;
            KeyStroke key;
            while ((key = screen.pollInput()) != null) {
                Command cmd = toCommand(key);
                if (cmd == Command.QUIT) return Command.QUIT;
                if (cmd != Command.UNKNOWN) result = cmd;
            }
            return result;
        } catch (IOException e) {
            return Command.QUIT;
        }
    }

    private Command toCommand(KeyStroke key) {
        return switch (key.getKeyType()) {
            case Character -> {
                char c = Character.toLowerCase(key.getCharacter());
                yield switch (c) {
                    case 'w' -> Command.MOVE_UP;
                    case 's' -> Command.MOVE_DOWN;
                    case 'a' -> Command.MOVE_LEFT;
                    case 'd' -> Command.MOVE_RIGHT;
                    case 'e' -> Command.DRINK_POTION;
                    case 'r' -> Command.RESTART;
                    case ' ' -> Command.SHOOT;
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
    }
}
