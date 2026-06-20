import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Connect Four (Java 8 Swing, single file).
 *
 * 7 columns x 6 rows. You are RED, the computer is YELLOW. Click anywhere in a
 * column to drop your disc into that column's lowest empty slot. The computer
 * then replies with a depth-limited minimax move (take an immediate win, block
 * your immediate win, otherwise play a central/strong square). Four in a row
 * horizontally, vertically or on either diagonal wins; a full board is a draw.
 *
 * Build:  javac -source 8 -target 8 -encoding UTF-8 ConnectFour.java
 * Run:    jamvm-openjdk -cp ConnectFour.jar ConnectFour
 *         (or:  java ConnectFour)
 */
public class ConnectFour {

    /* ---- model constants ---- */
    private static final int COLS = 7;
    private static final int ROWS = 6;
    private static final int EMPTY = 0;
    private static final int RED = 1;     // human
    private static final int YELLOW = 2;  // computer
    private static final int WIN_SCORE = 1000000;

    /* board[c][r] : column c, row r where r=0 is the BOTTOM row */
    private final int[][] board = new int[COLS][ROWS];

    private boolean gameOver = false;
    private boolean humanTurn = true;
    private int[] winLineC = null; // 4 winning cells (cols) or null
    private int[] winLineR = null; // 4 winning cells (rows) or null

