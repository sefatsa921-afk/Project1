import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/*
 * Futoshiki Game - Group Skeleton
 * Team: Zwivhuya(i), Siya, Lisa, Jack, Karabo
 */
public class FutoshikiGame extends JFrame {

    private int size;

    // DATA MODEL (Siya) - REPLACED with the generator's model
    // SOURCE of Inequality: Inequality.java (repo, compiles as-is).
    private int[][] board;                  // 0 = empty, 1..size = number
    private List<Inequality> constraints;   // the < > signs

    // SOURCE: LifeManager.java (repo). Call sites still BLANK in submitBoard.
    private LifeManager lives = new LifeManager(3);

<<<<<<< HEAD
    // Generator wiring (SOURCE: PuzzleGenerator.java - now in the repo)
    private final PuzzleGenerator generator = new PuzzleGenerator();
    private PuzzleGenerator.Puzzle currentPuzzle;
=======
    // Generator wiring (SOURCE: PuzzleGenerator.java + team split - now in the repo)
    private final PuzzleGenerator generator = new PuzzleGenerator();
    private Puzzle currentPuzzle;
>>>>>>> 6c8543fac4042f77eac03c298de69b528a2a9a1f
    private String difficulty = "Easy";

    // UI controls, created in createUI()
    private JLabel livesLabel;
    private JComboBox<String> difficultyCombo;
    private JComboBox<Integer> sizeCombo;
    private JPanel boardPanel;
    
    private JTextField[][] inputFields;
    private JLabel timerLabel;
    private JLabel scoreLabel;
    
    private Timer gameTimer;
    private int secondsElapsed;
    private int playerPoints;
    private int failCount;

    public FutoshikiGame() {
        // Setup window properties
        setTitle("Futoshiki");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(640, 700);
        setLocationRelativeTo(null);

        // Run Zwivhuya(me)'s unit tests on startup (prints results to console)
        runStringTests();
        runInequalityTests();

        // Run Karabo's board initialization and render UI.
        // Order matters: the board must exist BEFORE the UI renders it.
        setupNewGame(4);
        createUI();
        setVisible(true);
    }

    // DATA MODEL (Siya)
    // NOTE: the original Cell and Constraint nested classes were DELETED and
    // replaced by the int[][] + Inequality model (see fields above).
    // Constraint.isSatisfied() was a stub that always returned true;
    // Inequality.isSatisfied() actually checks the rule.

    // VALIDATION & RULE CHECKING (Jack)

    // Verifies if placing a number breaks row, col, or inequality rules
    // SOURCE: Validation.java (repo). Method body kept verbatim; the repo file
    // was a bare method with no class, so it is wrapped here. The
    // 'inequalities' parameter now uses the game's 'constraints' field, and
    // local 'b' was renamed 'b2' so it does not clash with the board parameter.
    private boolean isValidMove(int[][] board, int r, int c, int num) {
        int n = board.length;

        // 1 + 2: scan the row and the column for duplicates
        for (int i = 0; i < n; i++) {
            if (i != c && board[r][i] == num) return false; // dup in row
            if (i != r && board[i][c] == num) return false; // dup in column
        }

        // 3: every rule touching this cell must hold
        for (Inequality ineq : constraints) {
            boolean touchesFirst  = ineq.getR1() == r && ineq.getC1() == c;
            boolean touchesSecond = ineq.getR2() == r && ineq.getC2() == c;
            if (!touchesFirst && !touchesSecond) continue; // rule not involved

            int a  = touchesFirst  ? num : board[ineq.getR1()][ineq.getC1()];
            int b2 = touchesSecond ? num : board[ineq.getR2()][ineq.getC2()];

            if (a == 0 || b2 == 0) continue; // empty side can't violate yet

            if (ineq.getDir() == '<') {
                if (!(a < b2)) return false;
            } else {
                if (!(a > b2)) return false;
            }
        }

        return true;
    }

