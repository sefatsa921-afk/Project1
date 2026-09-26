import java.util.List;

/**
 * ============================================================
 *  FUTOSHIKI PUZZLE GENERATOR - SPLIT BY TEAM MEMBER
 * ============================================================
 * FILE     : SolutionCounter.java
 * OWNER    : Lisa (solver & hint engine)
 * JOB      : the backtracking heart - counts how many solutions a
 *            partially-filled puzzle has, stopping at 'cap'.
 *            cap = 2 and a result of 1 proves the puzzle is
 *            uniquely solvable (the uniqueness gate).
 * DEPENDS  : Inequality, Validator (Jack), BoardFactory (Siya)
 * USED BY  : PuzzleGenerator (Karabo), GeneratorTests (Zwivhuya)
 */
public class SolutionCounter {

    /**
     * How many solutions does this partially-filled puzzle have?
     * Stops as soon as 'cap' solutions are found - for uniqueness
     * checks we use cap = 2 (finding a second already proves the
     * puzzle is ambiguous, so no need to keep counting).
     *
     * The caller's board is never modified (we work on a copy).
     */
    public static int countSolutions(int[][] puzzle,
                                     List<Inequality> inequalities,
                                     int cap) {
        int[] count = {0};                  // shared box for the answer
        int[][] work = BoardFactory.deepCopy(puzzle); // protect the caller's board
        solve(work, inequalities, count, cap);
        return count[0];
    }

    /**
     * The backtracking heart of the game:
     *   find first empty cell -> try 1..4 -> recurse -> undo on failure.
     * Returns true when the search should STOP (cap solutions found).
     */
    private static boolean solve(int[][] board,
                                 List<Inequality> inequalities,
                                 int[] count, int cap) {
        int n = board.length;                  // works for any board size

        // 1) Find the first empty cell
        int r = -1, c = -1;
        outer:
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == 0) {
                    r = i; c = j;
                    break outer;
                }
            }
        }

        // 2) No empty cells left -> the board is complete
        if (r == -1) {
            // Safety check: a pre-filled board handed straight in must
            // still obey every rule before it counts as a solution.
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (!Validator.isValid(board, i, j, board[i][j], inequalities)) {
                        return false;
                    }
                }
            }
            count[0]++;
            return count[0] >= cap;   // stop early if we have enough
        }

        // 3) Try every digit in the empty cell
        for (int num = 1; num <= n; num++) {
            if (Validator.isValid(board, r, c, num, inequalities)) {
                board[r][c] = num;          // place the guess
                if (solve(board, inequalities, count, cap)) return true;
                board[r][c] = 0;            // undo (backtrack!)
            }
        }

        // 4) Nothing fits here -> dead end, tell the level above us
        return false;
    }
}
