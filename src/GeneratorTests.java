import java.util.List;

/**
 * ============================================================
 *  FUTOSHIKI PUZZLE GENERATOR - SPLIT BY TEAM MEMBER
 * ============================================================
 * FILE     : GeneratorTests.java
 * OWNER    : Zwivhuya(me) (unit testing & QA)
 * JOB      : proves the finished pipeline is healthy. For every
 *            generated puzzle it checks:
 *              1. exactly ONE solution exists (uniqueness gate held)
 *              2. every inequality is TRUE against the hidden solution
 *              3. every given cell matches the hidden solution
 * RUN      : java GeneratorTests
 */
public class GeneratorTests {
    public static void main(String[] args) {
        PuzzleGenerator gen = new PuzzleGenerator();
        String[] diffs = {"Easy", "Medium", "Hard"};
        int perDiff = 50;
        int total = 0, ok = 0;

        for (String d : diffs) {
            for (int i = 0; i < perDiff; i++) {
                Puzzle p = gen.generate(d);
                total++;

                // 1. uniqueness: re-count independently, cap at 2
                boolean unique =
                        SolutionCounter.countSolutions(p.puzzle, p.inequalities, 2) == 1;

                // 2. every sign must be true against the solution
                boolean signsTruthful = true;
                for (Inequality ineq : p.inequalities) {
                    if (!ineq.isSatisfied(p.solution)) { signsTruthful = false; break; }
                }

                // 3. givens must match the solution
                boolean givensOk = true;
                for (int r = 0; r < p.n; r++) {
                    for (int c = 0; c < p.n; c++) {
                        if (p.givens[r][c] && p.puzzle[r][c] != p.solution[r][c]) {
                            givensOk = false;
                        }
                    }
                }

                if (unique && signsTruthful && givensOk) {
                    ok++;
                } else {
                    System.out.println("FAIL [" + d + " #" + i + "] unique=" + unique
                            + " signsTruthful=" + signsTruthful + " givensOk=" + givensOk);
                }
            }
        }

        // also check the hand-made fallback
        Puzzle fb = PuzzleGenerator.createFallback();
        boolean fbUnique = SolutionCounter.countSolutions(fb.puzzle, fb.inequalities, 2) == 1;
        System.out.println("fallback puzzle unique: " + fbUnique);

        System.out.println("====================================");
        System.out.println(ok + "/" + total + " generated puzzles passed all 3 checks");
        System.out.println((ok == total && fbUnique) ? "RESULT: PUZZLEGENERATOR STATUS ACHIEVED"
                                                     : "RESULT: NOT YET");
    }
}
