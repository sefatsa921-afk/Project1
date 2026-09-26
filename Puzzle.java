import java.util.List;

/**
 * ============================================================
 *  FUTOSHIKI PUZZLE GENERATOR - SPLIT BY TEAM MEMBER
 * ============================================================
 * FILE     : Puzzle.java
 * OWNER    : Siya (data model)
 * JOB      : the data container - everything that describes ONE
 *            puzzle. Top-level version of the old nested class
 *            PuzzleGenerator.Puzzle.
 * DEPENDS  : Inequality
 * USED BY  : PuzzleGenerator (Karabo), the game GUI, tests
 */
public class Puzzle {

    public final int n;                          // board size of THIS puzzle
    public final int[][] solution;               // hidden fully-solved board
    public final int[][] puzzle;                 // what the player sees (0 = empty)
    public final boolean[][] givens;             // true = pre-filled, not editable
    public final List<Inequality> inequalities;  // the < / > rules

    public Puzzle(int n, int[][] solution, int[][] puzzle,
                  boolean[][] givens, List<Inequality> inequalities) {
        this.n = n;
        this.solution = solution;
        this.puzzle = puzzle;
        this.givens = givens;
        this.inequalities = inequalities;
    }
}
