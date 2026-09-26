import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * ============================================================
 *  FUTOSHIKI PUZZLE GENERATOR - SPLIT BY TEAM MEMBER
 * ============================================================
 * FILE     : BoardFactory.java
 * OWNER    : Siya (data model)
 * JOB      : creates the random valid boards (Latin squares) and
 *            the deep-copy helper the whole pipeline uses.
 * DEPENDS  : nothing (java.util only)
 * USED BY  : PuzzleGenerator (Karabo), SolutionCounter (Lisa)
 */
public class BoardFactory {

    public static final int N = 4;   // classic board size: 4x4, digits 1..4
    private final Random rng = new Random();

    // ---- generateLatinSquare() --------------------------------

    /**
     * Builds a random valid 4x4 Latin square: every row and every
     * column contains the numbers 1..4 exactly once.
     *
     * Strategy: start from a pattern that is PROVABLY Latin, then
     * scramble it with operations that can never break Latin-ness:
     *   shuffle rows -> shuffle columns -> rename the digits.
     */
    /** Classic 4x4 version - keeps existing callers and tests unchanged. */
    public int[][] generateLatinSquare() {
        return generateLatinSquare(N);
    }

    /** Builds a random valid n x n Latin square (any size we support). */
    public int[][] generateLatinSquare(int n) {
        int[][] grid = new int[n][n];

        // 1) Cyclic base pattern (provably Latin for any n)
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                grid[r][c] = (r + c) % n + 1;
            }
        }

        // 2) Shuffle the rows (whole rows swap places)
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < n; i++) rows.add(i);
        Collections.shuffle(rows, rng);
        int[][] next = new int[n][n];
        for (int r = 0; r < n; r++) {
            System.arraycopy(grid[rows.get(r)], 0, next[r], 0, n);
        }
        grid = next;

        // 3) Shuffle the columns
        List<Integer> cols = new ArrayList<>();
        for (int i = 0; i < n; i++) cols.add(i);
        Collections.shuffle(cols, rng);
        next = new int[n][n];
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                next[r][c] = grid[r][cols.get(c)];
            }
        }
        grid = next;

        // 4) Rename the symbols, e.g. 1->3, 2->1, 3->4, 4->2
        List<Integer> symbols = new ArrayList<>();
        for (int i = 1; i <= n; i++) symbols.add(i);
        Collections.shuffle(symbols, rng);
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                grid[r][c] = symbols.get(grid[r][c] - 1);
            }
        }

        return grid;
    }

    // ---- deepCopy() --------------------------------------------

    /**
     * Independent copy of a 2-D array.
     * In Java, "copy = board" would share the SAME array - editing
     * the copy would corrupt the original. Always deep-copy boards!
     */
    public static int[][] deepCopy(int[][] src) {
        int[][] copy = new int[src.length][];
        for (int r = 0; r < src.length; r++) {
            copy[r] = src[r].clone();
        }
        return copy;
    }
}
