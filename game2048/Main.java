package game2048;

import ucb.util.CommandArgs;

import game2048.gui.Game;
import static game2048.Main.Side.*;

/** The main class for the 2048 game.
 *  @author Fangzhou Chen
 */
public class Main {

    /** Size of the board: number of rows and of columns. */
    static final int SIZE = 4;
    /** Number of squares on the board. */
    static final int SQUARES = SIZE * SIZE;

    /** The the score to reach. */
    static final int GOAL = 2048;

    /** Symbolic names for the four sides of a board. */
    static enum Side { NORTH, EAST, SOUTH, WEST };

    /** The main program.  ARGS may contain the options --seed=NUM,
     *  (random seed); --log (record moves and random tiles
     *  selected.); --testing (take random tiles and moves from
     *  standard input); and --no-display. */
    public static void main(String... args) {
        CommandArgs options =
            new CommandArgs("--seed=(\\d+) --log --testing --no-display",
                            args);
        if (!options.ok()) {
            System.err.println("Usage: java game2048.Main [ --seed=NUM ] "
                               + "[ --log ] [ --testing ] [ --no-display ]");
            System.exit(1);
        }

        Main game = new Main(options);

        while (game.play()) {
            /* No action */
        }
        System.exit(0);
    }

    /** A new Main object using OPTIONS as options (as for main). */
    Main(CommandArgs options) {
        boolean log = options.contains("--log"),
            display = !options.contains("--no-display");
        long seed = !options.contains("--seed") ? 0 : options.getLong("--seed");
        _testing = options.contains("--testing");
        _game = new Game("2048", SIZE, seed, log, display, _testing);
    }

