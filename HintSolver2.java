
private boolean solve(Cell[][] b, int r, int c) {
        if (r == size) return true;                   // Target beyond last row: puzzle fully solved
        if (c == size) return solve(b, r + 1, 0);     // End of column: advance to start of next row
        if (b[r][c].value != 0) return solve(b, r, c + 1); // Cell pre-filled: advance to next column

        // Try candidate values 1 through N
        for (int val = 1; val <= size; val++) {
            if (isValidMove(b, r, c, val)) { // Verify row/col and inequality rules
                b[r][c].value = val;        // Place candidate value
                
                if (solve(b, r, c + 1)) return true; // Recursively attempt to fill remaining board
                
                b[r][c].value = 0;          // Backtrack: clear placement on dead-end path
            }
        }
        return false; // Trigger backtracking step in parent recursive call
    }

    // Generates hint for the first unassigned empty cell
    private void giveHint() {
        if (playerPoints < 20) {
            JOptionPane.showMessageDialog(this, "Not enough points for a hint!");
            return;
        }

        // Create independent deep copy of the board, preserving EVERY value the player has
        // already placed (not just the original fixed seeds) so the solver builds on top of
        // the player's progress instead of ignoring it.
        Cell[][] temp = new Cell[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                temp[r][c] = new Cell(r, c);
                temp[r][c].value = board[r][c].value;     // Copy current value (0 if still empty)
                temp[r][c].isFixed = board[r][c].isFixed; // Preserve fixed flag
            }
        }

        // Run solver engine on copied grid
        if (!solve(temp, 0, 0)) {
            JOptionPane.showMessageDialog(this, "No valid solution possible from your current board! Check for mistakes.");
            return;
        }

        // Search for first empty player cell and reveal parity clue (ODD/EVEN)
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c].value == 0) {
                    int correctVal = temp[r][c].value; // Solved correct digit
                    String parity = (correctVal % 2 == 0) ? "EVEN" : "ODD";

                    playerPoints -= 20; // Deduct point cost
                    scoreLabel.setText("Points: " + playerPoints); // Update score label

                    JOptionPane.showMessageDialog(this, "HINT: Cell at Row " + (r + 1) + 
                            ", Col " + (c + 1) + " is an " + parity + " number!");
                    return;
                }
            }
        }
    }
