package ca.ninja.thenix.textrogue.render;

import ca.ninja.thenix.textrogue.entity.Enemy;
import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.entity.Projectile;
import ca.ninja.thenix.textrogue.world.Dungeon;
import ca.ninja.thenix.textrogue.world.Position;
import ca.ninja.thenix.textrogue.world.Tile;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class LanternaRenderer implements Renderer {
    private final Screen screen;

    public LanternaRenderer(Screen screen) {
        this.screen = screen;
    }

    @Override
    public void render(Dungeon dungeon, Player player, List<Enemy> enemies, List<Projectile> projectiles, Set<Position> visibleTiles) {
        screen.clear();
        TextGraphics g = screen.newTextGraphics();
        for (int y = 0; y < Dungeon.HEIGHT; y++) {
            for (int x = 0; x < Dungeon.WIDTH; x++) {
                Position pos = new Position(x, y);
                if (visibleTiles.contains(pos)) {
                    Tile tile = dungeon.getTile(x, y);
                    g.setForegroundColor(colorFor(tile));
                    g.putString(x, y, String.valueOf(tile.toChar()));
                } else if (dungeon.isExplored(x, y)) {
                    Tile tile = dungeon.getTile(x, y);
                    g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
                    g.putString(x, y, String.valueOf(tile.toChar()));
                } else {
                    g.putString(x, y, " ");
                }
            }
        }
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;
            if (!visibleTiles.contains(new Position(enemy.getX(), enemy.getY()))) continue;
            g.setForegroundColor(TextColor.ANSI.RED);
            g.putString(enemy.getX(), enemy.getY(), "e");
        }
        for (Projectile projectile : projectiles) {
            if (!visibleTiles.contains(new Position(projectile.getX(), projectile.getY()))) continue;
            g.setForegroundColor(TextColor.ANSI.YELLOW);
            g.putString(projectile.getX(), projectile.getY(), "*");
        }
        g.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
        g.putString(player.getX(), player.getY(), String.valueOf(player.getFacing().toChar()));
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(0, Dungeon.HEIGHT, "Floor: " + player.getFloor() + "   HP: " + player.getHp() + "/" + player.getMaxHp());
        try {
            screen.refresh();
        } catch (IOException ignored) {}
    }

    @Override
    public void renderDeath(Player player) {
        screen.clear();
        TextGraphics g = screen.newTextGraphics();
        g.setForegroundColor(TextColor.ANSI.RED);
        String message = "You died";
        int x = Math.max(0, (Dungeon.WIDTH - message.length()) / 2);
        int y = Dungeon.HEIGHT / 2;
        g.putString(x, y, message);
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
