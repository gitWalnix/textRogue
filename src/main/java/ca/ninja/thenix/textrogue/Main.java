package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.input.Command;
import ca.ninja.thenix.textrogue.input.InputHandler;
import ca.ninja.thenix.textrogue.render.Renderer;
import ca.ninja.thenix.textrogue.entity.Player;
import ca.ninja.thenix.textrogue.world.Dungeon;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        InputHandler input = () -> Command.UNKNOWN;
        Renderer renderer = new Renderer() {
            @Override
            public void render(Dungeon dungeon, Player player) {}
            @Override
            public void close() throws IOException {}
        };
        new Game(input, renderer).start();
    }
}