    /** Reset the score for the current game to 0 and clear the board. */
    void clear() {
        _maxTile = 0;
        _score = 0;
        _count = 0;
        _game.clear();
        _game.setScore(_score, _maxScore);
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                _board[r][c] = 0;
            }
        }
    }

    /** Play one game of 2048, updating the maximum score. Return true
     *  iff play should continue with another game, or false to exit. */
    boolean play() {
        while (true) {

            setRandomPiece();

            if (gameOver()) {
                if (_score > _maxScore) {
                    _maxScore = _score;
                    _game.setScore(_score, _maxScore);
                }
                _game.endGame();
            }

        GetMove:
            while (true) {
                String key = _game.readKey();

                if (key.equals("\u2191")) {
                    key = "Up";
                }

                if (key.equals("\u2190")) {
                    key = "Left";
                }

                if (key.equals("\u2192")) {
                    key = "Right";
                }

                if (key.equals("\u2193")) {
                    key = "Down";
                }

                switch (key) {
                case "Up": case "Down": case "Left": case "Right":
                    if (!gameOver() && tiltBoard(keyToSide(key), true)) {
                        break GetMove;
                    }
                    break;
                case "New Game":
                    clear();
                    return true;
                case "Quit":
                    return false;
                default:
                    break;
                }
            }
            _game.setScore(_score, _maxScore);
            _game.displayMoves();
            return true;
        }
    }

    /** Return true iff the current game is over (no more moves
     *  possible). */
    boolean gameOver() {
        if (_maxTile >= GOAL) {
            return true;
        }
        if (_count == SQUARES) {
            if (tiltBoard(keyToSide("Up"), false)) {
                return false;
            }
            if (tiltBoard(keyToSide("Down"), false)) {
                return false;
            }
            if (tiltBoard(keyToSide("Left"), false)) {
                return false;
            }
            if (tiltBoard(keyToSide("Up"), false)) {
                return false;
            }
            return true;
        }
        return false;
    }

    /** Add a tile to a random, empty position, choosing a value (2 or
     *  4) at random.  Has no effect if the board is currently full. */
    void setRandomPiece() {
        boolean init = false;
        if (_count == SQUARES) {
            return;
        }
        if (_count == 0) {
            init = true;
        }

    AddEmpty:
        while (true) {
            int[] empty = _game.getRandomTile();
            int value = empty[0];
            int row = empty[1];
            int column = empty[2];

            try {
                _game.addTile(value, row, column);
                _board[row][column] = value;
                break AddEmpty;
            } catch (IllegalArgumentException e) { continue; }
        }
        _count += 1;

        if (init) {
            setRandomPiece();
        }
    }

    /** Perform the result of tilting the board toward SIDE.
     *  Returns true iff the tilt changes the board. Also do
     *  the actual moving iff IFMOVE is true.
     **/
    boolean tiltBoard(Side side, boolean ifMove) {
        /* As a suggestion (see the project text), you might try copying
         * the board to a local array, turning it so that edge SIDE faces
         * north.  That way, you can re-use the same logic for all
         * directions.  (As usual, you don't have to). */
        boolean moved = false;
        int[][] board = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                board[r][c] =
                    _board[tiltRow(side, r, c)][tiltCol(side, r, c)];
            }
        }
        for (int column = 0; column < SIZE; column += 1) {
            int[] mergedlist = new int[4];
            for (int row = 1; row < SIZE; row += 1) {
                int val = board[row][column];
                if (val != 0) {
                    int originalRow = tiltRow(side, row, column);
                    int originalCol = tiltCol(side, row, column);
                    int newRow = row;
                    int newCol = column;
                    for (int i = row - 1; i >= 0; i -= 1) {
                        if (board[i][newCol] != val) {
                            if (board[i][newCol] == 0) {
                                newRow = i;
                            } else {
                                break;
                            }
                        } else {
                            if (mergedlist[i] == 1) {
                                break;
                            }
                            newRow = i;
                            mergedlist[i] = 1;
                            break;
                        }
                    }
                    if (row != newRow) {
                        moved = true;
                    }
                    int tiltedNewrow = tiltRow(side, newRow, newCol);
                    int tiltedNewcol = tiltCol(side, newRow, newCol);
                    int[] rowinfo = {originalRow, originalCol,
                                     tiltedNewrow, tiltedNewcol,
                                     newRow, newCol, row, column, val};
                    if (ifMove) {
                        updateBoard(rowinfo, board);
                    }
                }
            }
        }
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                _board[tiltRow(side, r, c)][tiltCol(side, r, c)]
                    = board[r][c];
            }
        }
        return moved;
    }

    /** This method updates the board, both frontend and backend,
     *  INFO gives the row number information and BOARD is the
     *  board to be updated.
     */
    void updateBoard(int[] info, int[][] board) {

        int originalRow = info[0];
        int originalCol = info[1];
        int tiltedNewrow = info[2];
        int tiltedNewcol = info[3];
        int newRow = info[4];
        int newCol = info[5];
        int row = info[6];
        int column = info[7];
        int val = info[8];

        boolean merged = false;
        try {
            _game.moveTile(val, originalRow, originalCol,
                           tiltedNewrow, tiltedNewcol);
        } catch (java.lang.IllegalArgumentException e) {
            _game.mergeTile(val, val * 2, originalRow, originalCol,
                                      tiltedNewrow, tiltedNewcol);
            _count -= 1;
            merged = true;
        }
        if (merged) {
            board[row][column] = 0;
            board[newRow][newCol] = val * 2;
            if (val * 2 > _maxTile) {
                _maxTile = val * 2;
            }
            _score += val * 2;
        } else {
            board[row][column] = 0;
            board[newRow][newCol] = val;
        }
    }




    /** Return the row number on a playing board that corresponds to row R
     *  and column C of a board turned so that row 0 is in direction SIDE (as
     *  specified by the definitions of NORTH, EAST, etc.).  So, if SIDE
     *  is NORTH, then tiltRow simply returns R (since in that case, the
     *  board is not turned).  If SIDE is WEST, then column 0 of the tilted
     *  board corresponds to row SIZE - 1 of the untilted board, and
     *  tiltRow returns SIZE - 1 - C. */
    int tiltRow(Side side, int r, int c) {
        switch (side) {
        case NORTH:
            return r;
        case EAST:
            return c;
        case SOUTH:
            return SIZE - 1 - r;
        case WEST:
            return SIZE - 1 - c;
        default:
            throw new IllegalArgumentException("Unknown direction");
        }
    }

    /** Return the column number on a playing board that corresponds to row
     *  R and column C of a board turned so that row 0 is in direction SIDE
     *  (as specified by the definitions of NORTH, EAST, etc.). So, if SIDE
     *  is NORTH, then tiltCol simply returns C (since in that case, the
     *  board is not turned).  If SIDE is WEST, then row 0 of the tilted
     *  board corresponds to column 0 of the untilted board, and tiltCol
     *  returns R. */
    int tiltCol(Side side, int r, int c) {
        switch (side) {
        case NORTH:
            return c;
        case EAST:
            return SIZE - 1 - r;
        case SOUTH:
            return SIZE - 1 - c;
        case WEST:
            return r;
        default:
            throw new IllegalArgumentException("Unknown direction");
        }
    }

    /** Return the side indicated by KEY ("Up", "Down", "Left",
     *  or "Right"). */
    Side keyToSide(String key) {
        switch (key) {
        case "Up":
            return NORTH;
        case "Down":
            return SOUTH;
        case "Left":
            return WEST;
        case "Right":
            return EAST;
        default:
            throw new IllegalArgumentException("unknown key designation");
        }
    }

    /** Represents the board: _board[r][c] is the tile value at row R,
     *  column C, or 0 if there is no tile there. */
    private final int[][] _board = new int[SIZE][SIZE];

    /** */
    private int _maxTile;
    /** True iff --testing option selected. */
    private boolean _testing;
    /** THe current input source and output sink. */
    private Game _game;
    /** The score of the current game, and the maximum final score
     *  over all games in this session. */
    private int _score, _maxScore;
    /** Number of tiles on the board. */
    private int _count;
}
