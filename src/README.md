# Futoshiki

**A 2D Java/Swing puzzle game** — a small grid, a few numbers, and inequality signs that *always* tell the truth.

*Team: Zwivhuya · Siya · Lisa · Jack · Karabo*

---

## What is Futoshiki?

Futoshiki (不等式, *futōshiki* — Japanese for "inequality") is a logic puzzle. Fill every cell of the grid so that **each row and each column contains every number exactly once** — think tiny Sudoku, minus the boxes. The twist: some neighbouring cells have a `<` or `>` sign between them, telling you which of the two is bigger.

**The one rule that explains every sign: the point of the sign always faces the SMALLER number.**

```
   1   <   2          ← 1 is smaller than 2

      3
       v              ← 3 is smaller than the cell below it
      4
```

It works the same for vertical signs and for the diagonal signs that appear on Medium and Hard. No arithmetic, no guessing — every puzzle we serve has **exactly one solution**, and it is always reachable with pure logic.

---

## Run it (30 seconds)

```
cd src
javac *.java
java FutoshikiGUI
```

That's it. The `src/` folder is the entire game — brain, face and tests — and it is **self-contained**: nothing outside the folder is needed.

**Requirements:** a Java 8+ JDK and a screen (it's a windowed game).

---

## How to play

**Goal:** fill the whole board correctly. Grey cells are the *givens* — they come pre-filled and can't be touched.

### Controls

- **Click a cell** to select it (blue border = selected).
- **Click a number button** to place it, or use the keyboard:
  - **Arrow keys** — move the selection (it skips over locked cells)
  - **1–4** — place a number (on a 4×4 board, keys 5–9 are ignored)
  - **Backspace / Delete** — clear the selected cell
- The **Clear** button does the same as Backspace.

### Buttons

| Button | What it does |
|---|---|
| **Check** | Counts how many of your placed numbers are wrong (without giving away which). |
| **Hint** | Fills the selected cell with the correct number. Free — but the game will judge you for it. |
| **Reset** | Wipes everything you've entered. Your lives are kept. |
| **New Puzzle** | Generates a brand new puzzle. |

### Lives and losing

You start with **5 lives** (the hearts, top-left). A move that breaks a rule — a duplicate in a row/column, or a violation of a sign — is **refused and undone, and costs one life**. The game always tells you *why* ("there's already a 3 in this row", "the sign below it says this cell must be SMALLER..."). When your last life is gone, the answer is revealed so you can study it.

### Winning

Fill the board correctly and you get "SOLVED!". Hit **New Puzzle** for the next one.

### Difficulty and board sizes

| Difficulty | Signs | Diagonal signs |
|---|---|---|
| Easy | lots (plenty of help) | none |
| Medium | moderate | one |
| Hard | few (good luck) | one |

The **Extended boards** checkbox makes Medium play on 5×5 and Hard on 6×6 (Easy stays 4×4). It applies to the *next* puzzle.

### Sarcastic mode

Tick **Sarcastic mode** and the game grows a personality. The design rule: **the easier the difficulty, the harsher the roast** — dying on Easy, the difficulty that hands you free givens, is where the Council of Eons convenes to grant you mercy "by the narrowest of margins". Dying on Hard earns you reluctant respect instead.

It's the best part. We promise.

---

## The other game: `FutoshikiGame`

The repo also contains a second, complete game — the finished course "skeleton". Same brain, different flavour and a different set of mechanics:

- You **type numbers directly into the cells**, then press **Submit**.
- **Points:** you start with 100. A hint costs 20 and tells you whether the first empty cell's number is **ODD or EVEN** — it's worked out by running the real solver on your board, so it's always consistent with your progress.
- **3 lives** (managed by `LifeManager`).
- A **timer** runs; solve and you earn a time bonus (up to +100 points) and the next puzzle loads automatically.
- Combo boxes for **difficulty** (Easy/Medium/Hard) and **size** (4/5/6).
- The console prints its QA test results on startup (`[QA] runStringTests: 9/9 passed` ...).

Run it from the repo root:

```
javac -d out src/*.java FutoshikiGame.java LifeManager.java GameLogicTest.java
java -cp out FutoshikiGame
```

Both games share the same puzzle generator and both are fully playable. (Which one is the "official" submission is still a team decision — see *Next steps*.)

---

## Project structure

```
Project1/
├── src/                          ← THE GAME (self-contained: cd src && javac *.java)
│   ├── Inequality.java           one < / > rule (cells, direction, isSatisfied)      [Siya]
│   ├── Puzzle.java               the data container: what one puzzle IS              [Siya]
│   ├── BoardFactory.java         random Latin squares + deepCopy                     [Siya]
│   ├── Validator.java            "is this placement legal?" — the single rule        [Jack]
│   ├── InequalityBuilder.java    sprinkles the truthful signs                        [Jack]
│   ├── SolutionCounter.java      backtracking solver + the uniqueness gate           [Lisa]
│   ├── PuzzleGenerator.java      orchestrator: difficulty table, pipeline, fallback  [Karabo]
│   ├── GeneratorTests.java       150-puzzle stress test (QA)                         [Zwivhuya]
│   └── FutoshikiGUI.java         the face: board, signs, buttons, sarcastic mode
│
├── FutoshikiGame.java            the finished skeleton (the OTHER game)
├── LifeManager.java              lives + game-over dialog (used by FutoshikiGame)
├── GameLogicTest.java            headless integration test of the game logic
├── SmokeTests.java               constructor smoke test
│
├── CheckWin.java                 original drafts from week 1 (see "Honest notes")
├── Validation.java               "
├── HintSolver2.java              "
└── SolverHint.java               "
```

---

## How the puzzles are made (the brain)

Every puzzle goes through a five-stage pipeline. The fun fact is that **the signs can never lie**:

1. **Make a valid board** (`BoardFactory` — Siya). Start from a cyclic pattern that is *provably* a valid board, then scramble it with operations that can never break validity: shuffle the rows, shuffle the columns, rename the digits. The result is a random Latin square.
2. **Sprinkle the signs** (`InequalityBuilder` — Jack). Every pair of neighbours gets a sign with some probability — 70% on Easy, 50% on Medium, 35% on Hard. The direction of each sign is read from the *actual solution*, so every sign tells the truth. Medium and Hard also get one diagonal sign (diagonal neighbours can hold equal numbers, so the builder looks for a pair that actually differs).
3. **Hide the cells.** A fraction of the cells stay visible as givens — 50% on Easy, 31% on Medium, 19% on Hard — and the rest are blanked for the player.
4. **The uniqueness gate** (`SolutionCounter` — Lisa). A backtracking solver counts the solutions, stopping as soon as it finds two. If the puzzle has anything other than *exactly one* solution, it's thrown away and a new candidate is generated (up to 100 attempts). This is why every puzzle we serve is unambiguous.
5. **The fallback.** If all attempts fail (in practice, never), a hand-written puzzle whose solution is forced by row/column logic alone is served instead. The game can never get stuck or crash.

Extended sizes (5×5, 6×6) run the same pipeline with more attempts allowed, because uniqueness gets rarer as the grid grows.

---

## Team allocation

Each buddy owns their files — that's how the generator was split so everyone could work at once:

| Buddy | Role | Files |
|---|---|---|
| **Siya** | Data model | `Inequality`, `Puzzle`, `BoardFactory` |
| **Jack** | Validation & rules | `Validator`, `InequalityBuilder` |
| **Lisa** | Solver & uniqueness | `SolutionCounter` |
| **Karabo** | Orchestration & lifecycle | `PuzzleGenerator` (the orchestrator) |
| **Zwivhuya** | Unit testing & QA | `GeneratorTests`, `GameLogicTest` |

The dependency chain runs in this order: `Inequality` → `Puzzle` / `BoardFactory` / `Validator` / `InequalityBuilder` → `SolutionCounter` → `PuzzleGenerator` → the GUI. Because every public API is one or two lines, all five of us could start on day one.

---

## Testing

Everything that doesn't need a screen can be verified in seconds:

```
# 1. The generator stress test (the big one)
cd src && javac *.java && java GeneratorTests
```
Generates 150 fresh puzzles (50 per difficulty) and independently re-checks each one: **exactly one solution, every sign truthful, every given consistent**. Expected output:
```
150/150 generated puzzles passed all 3 checks
RESULT: PUZZLEGENERATOR STATUS ACHIEVED
```

```
# 2. Game logic, no display needed (run from the repo root)
javac -d out src/*.java FutoshikiGame.java LifeManager.java GameLogicTest.java
java -cp out GameLogicTest
```
Runs the game's *real* win-check, solver and sign lookups against a freshly generated puzzle:
```
[QA] runStringTests: 9/9 passed
[QA] runInequalityTests: 8/8 passed
[logic] checkWinCondition(solution) = true  (correct)
[logic] solve() completes puzzle = true, result matches hidden solution = true
[logic] sign lookups: n/n found
```

Plus: `SmokeTests.java` (constructor smoke test), and `FutoshikiGame` prints its own QA lines to the console on every startup.

---

## Honest notes (read before touching things)

- **Don't run `javac *.java` in the repo root.** The four files `CheckWin.java`, `Validation.java`, `HintSolver2.java` and `SolverHint.java` are our *original* week-1 drafts. They don't compile standalone (bare methods with no class wrapper, a missing brace, non-ASCII characters in comments). Their working logic was absorbed into the game files with `SOURCE:` tags so no one's contribution is lost — please leave them in place (or move them to an `archive/` folder), don't delete them.
- **Quirks we kept on purpose.** When we finished the skeleton we were asked to preserve the existing code *including its mistakes*. In `FutoshikiGame`: `giveHint()` silently does nothing if the board is already full, and `isValidMove()` has a `board` parameter that shadows the field. Both are harmless, and both are left in so that fixing them is a *deliberate* team decision rather than a silent accident.
- **`GameLogicTest` uses `sun.misc.Unsafe`** to build a game instance without a display — a test-only trick so the test runs on any machine. The game itself never touches it.
- **Keep files pure ASCII.** The default `javac` on Windows is US-ASCII and will reject characters like em-dashes ("unmappable character (0xE2)"). This bit us twice; the fix is a plain hyphen.
- **One commit in the history was a broken merge** (conflict markers committed by accident) — it was fixed in `efb6c8b` by keeping the split-API version of `FutoshikiGame.java`. If you're learning git: when you see `<<<<<<< HEAD` in a file, fix those lines and `git add` them *before* you commit.

---

## Next steps

- **Team decision:** which game is the official entry point — `FutoshikiGUI` (the polished face) or `FutoshikiGame` (points + timer + LifeManager) — or merge the best of both (our suggestion).
- Move the four week-1 draft files into `archive/` once the team agrees.
- Ideas for later: sound effects, a move-undo button, high scores saved to a file, and an "impossible" mode where the signs are also hidden.

---

*Made by a team of five who took the uniqueness gate very seriously and the sarcastic mode too far.*
