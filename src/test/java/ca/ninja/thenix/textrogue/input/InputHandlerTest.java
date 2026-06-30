package ca.ninja.thenix.textrogue.input;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;

class InputHandlerTest {
    private InputHandler handlerFor(String input) {
        return new InputHandler(new ByteArrayInputStream(input.getBytes()));
    }

    @Test
    void wReturnsMoveUp() {
        assertEquals(Command.MOVE_UP, handlerFor("w\n").readCommand());
    }

    @Test
    void uppercaseWReturnsMoveUp() {
        assertEquals(Command.MOVE_UP, handlerFor("W\n").readCommand());
    }

    @Test
    void sReturnsMoveDown() {
        assertEquals(Command.MOVE_DOWN, handlerFor("s\n").readCommand());
    }

    @Test
    void aReturnsMoveLeft() {
        assertEquals(Command.MOVE_LEFT, handlerFor("a\n").readCommand());
    }

    @Test
    void dReturnsMoveRight() {
        assertEquals(Command.MOVE_RIGHT, handlerFor("d\n").readCommand());
    }

    @Test
    void qReturnsQuit() {
        assertEquals(Command.QUIT, handlerFor("q\n").readCommand());
    }

    @Test
    void unknownCharReturnsUnknown() {
        assertEquals(Command.UNKNOWN, handlerFor("x\n").readCommand());
    }

    @Test
    void emptyLineReturnsUnknown() {
        assertEquals(Command.UNKNOWN, handlerFor("\n").readCommand());
    }
}
