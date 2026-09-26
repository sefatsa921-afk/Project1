# Futoshiki `src/` — code explained, block by block

How to use this: each class is broken into **blocks**. Each block says what it does, and the **⭐ line** is the one that matters most — remove it and the idea falls apart. Line numbers refer to the files in `src/`.

---

## 1. `Inequality.java` — one rule on the board (Siya)

**Block: fields (L5–8)**
What one sign remembers: the two cells it connects (`r1,c1` and `r2,c2`), its direction (`'<'` or `'>'`), and whether it's diagonal.
⭐ `private final int r1, c1;` — the `final` is the important word: a sign is created once and never moves. The "signs can never lie" guarantee starts with the data being immutable.

**Block: the two constructors (L11–23)**
The short constructor is for straight (non-diagonal) signs; it delegates to the full one. The full constructor does six `this.x = x` copies (parameter → field).
⭐ `this(r1, c1, r2, c2, dir, false);` — constructor delegation: one implementation instead of two. `this(...)` must be the first statement in a constructor.

**Block: getters (L27–32)**
One-line read accessors so the outside world (GUI, solver, tests) can peek at the private fields.
⭐ `public char getDir() { return dir; }` — the direction is the whole point of a sign; everything else is just coordinates.

**Block: `isSatisfied(board)` (L41–52) — THE important method**
Answers: "do the current numbers on the board obey this sign?"
⭐ `if (a == 0 || b == 0) { return true; }` — **the most important line in the file**: 0 means "empty", and an empty cell cannot violate a rule yet, so the rule counts as satisfied until BOTH cells hold numbers. The generator and the GUI both rely on this behaviour, and it's why a half-filled board never "breaks" a sign.
Also: `return a < b;` / `return a > b;` — strict comparisons; equal numbers fail the rule (that's why the diagonal hunter in `InequalityBuilder` looks for a pair that differs).

**Block: `toString()` (L57–60)**
Human-readable form, e.g. `(1,2) < (2,2)` — used in test output and debugging only.
⭐ none — a convenience, not logic.

---

## 2. `Puzzle.java` — the shipping box (Siya)

**Block: the five fields (L17–21)**
Everything one finished puzzle is: its size `n`, the hidden `solution`, the player's view `puzzle` (0 = hidden), the `givens` mask (true = locked), and the list of signs. `public final` = readable, frozen after construction.
⭐ `public final int[][] solution;` — the secret the game keeps: hints, the win check, and the "Check" button all read from it.

**Block: the constructor (L23–30)**
Takes all five pieces at once and stores them.
⭐ none — pure packaging. This class is a box, not logic; its job is to make "hand the GUI a complete puzzle" a single object.

---

## 3. `BoardFactory.java` — the board maker (Siya)

**Block: fields (L19–20)**
`N = 4` — the classic size, shared by the whole project. `rng` — the dice, one instance for the object's life.
⭐ `public static final int N = 4;` — one source of truth for the classic size; other classes write `BoardFactory.N` so the number exists in exactly one place.

**Block: no-arg overload (L33–35)**
⭐ `return generateLatinSquare(N);` — delegation: the 4×4 version just calls the general version, so the real logic exists once.

**Block: the cyclic pattern (L39–46)**
Fills every cell with the formula `(row + col) % n + 1`. Why it's valid: in any row, as `c` runs 0..n−1, `(r+c) % n` hits every remainder exactly once — so every number appears once per row; the same argument works for columns.
⭐ `grid[r][c] = (r + c) % n + 1;` — **this one formula is what makes the entire approach safe**: the base pattern is *provably* a valid board, so the scrambling steps below can never break it. No random board, no validation needed.

**Block: shuffle the rows (L49–56)**
Builds a list of row indices, shuffles it, then copies each chosen row whole into a new grid.
⭐ `System.arraycopy(grid[rows.get(r)], 0, next[r], 0, n);` — moves whole rows. Swapping rows can never create a duplicate in any row or column.

**Block: shuffle the columns (L59–68)**
Same idea sideways.
⭐ `next[r][c] = grid[r][cols.get(c)];` — destination column `c` receives whatever lived in a randomly chosen source column.

**Block: rename the digits (L71–78)**
Shuffles a list of the digits 1..n and re-labels every cell through it. A re-labelling is a bijection, so "every number once per row/column" survives.
⭐ `grid[r][c] = symbols.get(grid[r][c] - 1);` — **this is what actually makes boards different.** Steps 2–3 only moved things around; this one changes the values, so two generated boards genuinely differ.

**Block: `deepCopy` (L90–96)**
Copies a 2-D array so the two grids share *nothing*.
⭐ `copy[r] = src[r].clone();` — without this, `copy = board` makes both names point at the same rows, and the solver's edits would corrupt the caller's board. The uniqueness counter and both games depend on it.

---

## 4. `Validator.java` — the referee (Jack)

**Block: the signature (L25–26)**
`static boolean isValid(board, r, c, num, inequalities)` — a pure function: same inputs, same answer, nothing mutated.
⭐ `public static boolean isValid(...)` — "static, no state" is why this is the **single source of truth** for legality: the solver, both games, and the tests all call this exact method, so "legal" means the same thing everywhere in the project.

**Block: the row/column scan (L27–32)**
One loop over `i = 0..n−1` checks both the row and the column for an existing copy of `num`. (It reads `n` from the board itself, so the same code works for 4×4, 5×5, 6×6.)
⭐ `if (i != c && board[r][i] == num) return false;` — the subtle part is `i != c`: it skips the cell being filled, so the check only catches *real* duplicates, not the number we're trying to place.

**Block: the sign check (L35–50)**
Walks every sign and keeps only the ones touching the target cell. For each, it determines the value at both ends — using the candidate `num` for the end we're filling — and enforces the direction if both ends hold numbers.
⭐ `int a = touchesFirst ? num : board[ineq.getR1()][ineq.getC1()];` — **the whole trick in one ternary**: the rule is tested against the number we're *about to place*, not just what's on the board right now.
⭐ `if (a == 0 || b == 0) continue;` — same empty-cell rule as `Inequality`: an empty neighbour can't be violated yet.

**Block: the verdict (L52)**
⭐ `return true;` — only reached if every row, column and sign check passed: the move is legal.
---

## 5. `InequalityBuilder.java` — the sign painter (Jack)

**Block: field (L19)**
Its own dice.
⭐ none.

**Block: horizontal signs (L33–39)**
Visits every left–right neighbour pair; rolls the dice; if the roll lands under `dens`, stamps a sign there.
⭐ L36 `if (rng.nextDouble() < dens) { list.add(signBetween(...)); }` — **`dens` is the difficulty knob**: 70% of pairs get signs on Easy (lots of help), 35% on Hard (rare hints).

**Block: vertical signs (L42–48)**
The same loop over top–bottom pairs.
⭐ (mirror of L36 — same coin flip, different direction.)

**Block: the diagonal sign (L53–65)**
Medium/Hard get exactly ONE diagonal sign. The catch: diagonal neighbours can hold **equal** numbers (only rows and columns are constrained), and you can't honestly order equals. So it hunts: pick a random 2×2 block, randomly choose one of its two diagonals, and keep trying until the two values differ (max 20 tries).
⭐ L60 `if (solution[r1][c1] != solution[r2][c2]) { ... }` — **this guard is what keeps diagonal signs honest**: the builder refuses to sign a pair of equal numbers.
Also note L55 `int r = rng.nextInt(n - 1);` — `n−1`, because it's picking the top-left corner of a 2×2 block, not a cell.

**Block: `signBetween` (L71–75) — the truth machine**
⭐ L73 `char dir = solution[r1][c1] < solution[r2][c2] ? '<' : '>';` — **the most important line in the class**: the direction is read from the *actual solution*, so no sign can ever lie. The entire "truthful signs" guarantee of the project lives in this one ternary.

---

## 6. `SolutionCounter.java` — the counter (Lisa)

**Block: `countSolutions` (L25–32)**
The public entry point: count solutions of a partially-filled puzzle, stopping at `cap`.
⭐ L30 `int[][] work = BoardFactory.deepCopy(puzzle);` — work on a copy; the caller's board must come back untouched.
⭐ L29 `int[] count = {0};` — **the Java trick**: a plain `int` can't be updated by a recursive method, but an *array* can (arrays are passed by reference). Every level of the recursion reads and writes the same box, `count[0]`.

**Block: find the first empty cell (L46–55)**
Scans row by row until it finds a 0.
⭐ L52 `break outer;` — a **labelled break** that jumps out of *both* loops at once. A plain `break` would only leave the inner loop, and the scan would keep going.

**Block: complete board → count it (L58–70)**
No empty cells left means we've found a full solution. Before counting it, a safety re-check confirms every filled cell obeys the rules (protects the case where a puzzle is handed in already fully filled).
⭐ L68 `count[0]++;` — the only line in the whole class that increases the answer.
⭐ L69 `return count[0] >= cap;` — **the early stop**: once the cap (2) is reached, every level of the recursion returns true and the search unwinds instantly. This is what makes the gate cheap: we never count past two.

**Block: try every digit (L73–79) — the heart of backtracking**
For the first empty cell, try 1..n; each guess that passes the referee is placed, recursed into, and undone if it doesn't lead somewhere good.
⭐ L75 `board[r][c] = num;` (place) ... L77 `board[r][c] = 0;` (undo) — **the place/undo pair IS backtracking**: guess, explore, and if it fails, restore the board as if the guess never happened.
⭐ L76 `if (solve(board, inequalities, count, cap)) return true;` — if a deeper level reports "we already have enough solutions", stop immediately and pass the word up.

**Block: dead end (L82)**
⭐ L82 `return false;` — nothing fit this cell; tells the level above "your guess didn't work, try the next digit".

---

## 7. `PuzzleGenerator.java` — the foreman (Karabo)

**Block: fields (L22–26)**
`N` forwarded from `BoardFactory`; two worker objects (Siya's board factory, Jack's sign painter). Lisa's counter is static, so it's called directly.
⭐ L25 `private final BoardFactory boardFactory = new BoardFactory();` — **composition**: the orchestrator *owns* its workers and tells them what to do, in order.

**Block: the difficulty table (L56–69)**
⭐ the three case lines (L60–68) — **the entire difficulty design in one place**: Easy = 70% signs / 50% givens / no diagonal; Medium = 50% / 31% / one diagonal; Hard = 35% / 19% / one diagonal.
⭐ L59 `switch (difficulty == null ? "" : difficulty)` — the null guard: a nonsense or missing difficulty falls through to Hard instead of crashing.

**Block: the attempt loop, stages ① and ② (L73–79)**
Up to 100 candidates, each built the same way.
⭐ L75 `int[][] solution = boardFactory.generateLatinSquare(n);` — stage 1: a fresh random solved board.
⭐ L79 `inequalityBuilder.createInequalities(solution, dens, allowDiagonal);` — stage 2: truthful signs painted onto it.

**Block: hiding the cells (L82–92)**
First a deep copy of the solution (so the hidden answer is never touched), then a coin flip per cell.
⭐ L86 `if (rng.nextDouble() < keepRate) { givens[r][c] = true; } else { puzzle[r][c] = 0; }` — **the keep/hide flip**: this is where givens are decided and where a *solved* board becomes a *puzzle*.

**Block: the uniqueness gate (L95–96) — the most important block in the project**
⭐ L95 `if (SolutionCounter.countSolutions(puzzle, inequalities, 2) == 1) { return new Puzzle(...); }` — **one line is the entire quality guarantee**: count solutions (bailing at 2), and ship the puzzle *only* if exactly one exists. Without this line you'd occasionally hand out ambiguous or unsolvable boards; with it, "PuzzleGenerator status achieved" is provable.

**Block: the fallback (L102–109)**
If every attempt fails (in practice, never): the classic 4×4 gets the hand-made puzzle; extended sizes get a fully-revealed board, which is trivially unique.
⭐ L102 `if (n == N) return createFallback();` — **the game can never crash**: there is always a last resort that is known-good.

**Block: `createFallback` (L125–152)**
A hard-coded 4×4 solution, an 8-given mask, and six signs.
⭐ the `boolean[][] givens = {...}` array — the givens were chosen so the row/column logic *forces* the unique solution; the method's comment shows the grid so anyone can verify it by hand.

---

## 8. `GeneratorTests.java` — the proof (Zwivhuya)

**Block: setup (L17–21)**
A fresh generator, the three difficulties, 50 puzzles each, two counters.
⭐ L20 `int perDiff = 50;` — the volume knob (50 × 3 = 150 puzzles per run).

**Block: check 1 — uniqueness (L28–30)**
⭐ L30 `SolutionCounter.countSolutions(p.puzzle, p.inequalities, 2) == 1` — **re-proves uniqueness independently**: the test doesn't trust the generator's word, it re-counts the solutions itself.

**Block: check 2 — truthful signs (L33–36)**
⭐ L35 `if (!ineq.isSatisfied(p.solution)) { signsTruthful = false; break; }` — every single sign must be true against the hidden solution; one liar fails the whole puzzle.

**Block: check 3 — givens consistent (L38–46)**
⭐ L42 `if (p.givens[r][c] && p.puzzle[r][c] != p.solution[r][c]) givensOk = false;` — every number shown to the player must be the *correct* number (a puzzle that starts wrong is unplayable).

**Block: the verdict (L48–54 and L62–65)**
Prints a FAIL line for any bad puzzle, checks the fallback, then prints the total.
⭐ L64 `System.out.println((ok == total && fbUnique) ? "RESULT: PUZZLEGENERATOR STATUS ACHIEVED" : "RESULT: NOT YET");` — **the line you all look at**: 150/150 + this string is the definition of "the brain is healthy".
---

## 9. `FutoshikiGUI.java` — the face of the game (≈748 lines)

*Line numbers in this section are approximate (≈) — the file is long, so follow the block names instead.*

**Block: fields (≈L25–63)**
The three groups: puzzle data (board, solution, givens, inequalities — *copies* from the generator), game state (5 lives, the selected cell, `gameOver`), and the Swing components (cells grid, labels, boxes, board panel).
⭐ `private final PuzzleGenerator generator = new PuzzleGenerator();` — **the one line that connects the face to the brain.** The GUI never does puzzle thinking itself; it asks the brain.
⭐ `private int n = PuzzleGenerator.N;` — the *current* board size; it can change (extended mode), so it's a field, not a constant.

**Block: the constructor (≈L69–79)**
⭐ the call order: `buildUI(); newGame(); pack(); setLocationRelativeTo(null); setVisible(true);` — **order matters**: build the window first (the checkboxes exist), then the first puzzle (`newGame` needs those controls), then size and show. Reorder these and you'll get null pointers.

**Block: `keyHandler` (≈L84–107)**
One shared `KeyAdapter` for the whole game: top-row and numpad digits place a number, Backspace/Delete clear, arrow keys move the selection.
⭐ `if (num <= n) placeNumber(num);` — the extended-mode guard: on a 4×4 board, pressing 5 does nothing.
⭐ it's attached to the frame, every button, and the board panel — so the keyboard works no matter which widget has focus.

**Block: `buildUI` (≈L109–187)**
Lays out the window: top bar (lives label, difficulty box, New Puzzle, the two mode checkboxes), the board in the centre, and the bottom (number row, action row, status area).
⭐ `boardPanel = new BoardPanel();` — the custom canvas (next block) where cells and signs live.
⭐ `statusWrap.setPreferredSize(new Dimension(0, 100));` — **fixes the status area's height** so the longest roast fits and the non-resizable window never grows or clips mid-game.

**Block: `BoardPanel` (≈L194–260)**
A panel with **no layout manager** (`setLayout(null)`) — cells are placed by hand at exact pixels, and signs are *painted* rather than added as labels.
⭐ `t.rotate(Math.atan2(ly - my, lx - mx)); t.fillPolygon(...)` — **the sign-drawing trick**: each sign becomes a triangle rotated so its base faces the LARGER cell, which means its point always faces the smaller one (the tip convention). Because signs are painted (not widgets), they never sit on top of a cell, so clicks always reach the buttons.
⭐ `void configureFor(int size)` — picks cell/gap sizes for 4/5/6 (76/30, 61/24, 50/20) so the window stays roughly the same width at every board size.

**Block: `rebuildBoard` + `buildNumberRow` (≈L272–330)**
Recreates all cell buttons and the 1..n + Clear row — but only when the size actually changes.
⭐ `cells = new JButton[newSize][newSize];` — the whole grid is rebuilt from scratch (which is exactly why this must only run on a size change, not every new puzzle).
⭐ `b.addActionListener(e -> selectCell(fr, fc));` — **clicking a cell only selects it.** Placing a number is a separate, deliberate step (the `fr/fc` copies exist because Java lambdas need final variables).

**Block: `newGame` (≈L338–385)**
Resets state, picks the size, and gets a puzzle.
⭐ `Puzzle p = generator.generate(currentDifficulty, newSize);` — **the one call that runs the entire brain** (board → signs → hiding → uniqueness gate) and hands back a finished `Puzzle` box.
⭐ `solution = BoardFactory.deepCopy(p.solution); board = BoardFactory.deepCopy(p.puzzle);` — deep copies again: the GUI must never mutate the generator's data.
⭐ `if (cells == null || cells.length != newSize) rebuildBoard(newSize);` — cheap check: only rebuild the UI when the size changed.

**Block: `renderBoard` (≈L391–410)**
Redraws every cell button: its text from the board, grey background if it's a given, and the selection border.
⭐ `b.setBorder(selected ? BorderFactory.createLineBorder(0x2266CC, 4) : BorderFactory.createLineBorder(0x999999, 1));` — the ternary that draws the blue selection ring (or the thin grey one).

**Block: `selectCell` + `moveSelection` (≈L416–450)**
Selection rules: game over → ignored; given cell → ignored; otherwise select and redraw. Arrow keys *slide* the selection.
⭐ the `while` loop in `moveSelection`: `if (!givens[r][c]) { selectCell(r, c); return; } r += dr; c += dc;` — **the sliding selection that skips locked cells**: it marches in the arrow's direction until it finds an editable cell or falls off the board.

**Block: `placeNumber` (≈L456–510) — the heart of the game**
The move pipeline, in order: game over? → nothing selected? (message) → given cell? (ignore) → 0? (clear) → **try the move**.
⭐ `if (!Validator.isValid(board, r, c, num, inequalities)) {` — **every single move goes through Jack's referee.** The GUI never checks rules itself.
⭐ `board[r][c] = old;` — **the undo**: an illegal move is refused and taken back, so the player can keep playing.
⭐ `lives--; updateLives();` — the penalty, then the three tiers of reaction:
  - 0 lives → `gameOver = true; board = BoardFactory.deepCopy(solution);` — **reveal the answer** (another deep copy) and tell them.
  - 1 life left → the MERCY MECHANIC proclamation (the big speech).
  - otherwise → the normal "illegal move" message (or its roast).
⭐ `checkWin();` — called after every *legal* move; win-checking rides along for free.

**Block: helpers — `isSolved` / `checkWin` / `checkBoard` / `giveHint` / `resetBoard` / `updateLives` (≈L516–590)**
⭐ `if (board[r][c] != solution[r][c]) return false;` (in `isSolved`) — **winning = matching the unique hidden solution exactly**; since the puzzle has exactly one solution, this is a perfect win test.
⭐ `board[selectedRow][selectedCol] = solution[selectedRow][selectedCol];` (in `giveHint`) — a hint is literally "copy the solution's digit into the selected cell".
⭐ `new String(new char[Math.max(lives, 0)]).replace('\0', '\u2665')` (in `updateLives`) — **the hearts trick**: build a string of N zero characters, then swap every one for a ♥. Five lives = ♥♥♥♥♥.

**Block: `say` (≈L598–604)**
The message gateway — every word the game speaks to the player passes through here.
⭐ `statusLabel.setText("<html><div style='text-align:center;'>...")` — HTML mode makes long messages **word-wrap and centre** inside the fixed-height status box. One gateway = one place to change how the game talks.

**Block: `illegalReason` (≈L611–650)**
Explains *why* a move was refused — first row/column duplicates, then the violated sign, described in human words.
⭐ `boolean mustBeSmaller = (ineq.getDir() == '<') == t1;` — **a double-boolean trick** that works out whether *this* cell must be smaller or bigger (it depends which end of the sign you're standing on). This is what makes the message actually teach the rule instead of just saying "no".

**Block: the sarcastic mode writers' room (≈L656–745)**
`roasting()` checks the checkbox; `pick(...)` picks a random line from a list. Then one method per situation — wrong move, win, game over, hint, check, reset, the last-life proclamation, no selection, clear — each with difficulty-scaled strings (the design rule: **the easier the difficulty, the harsher the roast**).
⭐ `return lines[rng.nextInt(lines.length)];` (in `pick`) — **one line is what makes every roast feel fresh**: each failure gets a random quote from that situation's list.
⭐ the `switch (currentDifficulty)` at the top of each method — where "Easy is merciless, Hard earns respect" is actually implemented.
(The ~90 string lines inside are *content*, not logic — read them for the joy of it.)

**Block: `main` (≈L747–751)**
⭐ `SwingUtilities.invokeLater(() -> new FutoshikiGUI());` — **the first rule of Swing**: build the UI on the Event Dispatch Thread, never on `main`. Skip this and you get subtle threading bugs (or a crash on some platforms).
