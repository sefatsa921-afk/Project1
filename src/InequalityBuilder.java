import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ============================================================
 *  FUTOSHIKI PUZZLE GENERATOR - SPLIT BY TEAM MEMBER
 * ============================================================
 * FILE     : InequalityBuilder.java
 * OWNER    : Jack (validation & rule checking)
 * JOB      : sprinkles the < / > signs over a solved board. The
 *            direction of every sign is read from the REAL numbers
 *            in the solution - a sign always tells the truth.
 * DEPENDS  : Inequality
 * USED BY  : PuzzleGenerator (Karabo)
 */
public class InequalityBuilder {

    private final Random rng = new Random();

    /**
     * Sprinkles < / > signs over the board.
     * dens = probability that an adjacent pair gets a sign.
     * Easy uses a high dens (lots of help), Hard a low one.
     */
    public List<Inequality> createInequalities(int[][] solution,
                                               double dens,
                                               boolean allowDiagonal) {
        int n = solution.length;               // works for any board size
        List<Inequality> list = new ArrayList<>();

        // Horizontal neighbours: (r,c) and (r,c+1)
        for (int r = 0; r < n; r++) {
            for (int c = 0; c + 1 < n; c++) {
                if (rng.nextDouble() < dens) {
                    list.add(signBetween(solution, r, c, r, c + 1, false));
                }
            }
        }

        // Vertical neighbours: (r,c) and (r+1,c)
        for (int r = 0; r + 1 < n; r++) {
            for (int c = 0; c < n; c++) {
                if (rng.nextDouble() < dens) {
                    list.add(signBetween(solution, r, c, r + 1, c, false));
                }
            }
        }

        // Medium/Hard: also drop ONE diagonal sign somewhere.
        // Careful: diagonal neighbours CAN hold equal numbers (only rows
        // and columns are constrained), so we must find a pair that differs.
        if (allowDiagonal) {
            for (int tries = 0; tries < 20; tries++) {
                int r = rng.nextInt(n - 1);   // top-left corner of a 2x2 block
                int c = rng.nextInt(n - 1);
                boolean mainDiag = rng.nextBoolean();
                int r1 = r, c1 = mainDiag ? c : c + 1;
                int r2 = r + 1, c2 = mainDiag ? c + 1 : c;
                if (solution[r1][c1] != solution[r2][c2]) {
                    list.add(signBetween(solution, r1, c1, r2, c2, true));
                    break; // found a pair that can honestly be ordered
                }
            }
        }

        return list;
    }

    /** Builds the sign that is true for these two cells in the solution. */
    private Inequality signBetween(int[][] solution, int r1, int c1,
                                   int r2, int c2, boolean diagonal) {
        char dir = solution[r1][c1] < solution[r2][c2] ? '<' : '>';
        return new Inequality(r1, c1, r2, c2, dir, diagonal);
    }
}
