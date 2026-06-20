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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * TicTacToe — 3x3 Tic-Tac-Toe Swing game for Java-OS4 (JamVM / OpenJDK 8 / AmigaOS 4).
 *
 * Human plays X (click an empty cell); the computer plays O and is UNBEATABLE
 * (minimax). The grid and the X / O marks are drawn programmatically with
 * Graphics2D. A "New Game" button resets; a status label shows the game state.
 *
 * How to run on Java-OS4:   jamvm-openjdk -cp TicTacToe.jar TicTacToe
 * How to run on a desktop:  javac TicTacToe.java && java TicTacToe
 */
public class TicTacToe {

    /** Cell contents. EMPTY = 0, X (human) = 1, O (computer) = 2. */
    static final int EMPTY = 0;
    static final int HUMAN = 1;   // X
    static final int COMPUTER = 2; // O

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGui();
            }
        });
    }

    static void createAndShowGui() {
        JFrame frame = new JFrame("Tic-Tac-Toe");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final JLabel status = new JLabel("Your turn", SwingConstants.CENTER);
        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        status.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        final BoardPanel board = new BoardPanel(status);

        JButton newGame = new JButton("New Game");
        newGame.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                board.reset();
            }
        });

        JPanel top = new JPanel();
        top.add(newGame);

        frame.setLayout(new BorderLayout());
        frame.add(top, BorderLayout.NORTH);
        frame.add(board, BorderLayout.CENTER);
        frame.add(status, BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /** The 3x3 board: drawing + mouse input + game logic. */
    static class BoardPanel extends JPanel {

        private final int[] cells = new int[9]; // index = row * 3 + col
        private boolean gameOver = false;
        private int winner = EMPTY;            // EMPTY (none/draw), HUMAN, or COMPUTER
        private int[] winLine = null;          // the three winning indices, for highlight
        private final JLabel status;

        BoardPanel(JLabel status) {
            this.status = status;
            setPreferredSize(new Dimension(420, 420));
            setBackground(Color.WHITE);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    handleClick(e);
                }
            });
        }

        void reset() {
            for (int i = 0; i < cells.length; i++) {
                cells[i] = EMPTY;
            }
            gameOver = false;
            winner = EMPTY;
            winLine = null;
            status.setText("Your turn");
            repaint();
        }

        private void handleClick(MouseEvent e) {
            if (gameOver) {
                return;
            }

            int size = Math.min(getWidth(), getHeight());
            int originX = (getWidth() - size) / 2;
            int originY = (getHeight() - size) / 2;
            int cell = size / 3;
            if (cell <= 0) {
                return;
            }

            int x = e.getX() - originX;
            int y = e.getY() - originY;
            if (x < 0 || y < 0 || x >= cell * 3 || y >= cell * 3) {
                return; // click outside the board
            }

            int col = x / cell;
            int row = y / cell;
            int idx = row * 3 + col;
            if (idx < 0 || idx >= 9 || cells[idx] != EMPTY) {
                return; // illegal target
            }

            // Human move.
            cells[idx] = HUMAN;
            if (checkEndState()) {
                repaint();
                return;
            }

            // Computer (minimax) reply.
            int move = bestComputerMove();
            if (move >= 0) {
                cells[move] = COMPUTER;
            }
            checkEndState();
            repaint();
        }

        /**
         * Evaluates the board after a move and updates status / gameOver / winner.
         * @return true if the game has ended.
         */
        private boolean checkEndState() {
            int[] line = findWinningLine(cells);
            if (line != null) {
                winner = cells[line[0]];
                winLine = line;
                gameOver = true;
                status.setText(winner == HUMAN ? "X wins!" : "O wins!");
                return true;
            }
            if (isFull(cells)) {
                winner = EMPTY;
                winLine = null;
                gameOver = true;
                status.setText("Draw");
                return true;
            }
            status.setText("Your turn");
            return false;
        }

        // ---- Game-state helpers ----

        private static final int[][] LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // rows
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // cols
            {0, 4, 8}, {2, 4, 6}             // diagonals
        };

        private static int[] findWinningLine(int[] b) {
            for (int[] line : LINES) {
                int a = b[line[0]];
                if (a != EMPTY && a == b[line[1]] && a == b[line[2]]) {
                    return line;
                }
            }
            return null;
        }

        private static boolean isFull(int[] b) {
            for (int v : b) {
                if (v == EMPTY) {
                    return false;
                }
            }
            return true;
        }

        // ---- Minimax (computer is O and unbeatable) ----

        private int bestComputerMove() {
            int bestScore = Integer.MIN_VALUE;
            int bestMove = -1;
            for (int i = 0; i < 9; i++) {
                if (cells[i] == EMPTY) {
                    cells[i] = COMPUTER;
                    int score = minimax(cells, 0, false);
                    cells[i] = EMPTY;
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = i;
                    }
                }
            }
            return bestMove;
        }

        /**
         * @param maximizing true when it's COMPUTER's turn (maximize), false for HUMAN.
         * @return +score for a computer win, -score for a human win, 0 for a draw.
         *         Depth is used so the computer prefers faster wins / slower losses.
         */
        private int minimax(int[] b, int depth, boolean maximizing) {
            int[] line = findWinningLine(b);
            if (line != null) {
                int w = b[line[0]];
                if (w == COMPUTER) {
                    return 10 - depth;
                } else {
                    return depth - 10;
                }
            }
            if (isFull(b)) {
                return 0;
            }

            if (maximizing) {
                int best = Integer.MIN_VALUE;
                for (int i = 0; i < 9; i++) {
                    if (b[i] == EMPTY) {
                        b[i] = COMPUTER;
                        best = Math.max(best, minimax(b, depth + 1, false));
                        b[i] = EMPTY;
                    }
                }
                return best;
            } else {
                int best = Integer.MAX_VALUE;
                for (int i = 0; i < 9; i++) {
                    if (b[i] == EMPTY) {
                        b[i] = HUMAN;
                        best = Math.min(best, minimax(b, depth + 1, true));
                        b[i] = EMPTY;
                    }
                }
                return best;
            }
        }

        // ---- Rendering ----

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);

            int size = Math.min(getWidth(), getHeight());
            int originX = (getWidth() - size) / 2;
            int originY = (getHeight() - size) / 2;
            int cell = size / 3;
            int boardSize = cell * 3;

            // Grid lines.
            g2.setColor(new Color(60, 60, 60));
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i < 3; i++) {
                int x = originX + i * cell;
                int y = originY + i * cell;
                g2.drawLine(x, originY, x, originY + boardSize);
                g2.drawLine(originX, y, originX + boardSize, y);
            }

            // Highlight the winning line, if any.
            if (winLine != null) {
                int r0 = winLine[0] / 3;
                int c0 = winLine[0] % 3;
                int r2 = winLine[2] / 3;
                int c2 = winLine[2] % 3;
                int x1 = originX + c0 * cell + cell / 2;
                int y1 = originY + r0 * cell + cell / 2;
                int x2 = originX + c2 * cell + cell / 2;
                int y2 = originY + r2 * cell + cell / 2;
                g2.setColor(new Color(46, 160, 67));
                g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x1, y1, x2, y2);
            }

            // Marks.
            int pad = cell / 5;
            for (int idx = 0; idx < 9; idx++) {
                int row = idx / 3;
                int col = idx % 3;
                int cx = originX + col * cell;
                int cy = originY + row * cell;
                int left = cx + pad;
                int top = cy + pad;
                int span = cell - 2 * pad;

                if (cells[idx] == HUMAN) {
                    g2.setColor(new Color(33, 110, 200));
                    g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(left, top, left + span, top + span);
                    g2.drawLine(left + span, top, left, top + span);
                } else if (cells[idx] == COMPUTER) {
                    g2.setColor(new Color(210, 70, 70));
                    g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawOval(left, top, span, span);
                }
            }
        }
    }
}