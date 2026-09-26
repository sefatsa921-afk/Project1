import java.lang.reflect.*;

/**
 * ============================================================
 *  FUTOSHIKI - INTEGRATION TEST (NO DISPLAY NEEDED)
 * ============================================================
 * FILE     : GameLogicTest.java
 * OWNER    : Zwivhuya(me) (unit testing & QA)
 * JOB      : runs the REAL game methods on a puzzle produced by the
 *            REAL generator - without opening a window, so it runs
 *            on any machine (even a server with no display).
 *            Checks:
 *              1. the QA tests pass            (runStringTests etc.)
 *              2. checkWinCondition accepts the hidden solution  [Jack]
 *              3. the in-game solver completes a fresh puzzle and
 *                 finds the exact unique solution               [Lisa]
 *              4. every non-diagonal sign is found by the
 *                 horizontal/vertical lookups                   [Karabo]
 *
 * HOW      : creates a FutoshikiGame instance WITHOUT running its
 *            constructor (which needs a display) via sun.misc.Unsafe -
 *            a test-only trick, never used by the game itself.
 * RUN      : java GameLogicTest
 */
public class GameLogicTest {
    public static void main(String[] args) throws Exception {
        // create the instance WITHOUT running the constructor (skips the display)
        Class<?> u = Class.forName("sun.misc.Unsafe");
        Field uf = u.getDeclaredField("theUnsafe");
        uf.setAccessible(true);
        Object unsafe = uf.get(null);
        Method alloc = u.getMethod("allocateInstance", Class.class);
        FutoshikiGame g = (FutoshikiGame) alloc.invoke(unsafe, FutoshikiGame.class);

        // minimal state: a real generated puzzle
        Field sizeF = FutoshikiGame.class.getDeclaredField("size");   sizeF.setAccessible(true); sizeF.set(g, 4);
        Field boardF = FutoshikiGame.class.getDeclaredField("board"); boardF.setAccessible(true);
        Field consF = FutoshikiGame.class.getDeclaredField("constraints"); consF.setAccessible(true);
        PuzzleGenerator gen = new PuzzleGenerator();
        Puzzle p = gen.generate("Hard");
        boardF.set(g, BoardFactory.deepCopy(p.puzzle));
        consF.set(g, p.inequalities);

        // 1) the QA tests (their code, their slots)
        Method m1 = FutoshikiGame.class.getDeclaredMethod("runStringTests");       m1.setAccessible(true); m1.invoke(g);
        Method m2 = FutoshikiGame.class.getDeclaredMethod("runInequalityTests");   m2.setAccessible(true); m2.invoke(g);

        // 2) win check: the full solution must pass
        boardF.set(g, p.solution);
        Method cw = FutoshikiGame.class.getDeclaredMethod("checkWinCondition");    cw.setAccessible(true);
        boolean win = (Boolean) cw.invoke(g);
        System.out.println("[logic] checkWinCondition(solution) = " + win + (win ? "  (correct)" : "  (WRONG)"));

        // 3) solver: the in-game backtracking must complete a fresh generated puzzle
        int[][] scratch = BoardFactory.deepCopy(p.puzzle);
        Method sl = FutoshikiGame.class.getDeclaredMethod("solve", int[][].class, int.class, int.class);
        sl.setAccessible(true);
        boolean solved = (Boolean) sl.invoke(g, scratch, 0, 0);
        boolean matches = true;
        for (int r = 0; r < 4 && matches; r++)
            for (int c = 0; c < 4 && matches; c++)
                matches = scratch[r][c] == p.solution[r][c];
        System.out.println("[logic] solve() completes puzzle = " + solved
                + ", result matches hidden solution = " + matches
                + (solved && matches ? "  (correct)" : "  (WRONG)"));

        // 4) sign lookups must find every non-diagonal constraint
        Method h = FutoshikiGame.class.getDeclaredMethod("getHorizontalConstraintSymbol", int.class, int.class); h.setAccessible(true);
        Method v = FutoshikiGame.class.getDeclaredMethod("getVerticalConstraintSymbol", int.class, int.class);   v.setAccessible(true);
        int found = 0, checked = 0;
        for (Inequality ineq : p.inequalities) {
            if (ineq.isDiagonal()) continue;
            checked++;
            String s = (ineq.getR1() == ineq.getR2())
                    ? (String) h.invoke(g, ineq.getR1(), Math.min(ineq.getC1(), ineq.getC2()))
                    : (String) v.invoke(g, ineq.getR1(), ineq.getC1());
            if (s == null || s.isEmpty()) System.out.println("[logic] MISSING sign for " + ineq);
            else found++;
        }
        System.out.println("[logic] sign lookups: " + found + "/" + checked + " found");
        System.out.println("[logic] DONE");
        System.exit(0);
    }
}
