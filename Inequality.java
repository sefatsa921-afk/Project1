
public class Inequality {

    // Fields 
    private final int r1, c1;        // first cell  (row, column)
    private final int r2, c2;        // second cell (row, column)
    private final char dir;          // '<' means cell1 < cell2, '>' means cell1 > cell2
    private final boolean diagonal;  // true if the sign connects diagonal neighbours

    //Constructors 
    public Inequality(int r1, int c1, int r2, int c2, char dir) {
        this(r1, c1, r2, c2, dir, false);
    }

    /** Full constructor WITH diagonal flag. */
    public Inequality(int r1, int c1, int r2, int c2, char dir, boolean diagonal) {
        this.r1 = r1;
        this.c1 = c1;
        this.r2 = r2;
        this.c2 = c2;
        this.dir = dir;
        this.diagonal = diagonal;
    }

    // ---- Getters (the GUI needs these to draw the signs) ----

    public int getR1() { return r1; }
    public int getC1() { return c1; }
    public int getR2() { return r2; }
    public int getC2() { return c2; }
    public char getDir() { return dir; }
    public boolean isDiagonal() { return diagonal; }

    // The important method

    /*
     Design decision: 0 means "empty cell". An empty cell cannot
     violate a constraint yet, so we return true until BOTH cells
     contain numbers. The generator and GUI both rely on this.
    */
    public boolean isSatisfied(int[][] board) {
        int a = board[r1][c1];
        int b = board[r2][c2];

        if (a == 0 || b == 0) {
            return true;   // nothing to violate while a cell is empty
        }
        if (dir == '<') {
            return a < b;
        }
        return a > b;      // dir == '>'
    }

    // debugging

    @Override
    public String toString() {
        String kind = diagonal ? " (diagonal)" : "";
        return "(" + r1 + "," + c1 + ") " + dir + " (" + r2 + "," + c2 + ")" + kind;
    }
}