    private BoardPanel boardPanel;
    private JLabel statusLabel;
    private Timer aiTimer;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ConnectFour().createAndShowGui();
            }
        });
    }

    private void createAndShowGui() {
        JFrame frame = new JFrame("Connect Four");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        boardPanel = new BoardPanel();

        JPanel top = new JPanel(new BorderLayout());
        JButton newGame = new JButton("New Game");
        newGame.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetGame();
            }
        });
        statusLabel = new JLabel("Your turn (Red). Click a column.", SwingConstants.CENTER);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        top.add(newGame, BorderLayout.WEST);
        top.add(statusLabel, BorderLayout.CENTER);

        frame.setLayout(new BorderLayout());
        frame.add(top, BorderLayout.NORTH);
        frame.add(boardPanel, BorderLayout.CENTER);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void resetGame() {
        if (aiTimer != null && aiTimer.isRunning()) {
            aiTimer.stop();
        }
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                board[c][r] = EMPTY;
            }
        }
        gameOver = false;
        humanTurn = true;
        winLineC = null;
        winLineR = null;
        statusLabel.setText("Your turn (Red). Click a column.");
        boardPanel.repaint();
    }

    /* drop disc of given player into lowest empty slot of column; -1 if full */
    private int dropInColumn(int col, int player) {
        if (col < 0 || col >= COLS) {
            return -1;
        }
        for (int r = 0; r < ROWS; r++) {
            if (board[col][r] == EMPTY) {
                board[col][r] = player;
                return r;
            }
        }
        return -1;
    }

    private void handleHumanMove(int col) {
        if (gameOver || !humanTurn) {
            return;
        }
        int row = dropInColumn(col, RED);
        if (row < 0) {
            return; // column full or out of range -> ignore
        }
        boardPanel.repaint();

        if (wins(col, row, RED)) {
            captureWinLine(col, row, RED);
            gameOver = true;
            statusLabel.setText("Red wins! Click New Game.");
            boardPanel.repaint();
            return;
        }
        if (isBoardFull()) {
            gameOver = true;
            statusLabel.setText("Draw! Click New Game.");
            return;
        }

        humanTurn = false;
        statusLabel.setText("Computer is thinking...");

        // brief delay so the human's disc is visible before the reply
        aiTimer = new Timer(350, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                aiTimer.stop();
                doComputerMove();
            }
        });
        aiTimer.setRepeats(false);
        aiTimer.start();
    }

    private void doComputerMove() {
        if (gameOver) {
            return;
        }
        int col = chooseComputerColumn();
        if (col < 0) {
            // no legal move (should not happen unless full)
            if (isBoardFull()) {
                gameOver = true;
                statusLabel.setText("Draw! Click New Game.");
                boardPanel.repaint();
            }
            return;
        }
        int row = dropInColumn(col, YELLOW);
        boardPanel.repaint();

        if (row >= 0 && wins(col, row, YELLOW)) {
            captureWinLine(col, row, YELLOW);
            gameOver = true;
            statusLabel.setText("Yellow (Computer) wins! Click New Game.");
            boardPanel.repaint();
            return;
        }
        if (isBoardFull()) {
            gameOver = true;
            statusLabel.setText("Draw! Click New Game.");
            return;
        }
        humanTurn = true;
        statusLabel.setText("Your turn (Red). Click a column.");
    }

    /* ---- AI ---- */

    private List<Integer> legalColumns() {
        List<Integer> cols = new ArrayList<Integer>();
        for (int c = 0; c < COLS; c++) {
            if (board[c][ROWS - 1] == EMPTY) {
                cols.add(Integer.valueOf(c));
            }
        }
        return cols;
    }

    private int chooseComputerColumn() {
        List<Integer> legal = legalColumns();
        if (legal.isEmpty()) {
            return -1;
        }

        // 1) immediate win
        for (int i = 0; i < legal.size(); i++) {
            int c = legal.get(i).intValue();
            int r = topEmptyRow(c);
            board[c][r] = YELLOW;
            boolean win = wins(c, r, YELLOW);
            board[c][r] = EMPTY;
            if (win) {
                return c;
            }
        }

        // 2) block human's immediate win
        for (int i = 0; i < legal.size(); i++) {
            int c = legal.get(i).intValue();
            int r = topEmptyRow(c);
            board[c][r] = RED;
            boolean win = wins(c, r, RED);
            board[c][r] = EMPTY;
            if (win) {
                return c;
            }
        }

        // 3) depth-limited minimax (negamax) over the heuristic
        int bestCol = legal.get(0).intValue();
        int bestScore = Integer.MIN_VALUE;
        int depth = 5;
        for (int i = 0; i < legal.size(); i++) {
            int c = legal.get(i).intValue();
            int r = topEmptyRow(c);
            board[c][r] = YELLOW;
            int score;
            if (wins(c, r, YELLOW)) {
                score = WIN_SCORE;
            } else {
                score = -negamax(depth - 1, RED, -WIN_SCORE - 1, WIN_SCORE + 1);
            }
            board[c][r] = EMPTY;

            // mild center preference as tie-breaker
            score += centerBonus(c);

            if (score > bestScore) {
                bestScore = score;
                bestCol = c;
            }
        }
        return bestCol;
    }

    /* negamax with alpha-beta; 'player' is to move; score from the side-to-move's view */
    private int negamax(int depth, int player, int alpha, int beta) {
        if (isBoardFull()) {
            return 0;
        }
        if (depth == 0) {
            return evaluateForYellow() * (player == YELLOW ? 1 : -1);
        }

        List<Integer> legal = legalColumns();
        if (legal.isEmpty()) {
            return 0;
        }
        int best = Integer.MIN_VALUE;
        for (int i = 0; i < legal.size(); i++) {
            int c = legal.get(i).intValue();
            int r = topEmptyRow(c);
            board[c][r] = player;
            int val;
            if (wins(c, r, player)) {
                val = WIN_SCORE - (10 - depth); // prefer faster wins
            } else {
                val = -negamax(depth - 1, opponent(player), -beta, -alpha);
            }
            board[c][r] = EMPTY;

            if (val > best) {
                best = val;
            }
            if (best > alpha) {
                alpha = best;
            }
            if (alpha >= beta) {
                break; // prune
            }
        }
        return best;
    }

    private static int opponent(int player) {
        return player == YELLOW ? RED : YELLOW;
    }

    private int centerBonus(int col) {
        return (COLS / 2 - Math.abs(col - COLS / 2));
    }

    /* heuristic board evaluation from YELLOW's perspective */
    private int evaluateForYellow() {
        int score = 0;
        // center column control
        int centerCol = COLS / 2;
        for (int r = 0; r < ROWS; r++) {
            if (board[centerCol][r] == YELLOW) {
                score += 6;
            } else if (board[centerCol][r] == RED) {
                score -= 6;
            }
        }
        // all length-4 windows
        // horizontal
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                score += scoreWindow(c, r, 1, 0);
            }
        }
        // vertical
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r <= ROWS - 4; r++) {
                score += scoreWindow(c, r, 0, 1);
            }
        }
        // diagonal up-right
        for (int c = 0; c <= COLS - 4; c++) {
            for (int r = 0; r <= ROWS - 4; r++) {
                score += scoreWindow(c, r, 1, 1);
            }
        }
        // diagonal down-right
        for (int c = 0; c <= COLS - 4; c++) {
            for (int r = 3; r < ROWS; r++) {
                score += scoreWindow(c, r, 1, -1);
            }
        }
        return score;
    }

    private int scoreWindow(int c, int r, int dc, int dr) {
        int yellow = 0;
        int red = 0;
        for (int k = 0; k < 4; k++) {
            int v = board[c + dc * k][r + dr * k];
            if (v == YELLOW) {
                yellow++;
            } else if (v == RED) {
                red++;
            }
        }
        if (yellow > 0 && red > 0) {
            return 0; // mixed -> dead window
        }
        if (yellow > 0) {
            if (yellow == 4) return 10000;
            if (yellow == 3) return 50;
            if (yellow == 2) return 10;
            return 1;
        }
        if (red > 0) {
            if (red == 4) return -10000;
            if (red == 3) return -55; // slightly favour defending
            if (red == 2) return -10;
            return -1;
        }
        return 0;
    }

    private int topEmptyRow(int col) {
        for (int r = 0; r < ROWS; r++) {
            if (board[col][r] == EMPTY) {
                return r;
            }
        }
        return -1;
    }

    /* ---- win / draw detection ---- */

    private boolean isBoardFull() {
        for (int c = 0; c < COLS; c++) {
            if (board[c][ROWS - 1] == EMPTY) {
                return false;
            }
        }
        return true;
    }

    /*
     * Pure predicate: does the disc 'player' at (col,row) complete a 4-in-a-row?
     * No side effects, so it is safe to call on hypothetical probe positions
     * during the AI search. The winning-line highlight is captured separately,
     * only for confirmed real wins, by captureWinLine().
     */
    private boolean wins(int col, int row, int player) {
        int[][] dirs = { {1, 0}, {0, 1}, {1, 1}, {1, -1} };
        for (int d = 0; d < dirs.length; d++) {
            int dc = dirs[d][0];
            int dr = dirs[d][1];
            int count = 1;

            int cc = col + dc, rr = row + dr;
            while (inBounds(cc, rr) && board[cc][rr] == player) {
                count++;
                cc += dc;
                rr += dr;
            }
            cc = col - dc;
            rr = row - dr;
            while (inBounds(cc, rr) && board[cc][rr] == player) {
                count++;
                cc -= dc;
                rr -= dr;
            }

            if (count >= 4) {
                return true;
            }
        }
        return false;
    }

    /*
     * Find the actual run of 4 through (col,row) and store it for highlighting.
     * Called only after a confirmed real win on the real board.
     */
    private void captureWinLine(int col, int row, int player) {
        int[][] dirs = { {1, 0}, {0, 1}, {1, 1}, {1, -1} };
        for (int d = 0; d < dirs.length; d++) {
            int dc = dirs[d][0];
            int dr = dirs[d][1];

            List<int[]> cells = new ArrayList<int[]>();
            cells.add(new int[]{col, row});

            int cc = col + dc, rr = row + dr;
            while (inBounds(cc, rr) && board[cc][rr] == player) {
                cells.add(new int[]{cc, rr});
                cc += dc;
                rr += dr;
            }
            cc = col - dc;
            rr = row - dr;
            while (inBounds(cc, rr) && board[cc][rr] == player) {
                cells.add(0, new int[]{cc, rr});
                cc -= dc;
                rr -= dr;
            }

            if (cells.size() >= 4) {
                // first run of 4 in this direction is enough to highlight
                winLineC = new int[4];
                winLineR = new int[4];
                for (int k = 0; k < 4; k++) {
                    winLineC[k] = cells.get(k)[0];
                    winLineR[k] = cells.get(k)[1];
                }
                return;
            }
        }
        winLineC = null;
        winLineR = null;
    }

    private boolean inBounds(int c, int r) {
        return c >= 0 && c < COLS && r >= 0 && r < ROWS;
    }

    /* ---- view ---- */

    private class BoardPanel extends JPanel {
        private static final int CELL = 80;
        private static final int MARGIN = 10;

        BoardPanel() {
            int w = COLS * CELL + 2 * MARGIN;
            int h = ROWS * CELL + 2 * MARGIN;
            setPreferredSize(new Dimension(w, h));
            setBackground(new Color(20, 30, 60));
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    onClick(e);
                }
            });
        }

        private void onClick(MouseEvent e) {
            if (gameOver || !humanTurn) {
                return;
            }
            int x = e.getX();
            int boardLeft = MARGIN;
            int boardRight = MARGIN + COLS * CELL;
            if (x < boardLeft || x >= boardRight) {
                return; // outside the board horizontally
            }
            int col = (x - MARGIN) / CELL;
            if (col < 0 || col >= COLS) {
                return;
            }
            handleHumanMove(col);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            int boardW = COLS * CELL;
            int boardH = ROWS * CELL;
            int bx = MARGIN;
            int by = MARGIN;

            // blue board with a subtle gradient
            GradientPaint gp = new GradientPaint(bx, by, new Color(30, 80, 200),
                    bx, by + boardH, new Color(10, 40, 130));
            g2.setPaint(gp);
            g2.fillRoundRect(bx, by, boardW, boardH, 24, 24);

            // holes + discs (row 0 is bottom -> draw inverted)
            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    int cx = bx + c * CELL + CELL / 2;
                    int drawRow = ROWS - 1 - r;
                    int cy = by + drawRow * CELL + CELL / 2;
                    int rad = CELL / 2 - 8;

                    int v = board[c][r];
                    Color disc;
                    if (v == RED) {
                        disc = new Color(220, 50, 50);
                    } else if (v == YELLOW) {
                        disc = new Color(240, 210, 40);
                    } else {
                        disc = new Color(245, 245, 250); // empty hole
                    }

                    g2.setColor(disc);
                    g2.fillOval(cx - rad, cy - rad, rad * 2, rad * 2);

                    // shading ring
                    g2.setColor(new Color(0, 0, 0, 60));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(cx - rad, cy - rad, rad * 2, rad * 2);

                    // glossy highlight on filled discs
                    if (v != EMPTY) {
                        g2.setColor(new Color(255, 255, 255, 90));
                        int hr = rad / 2;
                        g2.fillOval(cx - hr, cy - rad + 6, hr, hr);
                    }
                }
            }

            // highlight the winning line
            if (winLineC != null && winLineR != null) {
                g2.setColor(new Color(0, 200, 0));
                g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int x0 = bx + winLineC[0] * CELL + CELL / 2;
                int y0 = by + (ROWS - 1 - winLineR[0]) * CELL + CELL / 2;
                int x1 = bx + winLineC[3] * CELL + CELL / 2;
                int y1 = by + (ROWS - 1 - winLineR[3]) * CELL + CELL / 2;
                g2.drawLine(x0, y0, x1, y1);
            }
        }
    }
}
