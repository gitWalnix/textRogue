# TextRogue

A terminal-based roguelike built in Java, using [Lanterna](https://github.com/mabe02/lanterna) for the terminal UI.

## Requirements

- Java 21
- Maven

## Running

```
mvn compile exec:java
```

This uses the `exec-maven-plugin` configured in `pom.xml`, which runs the main class `ca.ninja.thenix.textrogue.Main`.

Since the game uses Lanterna for its terminal UI, run it in a real, interactive terminal window (not piped or backgrounded).
