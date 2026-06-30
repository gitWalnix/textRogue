package ca.ninja.thenix.textrogue.input;

import java.io.InputStream;
import java.util.Scanner;

public class InputHandler {
    private final Scanner scanner;

    public InputHandler(InputStream in) {
        this.scanner = new Scanner(in);
    }

    public Command readCommand() {
        if (!scanner.hasNextLine()) return Command.QUIT;
        String line = scanner.nextLine().trim().toLowerCase();
        if (line.isEmpty()) return Command.UNKNOWN;
        return switch (line.charAt(0)) {
            case 'w' -> Command.MOVE_UP;
            case 's' -> Command.MOVE_DOWN;
            case 'a' -> Command.MOVE_LEFT;
            case 'd' -> Command.MOVE_RIGHT;
            case 'q' -> Command.QUIT;
            default -> Command.UNKNOWN;
        };
    }
}
