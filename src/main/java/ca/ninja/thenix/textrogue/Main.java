package ca.ninja.thenix.textrogue;

import ca.ninja.thenix.textrogue.input.InputHandler;
import ca.ninja.thenix.textrogue.render.Renderer;

public class Main {
    public static void main(String[] args) {
        InputHandler input = new InputHandler(System.in);
        Renderer renderer = new Renderer(System.out);
        new Game(input, renderer).start();
    }
}
