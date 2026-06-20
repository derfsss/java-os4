import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Minesweeper - classic 9x9 / 10-mine Minesweeper for Java-OS4 (Swing).
 *
 * How to play:
 *   Left-click a cell to reveal it. Mines are placed on your FIRST click so the
 *   first click is always safe; a 0-cell flood-reveals its neighbours. Right-click
 *   a covered cell to toggle a flag. Reveal a mine and you lose; reveal every
 *   non-mine cell and you win. Use "New Game" to restart.
 *
 * Run:  jamvm-openjdk -cp Minesweeper.jar Minesweeper
 */
public class Minesweeper {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShow();
            }
        });
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("Minesweeper");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final JLabel status = new JLabel("Mines: 10", SwingConstants.CENTER);
        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));

        final Board board = new Board(status);

        JButton newGame = new JButton("New Game");
        newGame.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                board.reset();
            }
        });

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        top.add(newGame, BorderLayout.WEST);
        top.add(status, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout());
        content.add(top, BorderLayout.NORTH);
        content.add(board, BorderLayout.CENTER);

        frame.setContentPane(content);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /** The Minesweeper board: model + rendering + mouse input. */
    static class Board extends JPanel {

        private static final int SIZE = 9;
        private static final int MINES = 10;
        private static final int CELL = 44;
        private static final int MARGIN = 6;

        // Cell state flags.
        private final boolean[][] mine = new boolean[SIZE][SIZE];
        private final boolean[][] revealed = new boolean[SIZE][SIZE];
        private final boolean[][] flagged = new boolean[SIZE][SIZE];
        private final int[][] count = new int[SIZE][SIZE]; // neighbour mine count

        private boolean minesPlaced;
        private boolean gameOver;
        private boolean won;
        private int flags;
        private int revealedCount;

        private final JLabel status;
        private final Random rng = new Random();

        // Standard Minesweeper number colours (index 1..8).
        private static final Color[] NUM_COLORS = {
            null,                       // 0 unused
            new Color(0, 0, 255),       // 1 blue
            new Color(0, 128, 0),       // 2 green
            new Color(255, 0, 0),       // 3 red
            new Color(0, 0, 128),       // 4 dark blue
            new Color(128, 0, 0),       // 5 dark red
            new Color(0, 128, 128),     // 6 teal
            new Color(0, 0, 0),         // 7 black
            new Color(128, 128, 128)    // 8 grey
        };

        Board(JLabel status) {
            this.status = status;
            int dim = MARGIN * 2 + SIZE * CELL;
            setPreferredSize(new Dimension(dim, dim));
            setBackground(new Color(200, 200, 200));
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    handleClick(e);
                }
            });
            reset();
        }

        /** Start a fresh game. */
        void reset() {
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    mine[r][c] = false;
                    revealed[r][c] = false;
                    flagged[r][c] = false;
                    count[r][c] = 0;
                }
            }
            minesPlaced = false;
            gameOver = false;
            won = false;
            flags = 0;
            revealedCount = 0;
            updateStatus();
            repaint();
        }

        private void updateStatus() {
            if (won) {
                status.setText("You win!");
            } else if (gameOver) {
                status.setText("BOOM - you lose");
            } else {
                status.setText("Mines: " + (MINES - flags));
            }
        }

        private void handleClick(MouseEvent e) {
            if (gameOver) {
                return;
            }
            int col = (e.getX() - MARGIN) / CELL;
            int row = (e.getY() - MARGIN) / CELL;
            // Ignore clicks in the margin / outside the grid.
            if (e.getX() < MARGIN || e.getY() < MARGIN
                    || row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
                return;
            }

            if (SwingUtilities.isRightMouseButton(e)) {
                toggleFlag(row, col);
            } else if (SwingUtilities.isLeftMouseButton(e)) {
                revealCell(row, col);
            }
        }

        private void toggleFlag(int row, int col) {
            if (revealed[row][col]) {
                return;
            }
            if (flagged[row][col]) {
                flagged[row][col] = false;
                flags--;
            } else {
                flagged[row][col] = true;
                flags++;
            }
            updateStatus();
            repaint();
        }

        private void revealCell(int row, int col) {
            if (revealed[row][col] || flagged[row][col]) {
                return;
            }
            if (!minesPlaced) {
                placeMines(row, col);
                minesPlaced = true;
            }

            if (mine[row][col]) {
                // Hit a mine -> lose. Reveal all mines.
                revealed[row][col] = true;
                gameOver = true;
                won = false;
                for (int r = 0; r < SIZE; r++) {
                    for (int c = 0; c < SIZE; c++) {
                        if (mine[r][c]) {
                            revealed[r][c] = true;
                        }
                    }
                }
                updateStatus();
                repaint();
                return;
            }

            floodReveal(row, col);

            // Win when every non-mine cell is revealed.
            if (revealedCount == SIZE * SIZE - MINES) {
                gameOver = true;
                won = true;
                // Auto-flag all mines for a tidy finish.
                for (int r = 0; r < SIZE; r++) {
                    for (int c = 0; c < SIZE; c++) {
                        if (mine[r][c] && !flagged[r][c]) {
                            flagged[r][c] = true;
                            flags++;
                        }
                    }
                }
            }
            updateStatus();
            repaint();
        }

        /** Iterative flood fill: reveal connected cells, expanding through 0-cells. */
        private void floodReveal(int startRow, int startCol) {
            Deque<int[]> stack = new ArrayDeque<int[]>();
            stack.push(new int[] {startRow, startCol});
            while (!stack.isEmpty()) {
                int[] cell = stack.pop();
                int r = cell[0];
                int c = cell[1];
                if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) {
                    continue;
                }
                if (revealed[r][c] || mine[r][c]) {
                    continue;
                }
                // A flagged cell still gets revealed via flood; clear stale flag.
                if (flagged[r][c]) {
                    flagged[r][c] = false;
                    flags--;
                }
                revealed[r][c] = true;
                revealedCount++;
                if (count[r][c] == 0) {
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            if (dr != 0 || dc != 0) {
                                stack.push(new int[] {r + dr, c + dc});
                            }
                        }
                    }
                }
            }
        }

        /** Place MINES mines at random, never on the first-clicked (safe) cell. */
        private void placeMines(int safeRow, int safeCol) {
            int placed = 0;
            while (placed < MINES) {
                int r = rng.nextInt(SIZE);
                int c = rng.nextInt(SIZE);
                if (mine[r][c] || (r == safeRow && c == safeCol)) {
                    continue;
                }
                mine[r][c] = true;
                placed++;
            }
            // Precompute neighbour counts.
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    if (mine[r][c]) {
                        continue;
                    }
                    int n = 0;
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            if (dr == 0 && dc == 0) {
                                continue;
                            }
                            int nr = r + dr;
                            int nc = c + dc;
                            if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE
                                    && mine[nr][nc]) {
                                n++;
                            }
                        }
                    }
                    count[r][c] = n;
                }
            }
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Font numFont = new Font(Font.SANS_SERIF, Font.BOLD, 20);
            g2.setFont(numFont);

            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    int x = MARGIN + c * CELL;
                    int y = MARGIN + r * CELL;
                    drawCell(g2, r, c, x, y);
                }
            }
        }

        private void drawCell(Graphics2D g2, int r, int c, int x, int y) {
            if (revealed[r][c]) {
                // Revealed background.
                g2.setColor(new Color(220, 220, 220));
                g2.fillRect(x, y, CELL, CELL);
                g2.setColor(new Color(160, 160, 160));
                g2.drawRect(x, y, CELL, CELL);

                if (mine[r][c]) {
                    drawMine(g2, x, y);
                } else if (count[r][c] > 0) {
                    drawNumber(g2, count[r][c], x, y);
                }
            } else {
                // Covered cell: raised look with bevel highlights.
                g2.setColor(new Color(189, 189, 189));
                g2.fillRect(x, y, CELL, CELL);
                g2.setColor(Color.WHITE);
                g2.fillRect(x, y, CELL, 3);
                g2.fillRect(x, y, 3, CELL);
                g2.setColor(new Color(123, 123, 123));
                g2.fillRect(x, y + CELL - 3, CELL, 3);
                g2.fillRect(x + CELL - 3, y, 3, CELL);
                g2.setColor(new Color(120, 120, 120));
                g2.drawRect(x, y, CELL, CELL);

                if (flagged[r][c]) {
                    drawFlag(g2, x, y);
                }
            }
        }

        private void drawNumber(Graphics2D g2, int n, int x, int y) {
            Color col = (n >= 1 && n < NUM_COLORS.length) ? NUM_COLORS[n] : Color.BLACK;
            g2.setColor(col);
            String s = Integer.toString(n);
            int sw = g2.getFontMetrics().stringWidth(s);
            int ascent = g2.getFontMetrics().getAscent();
            int descent = g2.getFontMetrics().getDescent();
            int tx = x + (CELL - sw) / 2;
            int ty = y + (CELL + ascent - descent) / 2;
            g2.drawString(s, tx, ty);
        }

        private void drawMine(Graphics2D g2, int x, int y) {
            int cx = x + CELL / 2;
            int cy = y + CELL / 2;
            int rad = CELL / 4;
            // Spikes.
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(cx - rad - 3, cy, cx + rad + 3, cy);
            g2.drawLine(cx, cy - rad - 3, cx, cy + rad + 3);
            g2.drawLine(cx - rad, cy - rad, cx + rad, cy + rad);
            g2.drawLine(cx - rad, cy + rad, cx + rad, cy - rad);
            // Body.
            g2.setColor(new Color(30, 30, 30));
            g2.fillOval(cx - rad, cy - rad, rad * 2, rad * 2);
            // Highlight.
            g2.setColor(new Color(200, 200, 200));
            g2.fillOval(cx - rad / 2, cy - rad / 2, rad / 2, rad / 2);
        }

        private void drawFlag(Graphics2D g2, int x, int y) {
            int cx = x + CELL / 2;
            int topY = y + CELL / 4;
            // Pole.
            g2.setColor(new Color(40, 40, 40));
            g2.setStroke(new BasicStroke(2f));
            int poleX = cx + CELL / 6;
            int baseY = y + CELL - CELL / 4;
            g2.drawLine(poleX, topY, poleX, baseY);
            // Flag triangle pointing left from the pole.
            int[] xs = {poleX, poleX - CELL / 3, poleX};
            int[] ys = {topY, topY + CELL / 6, topY + CELL / 3};
            g2.setColor(new Color(220, 0, 0));
            g2.fillPolygon(xs, ys, 3);
            // Base.
            g2.setColor(new Color(40, 40, 40));
            g2.fillRect(poleX - CELL / 6, baseY, CELL / 3, 3);
        }
    }
}
