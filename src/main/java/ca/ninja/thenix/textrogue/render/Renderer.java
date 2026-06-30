package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;

import java.io.PrintStream;

public class Renderer {
    private static final String CLEAR = "[2J[H";
    private static final String RESET = "[0m";
    private static final String DARK_GRAY = "[90m";
    private static final String WHITE = "[37m";
    private static final String YELLOW = "[33m";
    private static final String BRIGHT_GREEN = "[92m";

    private final PrintStream out;

    public Renderer(PrintStream out) {
        this.out = out;
    }

    public void render(Dungeon dungeon, Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append(CLEAR);

        for (int y = 0; y < Dungeon.HEIGHT; y++) {
            for (int x = 0; x < Dungeon.WIDTH; x++) {
                if (x == player.getX() && y == player.getY()) {
                    sb.append(BRIGHT_GREEN).append('@').append(RESET);
                } else {
                    Tile tile = dungeon.getTile(x, y);
                    sb.append(colorFor(tile)).append(tile.toChar()).append(RESET);
                }
            }
            sb.append('\n');
        }

        sb.append("Floor: ").append(player.getFloor()).append('\n');
        out.print(sb);
    }

    private String colorFor(Tile tile) {
        return switch (tile) {
            case WALL -> DARK_GRAY;
            case FLOOR -> WHITE;
            case STAIRS_DOWN -> YELLOW;
        };
    }
}
