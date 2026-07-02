package ca.ninja.thenix.textrogue.input;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class LanternaInputHandlerTest {

    private LanternaInputHandler handlerWithKey(KeyStroke key) throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(80, 25));
        terminal.addInput(key);
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.startScreen();
        return new LanternaInputHandler(screen);
    }

    private LanternaInputHandler handlerWithNoKey() throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(80, 25));
        TerminalScreen screen = new TerminalScreen(terminal);
        screen.startScreen();
        return new LanternaInputHandler(screen);
    }

    @Test
    void wKeyReturnsMoveUp() throws IOException {
        assertEquals(Command.MOVE_UP, handlerWithKey(new KeyStroke('w', false, false)).pollCommand());
    }

    @Test
    void sKeyReturnsMoveDown() throws IOException {
        assertEquals(Command.MOVE_DOWN, handlerWithKey(new KeyStroke('s', false, false)).pollCommand());
    }

    @Test
    void aKeyReturnsMoveLeft() throws IOException {
        assertEquals(Command.MOVE_LEFT, handlerWithKey(new KeyStroke('a', false, false)).pollCommand());
    }

    @Test
    void dKeyReturnsMoveRight() throws IOException {
        assertEquals(Command.MOVE_RIGHT, handlerWithKey(new KeyStroke('d', false, false)).pollCommand());
    }

    @Test
    void qKeyReturnsUnknown() throws IOException {
        assertEquals(Command.UNKNOWN, handlerWithKey(new KeyStroke('q', false, false)).pollCommand());
    }

    @Test
    void escapeKeyReturnsQuit() throws IOException {
        assertEquals(Command.QUIT, handlerWithKey(new KeyStroke(KeyType.Escape)).pollCommand());
    }

    @Test
    void eKeyReturnsDrinkPotion() throws IOException {
        assertEquals(Command.DRINK_POTION, handlerWithKey(new KeyStroke('e', false, false)).pollCommand());
    }

    @Test
    void arrowUpReturnsMoveUp() throws IOException {
        assertEquals(Command.MOVE_UP, handlerWithKey(new KeyStroke(KeyType.ArrowUp)).pollCommand());
    }

    @Test
    void unknownKeyReturnsUnknown() throws IOException {
        assertEquals(Command.UNKNOWN, handlerWithKey(new KeyStroke('x', false, false)).pollCommand());
    }

    @Test
    void noPendingKeyReturnsUnknown() throws IOException {
        assertEquals(Command.UNKNOWN, handlerWithNoKey().pollCommand());
    }

    @Test
    void spaceKeyReturnsShoot() throws IOException {
        assertEquals(Command.SHOOT, handlerWithKey(new KeyStroke(' ', false, false)).pollCommand());
    }

    @Test
    void rKeyReturnsRestart() throws IOException {
        assertEquals(Command.RESTART, handlerWithKey(new KeyStroke('r', false, false)).pollCommand());
    }
}
