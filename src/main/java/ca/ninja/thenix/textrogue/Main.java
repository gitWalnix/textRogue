package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.input.Command;
import ca.ninja.thenix.textrogue.input.InputHandler;
import ca.ninja.thenix.textrogue.render.Renderer;

public class Main {
    public static void main(String[] args) {
        InputHandler input = () -> Command.UNKNOWN;
        Renderer renderer = new Renderer(System.out);
        new Game(input, renderer).start();
    }
}
