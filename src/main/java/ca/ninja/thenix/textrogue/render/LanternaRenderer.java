package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Tile;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;
import java.io.IOException;

public class LanternaRenderer implements Renderer {
    private final Screen screen;

    public LanternaRenderer(Screen screen) {
        this.screen = screen;
    }

    @Override
    public void render(Dungeon dungeon, Player player) {
        screen.clear();
        TextGraphics g = screen.newTextGraphics();
        for (int y = 0; y < Dungeon.HEIGHT; y++) {
            for (int x = 0; x < Dungeon.WIDTH; x++) {
                if (x == player.getX() && y == player.getY()) {
                    g.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
                    g.putString(x, y, "@");
                } else {
                    Tile tile = dungeon.getTile(x, y);
                    g.setForegroundColor(colorFor(tile));
                    g.putString(x, y, String.valueOf(tile.toChar()));
                }
            }
        }
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(0, Dungeon.HEIGHT, "Floor: " + player.getFloor());
        try {
            screen.refresh();
        } catch (IOException ignored) {}
    }

    private TextColor colorFor(Tile tile) {
        return switch (tile) {
            case WALL        -> TextColor.ANSI.BLACK_BRIGHT;
            case FLOOR       -> TextColor.ANSI.WHITE;
            case STAIRS_DOWN -> TextColor.ANSI.YELLOW;
        };
    }

    @Override
    public void close() throws IOException {
        screen.stopScreen();
    }
}
