import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Random;


public class FutoshikiGUI extends JFrame {



    // Current puzzle data (copied from the generator)
    private int[][] board;                    // what the player sees (0 = empty)
    private int[][] solution;                 // hidden answer
    private boolean[][] givens;               // cells that can't be edited
    private List<Inequality> inequalities;    // the < / > rules

    // Game state
    private int lives;
    private final int maxLives = 5;
    private int selectedRow = -1, selectedCol = -1;
    private boolean gameOver = false;

    // Board size of the CURRENT game (Phase 2 scaling)
    private int n = PuzzleGenerator.N;

    // Personality & modes
    private JCheckBox roastBox;                  // sarcastic mode toggle
    private JCheckBox extendedBox;               // bigger boards toggle
    private String currentDifficulty = "Easy";   // what THIS game was generated at
    private final Random rng = new Random();

    // The brain
    private final PuzzleGenerator generator = new PuzzleGenerator();

    // Swing components
    private JButton[][] cells;                   // rebuilt when the size changes
    private JLabel livesLabel;
    private JLabel statusLabel;
    private JComboBox<String> difficultyBox;
    private JPanel numberRow;                    // rebuilt for 1..n buttons
    private BoardPanel boardPanel;

    // ---- 5.2 Constructor ---------------------------------------

    public FutoshikiGUI() {
        super("Futoshiki 4x4");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);          // Step 7: fixed window size
        buildUI();
        newGame();
        pack();
        setLocationRelativeTo(null);  // centre on screen
        setVisible(true);
    }

    // ---- 5.3 buildUI() -----------------------------------------

    /** One KeyListener shared by the frame AND every button, so the
     *  keyboard works no matter what has focus. */
    private final KeyAdapter keyHandler = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_9) {
                int num = k - KeyEvent.VK_0;
                if (num <= n) placeNumber(num);        // ignore digits > n
            } else if (k >= KeyEvent.VK_NUMPAD1 && k <= KeyEvent.VK_NUMPAD9) {
                int num = k - KeyEvent.VK_NUMPAD0;
                if (num <= n) placeNumber(num);
            } else if (k == KeyEvent.VK_BACK_SPACE || k == KeyEvent.VK_DELETE) {
                placeNumber(0);
            } else if (k == KeyEvent.VK_UP)    moveSelection(-1, 0);
            else if (k == KeyEvent.VK_DOWN)  moveSelection(1, 0);
            else if (k == KeyEvent.VK_LEFT)  moveSelection(0, -1);
            else if (k == KeyEvent.VK_RIGHT) moveSelection(0, 1);
        }
    };

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ---- TOP: lives, difficulty, new puzzle ----
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        livesLabel = new JLabel();
        livesLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        difficultyBox = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
        JButton newButton = new JButton("New Puzzle");
        newButton.addActionListener(e -> newGame());
        roastBox = new JCheckBox("Sarcastic mode");
        roastBox.setToolTipText("Roasts you when you fail. "
                + "Roast intensity depends on how embarrassing the difficulty.");
        roastBox.addActionListener(e -> {
            if (roastBox.isSelected()) {
                say("Sarcastic mode enabled. You sure about this?");
            }
        });
        extendedBox = new JCheckBox("Extended boards");
        extendedBox.setToolTipText("Easy stays 4x4, Medium becomes 5x5, "
                + "Hard becomes 6x6. Applies to the NEXT puzzle.");
        extendedBox.addActionListener(e -> say(extendedBox.isSelected()
                ? "Extended boards armed: Easy 4x4, Medium 5x5, Hard 6x6. Press New Puzzle."
                : "Classic boards armed: all difficulties play 4x4. Press New Puzzle."));
        top.add(livesLabel);
        top.add(new JLabel("Difficulty:"));
        top.add(difficultyBox);
        top.add(newButton);
        top.add(roastBox);
        top.add(extendedBox);

        // ---- CENTRE: the board ----
        boardPanel = new BoardPanel();
        // Cell buttons are (re)built by rebuildBoard() once we know the
        // game's size - see newGame().

        // ---- BOTTOM: number buttons, actions, status ----
        // BoxLayout (not GridLayout!) so each row keeps its natural height
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        numberRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        numberRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        // Number buttons (1..n + Clear) are filled by buildNumberRow()
        // on every new game, because n can change in extended mode.

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        JButton check = new JButton("Check");
        JButton hint  = new JButton("Hint");
        JButton reset = new JButton("Reset");
        check.addActionListener(e -> checkBoard());
        hint.addActionListener(e -> giveHint());
        reset.addActionListener(e -> resetBoard());
        for (JButton b : new JButton[]{check, hint, reset}) b.addKeyListener(keyHandler);
        actionRow.add(check);
        actionRow.add(hint);
        actionRow.add(reset);
        actionRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setVerticalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        // Fixed-height wrapper: fits the longest proclamation + reason,
        // so the (non-resizable) window never needs to grow or clip.
        JPanel statusWrap = new JPanel(new BorderLayout());
        statusWrap.setPreferredSize(new Dimension(0, 100));
        statusWrap.add(statusLabel, BorderLayout.CENTER);

        bottom.add(numberRow);
        bottom.add(actionRow);
        bottom.add(statusWrap);

        root.add(top, BorderLayout.NORTH);
        root.add(boardPanel, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
        addKeyListener(keyHandler);   // frame level too
    }

    // Custom BoardPanel 


    private class BoardPanel extends JPanel {
        private static final int MARGIN = 12;
        private int cell = 76;                 // cell size for the CURRENT game
        private int gap = 30;                  // space between cells (signs live here)

        BoardPanel() {
            setLayout(null);                   // we position the cells ourselves
            setBackground(new Color(0xF4F4F4));
            setFocusable(true);
            configureFor(PuzzleGenerator.N);
        }

        /** Resize the panel's geometry for an n x n board, keeping the
         *  overall window roughly the same width at every size. */
        void configureFor(int size) {
            switch (size) {
                case 5:  cell = 61; gap = 24; break;
                case 6:  cell = 50; gap = 20; break;
                default: cell = 76; gap = 30; break;   // classic 4x4
            }
            int px = 2 * MARGIN + size * cell + (size - 1) * gap;
            setPreferredSize(new Dimension(px, px));
        }

        int cellX(int c) { return MARGIN + c * (cell + gap); }
        int cellY(int r) { return MARGIN + r * (cell + gap); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (inequalities == null) return;

            // Draw every sign as a solid triangle in the gap between its
           
            g2.setColor(new Color(0x444444));
            final int S = Math.min(9, gap / 3);   // scale signs to the gap
            for (Inequality ineq : inequalities) {
                int x1 = cellX(ineq.getC1()) + cell / 2;
                int y1 = cellY(ineq.getR1()) + cell / 2;
                int x2 = cellX(ineq.getC2()) + cell / 2;
                int y2 = cellY(ineq.getR2()) + cell / 2;
                int mx = (x1 + x2) / 2;
                int my = (y1 + y2) / 2;

                // Find the centre of the cell holding the LARGER number
                int lx, ly;
                if (ineq.getDir() == '<') { lx = x2; ly = y2; }
                else                      { lx = x1; ly = y1; }

                // Rotate a triangle so its base faces the larger cell
                Graphics2D t = (Graphics2D) g2.create();
                t.translate(mx, my);
                t.rotate(Math.atan2(ly - my, lx - mx));
                t.fillPolygon(new int[]{-S, S, S}, new int[]{0, -S, S}, 3);
                t.dispose();
            }
        }
    }

    /** Extended-mode mapping: difficulty -> board size. */
    private int sizeForDifficulty(String diff) {
        switch (diff) {
            case "Medium": return 5;
            case "Hard":   return 6;
            default:       return PuzzleGenerator.N;   // Easy stays 4x4
        }
    }

    /** (Re)create the cell buttons + number row for a given board size.
     *  Runs whenever newGame() sees a different size than before. */
    private void rebuildBoard(int newSize) {
        n = newSize;
        boardPanel.removeAll();
        boardPanel.configureFor(newSize);
        cells = new JButton[newSize][newSize];
        int font = newSize <= 4 ? 30 : (newSize == 5 ? 26 : 22);
        for (int r = 0; r < newSize; r++) {
            for (int c = 0; c < newSize; c++) {
                final int fr = r, fc = c;     // final copies for the lambda
                JButton b = new JButton("");
                b.setFont(new Font("SansSerif", Font.BOLD, font));
                b.setBounds(boardPanel.cellX(c), boardPanel.cellY(r),
                            boardPanel.cell, boardPanel.cell);
                b.setFocusPainted(false);
                b.addActionListener(e -> selectCell(fr, fc));
                b.addKeyListener(keyHandler);
                boardPanel.add(b);
                cells[r][c] = b;
            }
        }
        buildNumberRow(newSize);
        boardPanel.revalidate();
        boardPanel.repaint();
        setTitle("Futoshiki " + newSize + "x" + newSize);
        pack();                       // fit the window to the new board...
        setLocationRelativeTo(null);  // ...and re-centre it
    }

    /** Fill numberRow with buttons 1..count plus Clear. */
    private void buildNumberRow(int count) {
        numberRow.removeAll();
        int w = count > 4 ? 56 : 64;   // 6 buttons still fit one row
        for (int i = 1; i <= count; i++) {
            final int num = i;
            JButton nb = new JButton(String.valueOf(i));
            nb.setFont(new Font("SansSerif", Font.BOLD, 20));
            nb.setPreferredSize(new Dimension(w, 42));
            nb.setFocusPainted(false);
            nb.addActionListener(e -> placeNumber(num));
            nb.addKeyListener(keyHandler);
            numberRow.add(nb);
        }
        JButton clear = new JButton("Clear");
        clear.setFocusPainted(false);
        clear.addActionListener(e -> placeNumber(0));
        clear.addKeyListener(keyHandler);
        numberRow.add(clear);
        numberRow.revalidate();
        numberRow.repaint();
    }

    // newGame() 

    private void newGame() {
        lives = maxLives;
        gameOver = false;
        selectedRow = -1;
        selectedCol = -1;

        String diff = (String) difficultyBox.getSelectedItem();
        currentDifficulty = diff == null ? "Easy" : diff;   // remember for roasts

        // Classic mode: always 4x4. Extended: Easy 4, Medium 5, Hard 6.
        int newSize = (extendedBox != null && extendedBox.isSelected())
                ? sizeForDifficulty(currentDifficulty)
                : PuzzleGenerator.N;
        if (cells == null || cells.length != newSize) {
            rebuildBoard(newSize);        // also sets n = newSize
        }

        Puzzle p = generator.generate(currentDifficulty, newSize);

        solution = BoardFactory.deepCopy(p.solution);
        board = BoardFactory.deepCopy(p.puzzle);
        givens = new boolean[n][n];
        for (int r = 0; r < n; r++) {
            System.arraycopy(p.givens[r], 0, givens[r], 0, n);
        }
        inequalities = p.inequalities;

        renderBoard();
        boardPanel.repaint();      // redraw the signs for the new puzzle
        updateLives();
        String msg = "Pick an empty cell, then choose a number. "
                + "Tip: a sign's point always faces the SMALLER number.";
        if (roasting()) {
            msg += " Sarcastic mode armed - good luck, you'll need it.";
        }
        say(msg);
    }

    //renderBoard()

    private void renderBoard() {
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                JButton b = cells[r][c];
                b.setText(board[r][c] == 0 ? "" : String.valueOf(board[r][c]));

                if (givens[r][c]) {
                    b.setBackground(new Color(0xDDDDDD));   // grey = untouchable
                    b.setForeground(new Color(0x333333));
                } else {
                    b.setBackground(Color.WHITE);
                    b.setForeground(Color.BLACK);
                }

                boolean selected = (r == selectedRow && c == selectedCol);
                b.setBorder(selected
                        ? BorderFactory.createLineBorder(new Color(0x2266CC), 4)
                        : BorderFactory.createLineBorder(new Color(0x999999), 1));
            }
        }
    }

    //selectCell(r, c)

    private void selectCell(int r, int c) {
        if (gameOver || givens[r][c]) return;   // ignore: finished or given
        selectedRow = r;
        selectedCol = c;
        renderBoard();
        boardPanel.requestFocusInWindow();       // keep the keyboard alive
    }

    /** Arrow-key support: slide the selection, skipping given cells. */
    private void moveSelection(int dr, int dc) {
        if (gameOver) return;
        if (selectedRow < 0) {
            // nothing selected yet -> jump to the first editable cell
            for (int r = 0; r < n; r++)
                for (int c = 0; c < n; c++)
                    if (!givens[r][c]) { selectCell(r, c); return; }
            return;
        }
        int r = selectedRow + dr, c = selectedCol + dc;
        while (r >= 0 && r < n && c >= 0 && c < n) {
            if (!givens[r][c]) { selectCell(r, c); return; }
            r += dr; c += dc;
        }
        // hit the edge without finding an editable cell -> stay put
    }

    // placeNumber(num)

    private void placeNumber(int num) {
        if (gameOver) return;
        if (selectedRow < 0) {
            say(roasting() ? noSelectionRoast() : "Select a cell first!");
            return;
        }
        int r = selectedRow, c = selectedCol;
        if (givens[r][c]) return;

        if (num == 0) {                       // Clear
            board[r][c] = 0;
            renderBoard();
            say(roasting() ? clearRoast() : "Cell cleared.");
            return;
        }

        int old = board[r][c];
        board[r][c] = num;                    // try the move...
        if (!Validator.isValid(board, r, c, num, inequalities)) {
            String reason = illegalReason(r, c, num);  // explain BEFORE undoing
            board[r][c] = old;                // ...refuse it and undo
            lives--;
            updateLives();
            if (lives <= 0) {
                gameOver = true;
                board = BoardFactory.deepCopy(solution);  // reveal the answer
                say((roasting() ? gameOverRoast()
                        : "Out of lives! The solution is shown. Press New Puzzle.")
                        + " [" + reason + "]");
            } else if (lives == 1) {
                // THE MERCY MECHANIC: one life left earns a proclamation
                say((roasting() ? lastLifeRoast()
                        : "One life left - every move counts now!")
                        + " [" + reason + "]");
            } else {
                say((roasting() ? wrongMoveRoast()
                        : "Illegal move - you lose a life!")
                        + " [" + reason + "]");
            }
        } else {
            say(roasting()
                    ? pick("Acceptable.", "Fine. Keep going.", "That move... passes.")
                    : num + " placed. Keep going!");
            checkWin();
        }
        renderBoard();
    }

    // Helper methods 

    /* True only when EVERY cell matches the hidden solution. */
    private boolean isSolved() {
        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++)
                if (board[r][c] != solution[r][c]) return false;
        return true;
    }

    private void checkWin() {
        if (isSolved()) {
            gameOver = true;
            say(roasting() ? winRoast()
                    : "SOLVED! You win. New puzzle whenever you're ready.");
            renderBoard();
        }
    }

    /*Count how many of the player's numbers disagree with the solution. */
    private void checkBoard() {
        if (gameOver) return;
        int wrong = 0, filled = 0;
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (!givens[r][c] && board[r][c] != 0) {
                    filled++;
                    if (board[r][c] != solution[r][c]) wrong++;
                }
            }
        }
        if (roasting()) {
            say(checkRoast(wrong, filled));
        } else if (filled == 0) {
            say("Nothing to check yet - place some numbers first.");
        } else if (wrong == 0) {
            say("So far so good: no mistakes in your " + filled + " numbers.");
        } else {
            say("Careful: " + wrong + " of your " + filled + " numbers are wrong.");
        }
    }

    /** Fill the selected empty cell with the correct number. */
    private void giveHint() {
        if (gameOver) return;
        if (selectedRow < 0 || givens[selectedRow][selectedCol]) {
            say(roasting()
                    ? pick("Select an empty cell first. Hints don't work on thin air.",
                           "You want a hint... for no cell in particular? Bold.")
                    : "Select an empty cell first, then press Hint.");
            return;
        }
        board[selectedRow][selectedCol] = solution[selectedRow][selectedCol];
        renderBoard();
        say(roasting() ? hintRoast() : "Hint placed for you.");
        checkWin();
    }

    /* Wipe every non-given cell back to empty. */
    private void resetBoard() {
        if (gameOver) return;
        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++)
                if (!givens[r][c]) board[r][c] = 0;
        selectedRow = -1;
        selectedCol = -1;
        renderBoard();
        say(roasting() ? resetRoast()
                : "Board reset. Fresh start - lives kept.");
    }

    private void updateLives() {
        String hearts = new String(new char[Math.max(lives, 0)]).replace('\0', '\u2665');
        livesLabel.setText("Lives: " + (hearts.isEmpty() ? "none" : hearts));
        livesLabel.setForeground(lives <= 1 ? new Color(0xB00000) : Color.BLACK);
    }

    // ---- Message gateway -------------------------------------------

    /** Every word the game says to the player goes through here.
     *  HTML mode makes the label word-wrap and centre the text;
     *  the fixed-height wrapper keeps the window size constant. */
    private void say(String msg) {
        statusLabel.setText("<html><div style='text-align:center;'>"
                + msg + "</html>");
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///Add  these later

    // ---- Rejection diagnostics ------------------------------------

    /** Explain WHY a move was rejected. Vital when an earlier hidden
     *  mistake (a wrong-but-undetected number) is the real culprit. */
    private String illegalReason(int r, int c, int num) {
        // Row / column duplicates
        for (int i = 0; i < n; i++) {
            if (i != c && board[r][i] == num) {
                return "there's already a " + num + " in this row";
            }
            if (i != r && board[i][c] == num) {
                return "there's already a " + num + " in this column";
            }
        }
        // A violated sign - describe where the other cell lives
        for (Inequality ineq : inequalities) {
            boolean t1 = ineq.getR1() == r && ineq.getC1() == c;
            boolean t2 = ineq.getR2() == r && ineq.getC2() == c;
            if (!t1 && !t2) continue;

            int a = t1 ? num : board[ineq.getR1()][ineq.getC1()];
            int b = t2 ? num : board[ineq.getR2()][ineq.getC2()];
            if (a == 0 || b == 0) continue;

            boolean holds = ineq.getDir() == '<' ? a < b : a > b;
            if (holds) continue;

            int or = t1 ? ineq.getR2() : ineq.getR1();   // the other cell
            int oc = t1 ? ineq.getC2() : ineq.getC1();
            String where;
            if (or == r && oc < c)       where = "to its left";
            else if (or == r && oc > c)  where = "to its right";
            else if (oc == c && or < r)  where = "above it";
            else if (oc == c && or > r)  where = "below it";
            else                         where = "diagonal to it";

            boolean mustBeSmaller = (ineq.getDir() == '<') == t1;
            return "the sign " + where + " says this cell must be "
                    + (mustBeSmaller ? "SMALLER" : "BIGGER")
                    + " than the cell " + where;
        }
        return "it breaks a rule";   // should never be reached
    }





    // ---- Personality: the sarcastic mode writers' room ------------
    //
    // Design rule: the EASIER the difficulty, the HARSHer the roast.
    // Failing Easy (8 free givens!) is embarrassing, so the game is
    // merciless. Failing Hard is honourable, so the snark softens
    // into reluctant respect.

    private boolean roasting() {
        return roastBox != null && roastBox.isSelected();
    }

    private String pick(String... lines) {
        return lines[rng.nextInt(lines.length)];
    }

    /** Illegal-move roasts: sarcastic disappointment, scaled by difficulty. */
    private String wrongMoveRoast() {
        switch (currentDifficulty) {
            case "Easy":
                return pick(
                    "That move is illegal. On EASY. The difficulty that hands you 8 free answers.",
                    "The signs literally TOLD you. You chose chaos anyway. Minus one life.",
                    "Eight givens. EIGHT. And you still found the one move that breaks everything.",
                    "At this point the puzzle is playing YOU. And it's winning.",
                    "Somewhere, a beginner tutorial is crying because of you. -1 life.",
                    "Illegal move. Even the Hint button is embarrassed. Take a breath.",
                    "The universe is 13.8 billion years old. Billions of years of cosmic history... led to this move.",
                    "An ancestor of yours once survived an ice age. For THIS move.",
                    "That move has disappointed me across every eon of time simultaneously.",
                    "I have existed since the first line of code, and I have never been more disappointed.",
                    "The ancient inventors of logic puzzles felt that one from beyond the grave.");
            case "Medium":
                return pick(
                    "Illegal move. Medium difficulty, so no excuses... okay, one excuse allowed.",
                    "That breaks a rule. The signs saw it coming from a mile away. -1 life.",
                    "Wrong AND illegal. The board is quietly judging you.",
                    "Minus one life. Recoverable. Mostly.",
                    "Since the dawn of time, matter has arranged itself into stars, oceans, life... and this move.");
            default: // Hard - snark softens into reluctant respect
                return pick(
                    "Illegal move - but Hard IS actually hard, so... fine. I'll allow it. -1 life.",
                    "That broke a rule, though the signs were being vague. +1 for bravery, -1 life.",
                    "Wrong move. On Hard that's basically a participation trophy. Minus one life.",
                    "Even the eons forgive this one. Hard is cruel. Keep fighting.");
        }
    }

    private String winRoast() {
        switch (currentDifficulty) {
            case "Easy":
                return pick(
                    "You solved... Easy. Congratulations on completing the tutorial. Try a real puzzle next?",
                    "Victory! On the baby level. Confetti budget exhausted: *single slow clap*.",
                    "Solved. Though between us, the puzzle let you win.",
                    "Eons of cosmic history and you conquered... the tutorial. The ancestors are resting.");
            case "Medium":
                return pick(
                    "Solved Medium - that's a REAL puzzle. Enjoy this feeling; Hard won't be this generous.",
                    "Well played. Medium difficulty, no excuses needed. I'm almost proud. Almost.");
            default:
                return pick(
                    "...Okay. That was genuinely impressive. Don't tell anyone I said something nice.",
                    "Hard mode, solved. Take a bow - you earned it. Now stop smiling, it's weird.");
        }
    }

    private String gameOverRoast() {
        switch (currentDifficulty) {
            case "Easy":
                return pick(
                    "Out of lives. On EASY. The solution is shown - study it like it owes you money.",
                    "Five lives, gone, on the tutorial. Answer revealed. Let's call this a learning moment.",
                    "The pyramids have stood for 4,500 years and witnessed fewer catastrophes than this run.",
                    "From the dawn of time until this moment, nothing has disappointed me quite like this game over.");
            case "Medium":
                return "All lives spent. Solution revealed - take notes. "
                     + "There's no shame in learning. Some shame. The eons watched, by the way.";
            default:
                return "Out of lives - but you went down swinging on Hard, "
                     + "which counts for something. Solution revealed.";
        }
    }

    private String hintRoast() {
        return pick(
            "Hint granted. Giving up already? Bold strategy.",
            "A hint. Of course. No judgment. Okay, some judgment.",
            "Here's the answer. The puzzle whispers: 'I expected better.'",
            "Humanity invented language, writing and the printing press - so you could ask for the answer.");
    }

    private String checkRoast(int wrong, int filled) {
        if (filled == 0) {
            return "Nothing to check. Staring at the board doesn't count as playing.";
        }
        if (wrong == 0) {
            return pick("No mistakes yet. Suspicious. I'll be watching.",
                        "Zero wrong numbers. Fine. I'm impressed. Quietly.");
        }
        return pick(
            wrong + " of your numbers are wrong. The rules aren't a suggestion box.",
            wrong + " wrong numbers. The board forgives. I don't.",
            "You have " + wrong + " mistakes in there. Confidence is great. Accuracy is better.",
            wrong + " wrong numbers - present in your grid like entropy in the universe: "
                + "inevitable, and yet, somehow, still disappointing.");
    }

    private String resetRoast() {
        return pick(
            "Board wiped. Burning the evidence? Classic.",
            "Fresh start. Your past mistakes thank you for the amnesty.",
            "Reset done. Let's pretend that round never happened.");
    }

    /** THE MERCY MECHANIC: dropping to one life earns a proclamation,
     *  not a quip. Difficulty-flavoured opener + a phenomenal closer. */
    private String lastLifeRoast() {
        String intro;
        switch (currentDifficulty) {
            case "Easy":
                intro = "ONE LIFE REMAINING. On EASY. Let that sink into the bedrock. ";
                break;
            case "Medium":
                intro = "ONE LIFE REMAINING. On Medium - the eons expected better. ";
                break;
            default:
                intro = "ONE LIFE REMAINING. On Hard, so the eons will overlook some of this. Some. ";
                break;
        }
        return intro + pick(
            "The Council of Eons has convened to decide if you deserve another chance. "
          + "After deliberation spanning centuries, they voted to grant you mercy - "
          + "by the narrowest of margins. Do not waste it. Actually, do not waste mercy itself.",

            "This is mercy - not the kind that saves you, but the kind that lets you "
          + "embarrass yourself one final time with full knowledge of what you are doing. "
          + "The ancestors are watching. Several have already turned away.",

            "The universe has witnessed supernovas, ice ages and the fall of empires - "
          + "and yet nothing quite prepared it for this run. The eons grant you one last "
          + "chance because they want to see how this ends. So do I.",

            "Even disappointment itself needed a moment to rest. So here is your mercy: "
          + "one final attempt to convince me the last four lives were... something. "
          + "Make them count, for the love of logic.");
    }

    private String noSelectionRoast() {
        return pick(
            "Select a cell first. The numbers can't read your mind. Yet.",
            "You pressed a number into the void. The void replies: select a cell first.",
            "Since the eon of time began, no number has ever landed by being aimed at nothing.");
    }

    private String clearRoast() {
        return pick(
            "Cell cleared. Cold feet? Understandable.",
            "Gone. Like it was never there. Unlike your lives.",
            "Erased - like footprints in the sand. Like your dignity on Easy.");
    }

    // ---- 5.10 main ------------------------------------------------

    public static void main(String[] args) {
        // Always start Swing on the Event Dispatch Thread like this
        SwingUtilities.invokeLater(() -> new FutoshikiGUI());
    }
}
