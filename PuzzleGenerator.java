import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ============================================================
 *  FUTOSHIKI PUZZLE GENERATOR - THE ORCHESTRATOR
 * ============================================================
 * FILE     : PuzzleGenerator.java
 * OWNER    : Karabo (UI & game lifecycle)
 * JOB      : runs the whole pipeline in chronological order and
 *            returns one finished, uniquely solvable Puzzle:
 *
 *              1) BoardFactory       -> random Latin square     [Siya]
 *              2) InequalityBuilder  -> truthful < / > signs    [Jack]
 *              3) hide the cells     -> givens + player grid
 *              4) SolutionCounter    -> uniqueness gate         [Lisa]
 *            plus the difficulty table and the emergency fallback.
 *
 * DEPENDS  : Puzzle, BoardFactory, InequalityBuilder, SolutionCounter
 * USED BY  : FutoshikiGame (the GUI calls generate(...))
 */
public class PuzzleGenerator {

    public static final int N = BoardFactory.N; // 4, forwarded so old callers keep working
    private final Random rng = new Random();

    private final BoardFactory boardFactory = new BoardFactory();                  // [Siya]
    private final InequalityBuilder inequalityBuilder = new InequalityBuilder();   // [Jack]
    // [Lisa]'s SolutionCounter is static - called directly below.

    // ---- generate(String difficulty) ---------------------------

    /**
     * Main public method: build a complete, uniquely solvable puzzle.
     *
     * dens     = probability that an adjacent pair gets a sign
     * keepRate = probability that a cell stays visible as a given
     *
     * We try up to 100 random candidates; each one is accepted only
     * if the solver proves it has EXACTLY ONE solution.
     */
    /** Classic game: always 4x4 (submission-safe default). */
    public Puzzle generate(String difficulty) {
        return generate(difficulty, N);
    }

    /**
     * Size-aware generator (Phase 1 of the scaling project).
     * Same clue ratios at every size; bigger boards simply get more cells.
     * Bigger boards are allowed more attempts, since uniqueness gets rarer
     * as the grid grows.
     */
    public Puzzle generate(String difficulty, int n) {
        double dens;
        double keepRate;
        boolean allowDiagonal;

        switch (difficulty == null ? "" : difficulty) {
            case "Easy":
                dens = 0.70; keepRate = 0.50; allowDiagonal = false;
                break;
            case "Medium":
                dens = 0.50; keepRate = 0.31; allowDiagonal = true;
                break;
            default: // "Hard"
                dens = 0.35; keepRate = 0.19; allowDiagonal = true;
                break;
        }

        int maxAttempts = (n == N) ? 100 : 300;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // 1) a fresh random solution of the requested size        [Siya]
            int[][] solution = boardFactory.generateLatinSquare(n);

            // 2) truthful signs derived from it                       [Jack]
            List<Inequality> inequalities =
                    inequalityBuilder.createInequalities(solution, dens, allowDiagonal);

            // 3) hide most cells, keep some as givens
            int[][] puzzle = BoardFactory.deepCopy(solution);
            boolean[][] givens = new boolean[n][n];
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    if (rng.nextDouble() < keepRate) {
                        givens[r][c] = true;   // stays visible, not editable
                    } else {
                        puzzle[r][c] = 0;      // hidden from the player
                    }
                }
            }

            // 4) the uniqueness gate: exactly one solution or retry   [Lisa]
            if (SolutionCounter.countSolutions(puzzle, inequalities, 2) == 1) {
                return new Puzzle(n, solution, puzzle, givens, inequalities);
            }
        }

        // Emergency spares: classic size gets the hand-made fallback;
        // extended sizes fall back to a fully-revealed board (always unique).
        if (n == N) return createFallback();
        int[][] solution = boardFactory.generateLatinSquare(n);
        boolean[][] allGivens = new boolean[n][n];
        for (int r = 0; r < n; r++) {
            java.util.Arrays.fill(allGivens[r], true);
        }
        return new Puzzle(n, solution, BoardFactory.deepCopy(solution), allGivens,
                inequalityBuilder.createInequalities(solution, 0.5, true));
    }

    // ---- createFallback() ---------------------------------------

    /**
     * A known unique puzzle, hard-coded so the game can never crash.
     * Its solution is forced by row/column logic alone, so it stays
     * unique even if the signs were removed.
     *
     * Solution:        Givens shown:
     *   1 2 3 4          1 2 . 4
     *   3 4 1 2          . 4 . 2
     *   2 1 4 3          2 . . 3
     *   4 3 2 1          . . 2 .
     */
    public static Puzzle createFallback() {
        int[][] solution = {
            {1, 2, 3, 4},
            {3, 4, 1, 2},
            {2, 1, 4, 3},
            {4, 3, 2, 1}
        };
        boolean[][] givens = {
            {true,  true,  false, true},
            {false, true,  false, true},
            {true,  false, false, true},
            {false, false, true,  false}
        };
        int[][] puzzle = BoardFactory.deepCopy(solution);
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (!givens[r][c]) puzzle[r][c] = 0;
            }
        }
        List<Inequality> inequalities = new ArrayList<>();
        inequalities.add(new Inequality(0, 1, 0, 2, '<')); // 2 < 3
        inequalities.add(new Inequality(1, 0, 1, 1, '<')); // 3 < 4
        inequalities.add(new Inequality(2, 1, 2, 2, '<')); // 1 < 4
        inequalities.add(new Inequality(3, 0, 3, 1, '>')); // 4 > 3
        inequalities.add(new Inequality(0, 0, 1, 0, '<')); // 1 < 3
        inequalities.add(new Inequality(2, 3, 3, 3, '>')); // 3 > 1
        return new Puzzle(N, solution, puzzle, givens, inequalities);
    }
}
