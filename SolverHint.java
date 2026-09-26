
    // Backtracking algorithm to solve Futoshiki puzzle grid recursively
    private boolean solve(Cell[][] b, int r, int c) {
        if (r == size) return true;                          // past last row: solved
        if (c == size) return solve(b, r + 1, 0);             // end of row: go to next row
        if (b[r][c].value != 0) return solve(b, r, c + 1);    // pre-filled: skip ahead

        for (int val = 1; val <= size; val++) {
            if (isValidMove(b, r, c, val)) {
                b[r][c].value = val;

                if (solve(b, r, c + 1)) return true;

                b[r][c].value = 0; // backtrack
            }
        }
        return false;
    }

    // Generates a hint by solving a scratch copy of the board and inserting
    // the correct value directly into the first empty cell on the real board
    private void giveHint() {
        if (playerPoints < 20) {
            JOptionPane.showMessageDialog(this, "Not enough points for a hint!");
            return;
        }

        // deep copy of the current board (including player progress)
        Cell[][] temp = new Cell[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                temp[r][c] = new Cell(r, c);
                temp[r][c].value = board[r][c].value;
                temp[r][c].isFixed = board[r][c].isFixed;
            }
        }

        if (!solve(temp, 0, 0)) {
            JOptionPane.showMessageDialog(this, "No valid solution possible from your current board! Check for mistakes.");
            return;
        }

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c].value == 0) {
                    int correctVal = temp[r][c].value;

                    board[r][c].value = correctVal;    // insert the actual solved value
                    board[r][c].isFixed = true;         // lock it so the player can't overwrite/clear it

                    // reflect the inserted value in the corresponding UI field
                    inputFields[r][c].setText(String.valueOf(correctVal));
                    inputFields[r][c].setEditable(false);
                    inputFields[r][c].setBackground(new Color(0xB18007));

                    playerPoints -= 20;
                    scoreLabel.setText("Points: " + playerPoints);

                    revalidate();
                    repaint(); // refresh the board UI so the inserted value is visible

                    JOptionPane.showMessageDialog(this, "HINT: Cell at Row " + (r + 1) +
                            ", Col " + (c + 1) + " has been filled in for you.");
                    return;
                }
            }
        }

        // solve() succeeded but no empty cell was found — board was already complete
        JOptionPane.showMessageDialog(this, "Board is already complete!");
    }

