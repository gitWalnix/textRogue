package ca.ninja.thenix.textrogue.input;

@FunctionalInterface
public interface InputHandler {
    Command pollCommand();
}
