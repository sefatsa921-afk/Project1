import java.util.List;

/**
 * ============================================================
 *  FUTOSHIKI PUZZLE GENERATOR - SPLIT BY TEAM MEMBER
 * ============================================================
 * FILE     : Validator.java
 * OWNER    : Jack (validation & rule checking)
 * JOB      : the single source of truth for "is this placement
 *            legal?" Used by the solution counter (Lisa), the
 *            fallback checks and the QA tests (Zwivhuya).
 * DEPENDS  : Inequality
 */
public class Validator {

    /**
     * Is it legal to put num into cell (r, c)?
     *   1. num must not already be in row r
     *   2. num must not already be in column c
     *   3. every inequality touching (r,c) must hold, assuming num is there
     *
     * Both the solver and the GUI call this exact method - that is
     * why it is static and changes nothing.
     */
    public static boolean isValid(int[][] board, int r, int c, int num,
                                  List<Inequality> inequalities) {
        int n = board.length;                  // works for any board size
        // 1 + 2: scan the row and the column for duplicates
        for (int i = 0; i < n; i++) {
            if (i != c && board[r][i] == num) return false; // dup in row
            if (i != r && board[i][c] == num) return false; // dup in column
        }

        // 3: every rule touching this cell must hold
        for (Inequality ineq : inequalities) {
            boolean touchesFirst  = ineq.getR1() == r && ineq.getC1() == c;
            boolean touchesSecond = ineq.getR2() == r && ineq.getC2() == c;
            if (!touchesFirst && !touchesSecond) continue; // rule not involved

            int a = touchesFirst  ? num : board[ineq.getR1()][ineq.getC1()];
            int b = touchesSecond ? num : board[ineq.getR2()][ineq.getC2()];

            if (a == 0 || b == 0) continue; // empty side can't violate yet

            if (ineq.getDir() == '<') {
                if (!(a < b)) return false;
            } else {
                if (!(a > b)) return false;
            }
        }

        return true;
    }
}
