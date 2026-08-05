package ca.ninja.thenix.textrogue.world;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class PathFinder {
    public static final int UNREACHABLE = -1;

    private static final int[] DX = {0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 0};

    public static int[][] distanceField(Dungeon dungeon, int fromX, int fromY) {
        int[][] distances = new int[Dungeon.HEIGHT][Dungeon.WIDTH];
        for (int[] row : distances) Arrays.fill(row, UNREACHABLE);
        distances[fromY][fromX] = 0;

        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{fromX, fromY});

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int cx = current[0];
            int cy = current[1];
            int nextDistance = distances[cy][cx] + 1;
            for (int i = 0; i < DX.length; i++) {
                int nx = cx + DX[i];
                int ny = cy + DY[i];
                if (dungeon.getTile(nx, ny) == Tile.WALL) continue;
                if (distances[ny][nx] != UNREACHABLE) continue;
                distances[ny][nx] = nextDistance;
                queue.add(new int[]{nx, ny});
            }
        }
        return distances;
    }
}