    // Checks whether the entire board is correctly filled
    // SOURCE: CheckWin.java (repo). Three-step logic preserved; adapted from
    // the old Constraint model + Cell.getValue() to int[][] + Inequality.
    private boolean checkWinCondition() {
        // Jack: iterate grid and verify all values
        int n = size;
        // 1: checking if the entire board is filled
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (board[r][c] == 0) { // 0 = empty cell
                    return false;
                }
            }
        }
        // 2: checking for duplicates
        for (int i = 0; i < n; i++) {
            boolean[] rowHas = new boolean[n + 1];
            boolean[] colHas = new boolean[n + 1];
            for (int k = 0; k < n; k++) {
                if (rowHas[board[i][k]] || colHas[board[k][i]]) {
                    return false;
                }
                rowHas[board[i][k]] = true;
                colHas[board[k][i]] = true;
            }
        }
        // 3: verifying the constraints
        for (Inequality ineq : constraints) {
            if (!ineq.isSatisfied(board)) {
                return false;
            }
        }
        return true; // all checks passed
    }

    // SOLVER & HINT ENGINE (Lisa)

    // Backtracking algorithm to solve the board recursively
    // SOURCE: HintSolver2.java (repo), adapted: Cell[][] -> int[][],
    // b[r][c].value -> b[r][c]. The backtracking logic is untouched.
    private boolean solve(int[][] b, int r, int c) {
        // Lisa: recursive placement and backtracking logic
        if (r == size) return true;                   
        if (c == size) return solve(b, r + 1, 0);     
        if (b[r][c] != 0) return solve(b, r, c + 1);  

        // Try candidate values 1 through size
        for (int val = 1; val <= size; val++) {
            if (isValidMove(b, r, c, val)) {          
                b[r][c] = val;                        
                if (solve(b, r, c + 1)) return true;  
                b[r][c] = 0;                          
            }
        }
        return false; // trigger backtracking step in parent recursive call
    }

    // Solves a copy of the board to give an ODD/EVEN clue to the player
    // SOURCE: HintSolver2.java (repo), adapted to int[][].
    // RIVAL: SolverHint.java (repo) fills the cell with the actual value -
    // not used here; team decision pending.
    private void giveHint() {
        // Lisa: check points, solve temp board, and display clue dialog
        if (playerPoints < 20) {
            JOptionPane.showMessageDialog(this, "Not enough points for a hint!");
            return;
        }

        // Independent copy of the board, preserving EVERY value the player
        // has already placed, so the solver builds on their progress.
        int[][] temp = new int[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                temp[r][c] = board[r][c];
            }
        }

        // Run solver engine on the copied grid
        if (!solve(temp, 0, 0)) {
            JOptionPane.showMessageDialog(this, "No valid solution possible from your current board! Check for mistakes.");
            return;
        }

        // Search for first empty cell and reveal parity clue (ODD/EVEN)
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] == 0) {
                    int correctVal = temp[r][c];      // the solved correct digit
                    String parity = (correctVal % 2 == 0) ? "EVEN" : "ODD";

                    playerPoints -= 20;               // deduct point cost
                    scoreLabel.setText("Points: " + playerPoints);

                    JOptionPane.showMessageDialog(this, "HINT: Cell at Row " + (r + 1) +
                            ", Col " + (c + 1) + " is an " + parity + " number!");
                    return;
                }
            }
        }
    }

    // UNIT TESTING & QA (Zwivhuya(me))

    // Tests string input validation and number bounds
    private void runStringTests() {
        // Zwivhuya(me): test empty strings, valid ranges, and bad input formats
        int max = 4;
        String[][] cases = {
            // { input, expected }   (0 = empty cell, -1 = invalid)
            {"", "0"}, {"1", "1"}, {"4", "4"}, {"  2  ", "2"},
            {"0", "-1"}, {"5", "-1"}, {"-1", "-1"}, {"abc", "-1"}, {"1.5", "-1"}
        };
        int pass = 0, total = 0;
        for (String[] tc : cases) {
            total++;
            if (parseCell(tc[0], max) == Integer.parseInt(tc[1])) pass++;
            else System.out.println("  FAIL: parseCell(\"" + tc[0] + "\") expected " + tc[1]);
        }
        System.out.println("[QA] runStringTests: " + pass + "/" + total + " passed");
    }

    // Tests mathematical logic for all inequality types
    private void runInequalityTests() {
        // Zwivhuya(me): test <, >, ^, v constraints, equal values, and empty cells
        int[][] b = {
            {1, 2, 3, 4},
            {4, 3, 2, 1},
            {2, 4, 1, 3},
            {3, 1, 4, 2}
        };
        int pass = 0, total = 0;

        // (0,0)=1 < (0,1)=2 -> satisfied
        total++; if (new Inequality(0, 0, 0, 1, '<').isSatisfied(b)) pass++;
        // same pair read backwards as 2 < 1 -> NOT satisfied
        total++; if (!new Inequality(0, 1, 0, 0, '<').isSatisfied(b)) pass++;
        // (0,1)=2 > (0,0)=1 -> satisfied; reversed 1 > 2 -> NOT
        total++; if (new Inequality(0, 1, 0, 0, '>').isSatisfied(b)) pass++;
        total++; if (!new Inequality(0, 0, 0, 1, '>').isSatisfied(b)) pass++;
        // equal values (1 and 1) can never satisfy a strict sign
        total++; if (!new Inequality(0, 0, 3, 1, '<').isSatisfied(b)) pass++;
        // an empty side cannot violate yet -> treated as satisfied
<<<<<<< HEAD
        int[][] b2 = PuzzleGenerator.deepCopy(b);
=======
        int[][] b2 = BoardFactory.deepCopy(b);
>>>>>>> 6c8543fac4042f77eac03c298de69b528a2a9a1f
        b2[0][0] = 0;
        total++; if (new Inequality(0, 0, 0, 1, '<').isSatisfied(b2)) pass++;
        // diagonal signs are checked the same way once both cells hold values
        total++; if (new Inequality(0, 0, 1, 1, '<', true).isSatisfied(b)) pass++;   // 1 < 3
        total++; if (!new Inequality(1, 0, 0, 1, '<', true).isSatisfied(b)) pass++;  // 4 < 2

        System.out.println("[QA] runInequalityTests: " + pass + "/" + total + " passed");
    }

    // UI & GAME LIFECYCLE (Karabo)

    // Gets horizontal symbol (< or >) between two cells if one exists
    private String getHorizontalConstraintSymbol(int r, int c) {
        // Karabo: lookup matching horizontal rule
        // Sign between (r,c) and (r,c+1); open side of the sign faces the bigger number.
        for (Inequality ineq : constraints) {
            if (ineq.isDiagonal()) continue;
            if (ineq.getR1() == r && ineq.getC1() == c
                    && ineq.getR2() == r && ineq.getC2() == c + 1) {
                return String.valueOf(ineq.getDir());           // stored left-to-right
            }
            if (ineq.getR2() == r && ineq.getC2() == c
                    && ineq.getR1() == r && ineq.getC1() == c + 1) {
                return ineq.getDir() == '<' ? ">" : "<";        // stored right-to-left
            }
        }
        return "";
    }

    // Gets vertical symbol (^ or v) between two cells if one exists
    private String getVerticalConstraintSymbol(int r, int c) {
        // Karabo: lookup matching vertical rule
        // Sign between (r,c) [top] and (r+1,c) [bottom].
        // Display: top < bottom -> "^"     top > bottom -> "v"
        for (Inequality ineq : constraints) {
            if (ineq.isDiagonal()) continue;
            if (ineq.getR1() == r && ineq.getC1() == c
                    && ineq.getR2() == r + 1 && ineq.getC2() == c) {
                return ineq.getDir() == '<' ? "^" : "v";        // stored top-to-bottom
            }
            if (ineq.getR2() == r && ineq.getC2() == c
                    && ineq.getR1() == r + 1 && ineq.getC1() == c) {
                return ineq.getDir() == '<' ? "v" : "^";        // stored bottom-to-top
            }
        }
        return "";
    }

    // Sets up grid data, default constraints, and game timer
    private void setupNewGame(int newSize) {
        // Karabo: initialize cell matrix, seed constraints, reset timer
        size = newSize;

        // The puzzle comes from the generator (the whole generator chain runs here)
        currentPuzzle = generator.generate(difficulty, newSize);
<<<<<<< HEAD
        board = PuzzleGenerator.deepCopy(currentPuzzle.puzzle);
=======
        board = BoardFactory.deepCopy(currentPuzzle.puzzle);
>>>>>>> 6c8543fac4042f77eac03c298de69b528a2a9a1f
        constraints = currentPuzzle.inequalities;

        secondsElapsed = 0;
        failCount = 0;
        playerPoints = 100;               // starting points (a hint costs 20)
        lives = new LifeManager(3);       // fresh lives for each new game

        if (gameTimer != null) gameTimer.stop();
        gameTimer = new Timer(1000, e -> {
            secondsElapsed++;
            updateTimerLabel();
        });
        gameTimer.start();
    }

    // Renders grid components, input boxes, and visual inequality labels
    private void createUI() {
        // Karabo: build layout panels and display inequality symbols on grid
        removeAll(); // allow rebuild when reloadGame changes size/difficulty
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ---- top bar: labels + controls ----
        timerLabel = new JLabel("Time: 00:00");
        updateTimerLabel();
        scoreLabel = new JLabel("Points: " + playerPoints);
        livesLabel = new JLabel("Lives: " + lives.getLives());

        // selections are set BEFORE the listeners so the first render
        // cannot trigger a reload loop
        difficultyCombo = new JComboBox<>(new String[] {"Easy", "Medium", "Hard"});
        difficultyCombo.setSelectedItem(difficulty);
        difficultyCombo.addActionListener(e -> {
            difficulty = (String) difficultyCombo.getSelectedItem();
            reloadGame(size);
        });

        sizeCombo = new JComboBox<>(new Integer[] {4, 5, 6});
        sizeCombo.setSelectedItem(size);
        sizeCombo.addActionListener(e -> reloadGame((Integer) sizeCombo.getSelectedItem()));

        JButton newGameBtn = new JButton("New Game");
        newGameBtn.addActionListener(e -> reloadGame(size));
        JButton hintBtn = new JButton("Hint (-20 pts)");
        hintBtn.addActionListener(e -> giveHint());
        JButton submitBtn = new JButton("Submit");
        submitBtn.addActionListener(e -> submitBoard());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.add(new JLabel("Futoshiki"));
        top.add(timerLabel);
        top.add(scoreLabel);
        top.add(livesLabel);
        top.add(new JLabel("Difficulty:"));
        top.add(difficultyCombo);
        top.add(new JLabel("Size:"));
        top.add(sizeCombo);
        top.add(newGameBtn);
        top.add(hintBtn);
        top.add(submitBtn);
        add(top, BorderLayout.NORTH);

        // ---- board: a (2n-1) x (2n-1) grid of cells and sign slots ----
        int slots = 2 * size - 1;
        boardPanel = new JPanel(new GridLayout(slots, slots, 4, 4));
        inputFields = new JTextField[size][size];

        for (int sr = 0; sr < slots; sr++) {
            for (int sc = 0; sc < slots; sc++) {
                if (sr % 2 == 0 && sc % 2 == 0) {
                    boardPanel.add(makeCellPanel(sr / 2, sc / 2));             // a real cell
                } else if (sr % 2 == 0) {
                    boardPanel.add(signLabel(                                  // horizontal sign
                            getHorizontalConstraintSymbol(sr / 2, sc / 2 - 1)));
                } else if (sc % 2 == 0) {
                    boardPanel.add(signLabel(                                  // vertical sign
                            getVerticalConstraintSymbol(sr / 2, sc / 2)));
                } else {
                    boardPanel.add(new JLabel(" "));                           // corner spacer
                }
            }
        }
        add(boardPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    // Builds one grid cell: the text field plus any diagonal sign in a corner
    private JPanel makeCellPanel(int r, int c) {
        JTextField field = new JTextField();
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setFont(new Font("Monospaced", Font.BOLD, 18));
        if (currentPuzzle != null && currentPuzzle.givens[r][c]) {
            field.setText(String.valueOf(board[r][c]));
            field.setEditable(false);
            field.setBackground(new Color(0xE0E0E0));
        }
        inputFields[r][c] = field;

        JPanel cellPanel = new JPanel(new BorderLayout());
        cellPanel.add(field, BorderLayout.CENTER);

        // Diagonal signs (Medium/Hard only): shown small in the corner of
        // cell 1 that faces cell 2, with a tooltip spelling out the rule.
        for (Inequality ineq : constraints) {
            if (!ineq.isDiagonal()) continue;
            if (ineq.getR1() != r || ineq.getC1() != c) continue;
            JLabel diag = new JLabel(String.valueOf(ineq.getDir()));
            diag.setFont(new Font("Dialog", Font.PLAIN, 12));
            diag.setForeground(new Color(0x0055AA));
            diag.setToolTipText("Cell (" + (r + 1) + "," + (c + 1) + ") "
                    + ineq.getDir() + " Cell (" + (ineq.getR2() + 1) + "," + (ineq.getC2() + 1) + ")");
            // shown in the strip below the cell (cell 2 is always diagonally below cell 1)
            cellPanel.add(diag, BorderLayout.SOUTH);
        }
        return cellPanel;
    }

    // A centered sign label used between cells
    private JLabel signLabel(String symbol) {
        JLabel label = new JLabel(symbol, SwingConstants.CENTER);
        label.setFont(new Font("Dialog", Font.BOLD, 20));
        label.setForeground(new Color(0x0055AA));
        return label;
    }

    // Refreshes the timer label (mm:ss)
    private void updateTimerLabel() {
        if (timerLabel != null) {
            timerLabel.setText(String.format("Time: %02d:%02d",
                    secondsElapsed / 60, secondsElapsed % 60));
        }
    }

    // Reads player entries from UI and evaluates win condition
    private void submitBoard() {
        // Karabo: parse text fields into board array and trigger win check
        int[][] parsed = new int[size][size];

        // 1) parse every field ("" counts as empty = 0)
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int v = parseCell(inputFields[r][c].getText(), size);
                if (v < 0) {
                    JOptionPane.showMessageDialog(this,
                            "Cell Row " + (r + 1) + ", Col " + (c + 1)
                                    + " is not a valid number (1-" + size + ").");
                    return;
                }
                parsed[r][c] = v;
            }
        }

        // 2) every entered number must obey the rules on the FULL entered board
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (parsed[r][c] != 0 && !isValidMove(parsed, r, c, parsed[r][c])) {
                    failCount++;
                    if (!lives.deductLife("MISTAKE")) {
                        gameTimer.stop();
                        return; // LifeManager already showed the game-over dialog
                    }
                    if (livesLabel != null) livesLabel.setText("Lives: " + lives.getLives());
                    JOptionPane.showMessageDialog(this,
                            "That placement breaks a row, column or sign rule. A life was lost.");
                    return;
                }
            }
        }

        // 3) everything entered so far is legal - keep the progress
        board = parsed;
        boolean full = true;
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board[r][c] == 0) full = false;

        if (full && checkWinCondition()) {
            gameTimer.stop();
            int bonus = Math.max(0, 100 - secondsElapsed);
            playerPoints += bonus;
            scoreLabel.setText("Points: " + playerPoints);
            JOptionPane.showMessageDialog(this,
                    "Solved in " + secondsElapsed + "s! Time bonus: +" + bonus
                            + ". Total points: " + playerPoints);
            reloadGame(size); // straight into the next puzzle
        }
    }

    // Parses one cell's text: "" -> 0 (empty), "1".."max" -> the value, else -1
    private int parseCell(String text, int max) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return 0;
        try {
            int v = Integer.parseInt(t);
            return (v >= 1 && v <= max) ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Restarts or changes the current level size
    private void reloadGame(int newSize) {
        // Karabo: re-run setup and refresh UI frame
        setupNewGame(newSize);
        createUI();
    }

    public static void main(String[] args) {
        // Launch the app on the Swing event dispatch thread
        SwingUtilities.invokeLater(FutoshikiGame::new);
    }
}