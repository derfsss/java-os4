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
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Checkers -- 8x8 English draughts, 2-player hotseat Swing game.
 *
 * Red (bottom) vs Black (top), 12 pieces each on the dark squares.
 * Click your own piece to select it (the piece and its legal destination
 * squares are highlighted), then click a highlighted square to move. A piece
 * may step one square forward diagonally onto an empty dark square, or JUMP
 * an adjacent opponent piece onto the empty square beyond (the jumped piece
 * is removed). Reaching the far back row promotes to a KING (drawn with an
 * inner crown ring); kings move and jump both forward and backward. If after
 * a jump the same piece can jump again, the same player must continue jumping
 * with it. Otherwise the turn alternates. A side with no pieces, or with no
 * legal move on its turn, loses.
 *
 * All input is mouse-driven; "New Game" resets. Single JFrame, custom JPanel
 * board painted with Java2D.
 *
 * How to run:  jamvm-openjdk -cp Checkers.jar Checkers
 */
public class Checkers {

    // ---- Model constants ------------------------------------------------
    static final int SIZE = 8;          // 8x8 board

    static final int EMPTY = 0;
    static final int RED_MAN = 1;       // Red, moves up (toward row 0)
    static final int RED_KING = 2;
    static final int BLACK_MAN = 3;     // Black, moves down (toward row 7)
    static final int BLACK_KING = 4;

    static boolean isRed(int p)   { return p == RED_MAN || p == RED_KING; }
    static boolean isBlack(int p) { return p == BLACK_MAN || p == BLACK_KING; }
    static boolean isKing(int p)  { return p == RED_KING || p == BLACK_KING; }
    static boolean isEmpty(int p) { return p == EMPTY; }

    /** A single legal move: from (r,c) to (r,c); jumped square (-1 if none). */
    static final class Move {
        final int fr, fc, tr, tc, jr, jc;
        final boolean jump;
        Move(int fr, int fc, int tr, int tc, int jr, int jc) {
            this.fr = fr; this.fc = fc; this.tr = tr; this.tc = tc;
            this.jr = jr; this.jc = jc;
            this.jump = (jr >= 0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShow();
            }
        });
    }

    static void createAndShow() {
        JFrame frame = new JFrame("Checkers - English Draughts");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final JLabel status = new JLabel("Red's turn", SwingConstants.CENTER);
        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        status.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        final BoardPanel board = new BoardPanel(status);

        JButton newGame = new JButton("New Game");
        newGame.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                board.reset();
            }
        });

        JPanel top = new JPanel(new BorderLayout());
        top.add(newGame, BorderLayout.WEST);
        top.add(status, BorderLayout.CENTER);
        top.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        frame.setLayout(new BorderLayout());
        frame.add(top, BorderLayout.NORTH);
        frame.add(board, BorderLayout.CENTER);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ---- The board panel + game logic -----------------------------------
    static final class BoardPanel extends JPanel {

        private final JLabel status;
        private final int[][] cell = new int[SIZE][SIZE];

        private boolean redTurn = true;
        private boolean gameOver = false;

        // selection state
        private int selR = -1, selC = -1;
        private List<Move> selMoves = new ArrayList<Move>();

        // if non-null, the player is in a forced multi-jump chain with this piece
        private int chainR = -1, chainC = -1;

        // colors
        private final Color light = new Color(0xEED9B5);
        private final Color dark = new Color(0x9B5B2E);
        private final Color redPiece = new Color(0xCC2222);
        private final Color redPieceHi = new Color(0xEE5555);
        private final Color blackPiece = new Color(0x222222);
        private final Color blackPieceHi = new Color(0x555555);
        private final Color outline = new Color(0x111111);
        private final Color selColor = new Color(0x2266FF);
        private final Color targetColor = new Color(0x33CC55);

        BoardPanel(JLabel status) {
            this.status = status;
            setPreferredSize(new Dimension(480, 480));
            setBackground(new Color(0x303030));
            reset();
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    handleClick(e);
                }
            });
        }

        void reset() {
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    cell[r][c] = EMPTY;
                    if (isDarkSquare(r, c)) {
                        if (r < 3) {
                            cell[r][c] = BLACK_MAN;   // Black at top
                        } else if (r > 4) {
                            cell[r][c] = RED_MAN;     // Red at bottom
                        }
                    }
                }
            }
            redTurn = true;
            gameOver = false;
            clearSelection();
            chainR = chainC = -1;
            updateStatus();
            repaint();
        }

        private void clearSelection() {
            selR = selC = -1;
            selMoves = new ArrayList<Move>();
        }

        private static boolean isDarkSquare(int r, int c) {
            return ((r + c) & 1) == 1;
        }

        private boolean inBounds(int r, int c) {
            return r >= 0 && r < SIZE && c >= 0 && c < SIZE;
        }

        private boolean ownsPiece(int p) {
            return redTurn ? isRed(p) : isBlack(p);
        }

        // -- geometry: keep the board square, centered -------------------
        private int boardPixels() {
            return Math.min(getWidth(), getHeight());
        }
        private int squareSize() {
            return boardPixels() / SIZE;
        }
        private int originX() {
            return (getWidth() - squareSize() * SIZE) / 2;
        }
        private int originY() {
            return (getHeight() - squareSize() * SIZE) / 2;
        }

        // -- input -------------------------------------------------------
        private void handleClick(MouseEvent e) {
            if (gameOver) {
                return;
            }
            int sq = squareSize();
            if (sq <= 0) {
                return;
            }
            int x = e.getX() - originX();
            int y = e.getY() - originY();
            if (x < 0 || y < 0) {
                return;
            }
            int c = x / sq;
            int r = y / sq;
            if (!inBounds(r, c)) {
                return;
            }

            // 1) If a piece is selected and this is a highlighted target -> move.
            if (selR >= 0) {
                Move m = findMoveTo(r, c);
                if (m != null) {
                    applyMove(m);
                    return;
                }
            }

            // 2) Otherwise try to (re)select one of the current player's pieces.
            int p = cell[r][c];
            if (ownsPiece(p)) {
                // During a forced jump chain only the chaining piece is selectable.
                if (chainR >= 0 && (r != chainR || c != chainC)) {
                    return;
                }
                selectPiece(r, c);
                return;
            }

            // 3) Click on empty / opponent / non-target: clear selection
            //    (unless we are mid-chain, where the piece stays selected).
            if (chainR < 0) {
                clearSelection();
                repaint();
            }
        }

        private void selectPiece(int r, int c) {
            List<Move> legal = legalMovesForPlayer();
            List<Move> mine = new ArrayList<Move>();
            for (Move m : legal) {
                if (m.fr == r && m.fc == c) {
                    mine.add(m);
                }
            }
            if (mine.isEmpty()) {
                // Selected a piece that has no legal move this turn: ignore.
                clearSelection();
                repaint();
                return;
            }
            selR = r;
            selC = c;
            selMoves = mine;
            repaint();
        }

        private Move findMoveTo(int r, int c) {
            for (Move m : selMoves) {
                if (m.tr == r && m.tc == c) {
                    return m;
                }
            }
            return null;
        }

        // -- applying a move ---------------------------------------------
        private void applyMove(Move m) {
            int p = cell[m.fr][m.fc];
            cell[m.fr][m.fc] = EMPTY;
            if (m.jump) {
                cell[m.jr][m.jc] = EMPTY;
            }

            // promotion: reaching the far back row
            boolean promoted = false;
            if (p == RED_MAN && m.tr == 0) {
                p = RED_KING;
                promoted = true;
            } else if (p == BLACK_MAN && m.tr == SIZE - 1) {
                p = BLACK_KING;
                promoted = true;
            }
            cell[m.tr][m.tc] = p;

            clearSelection();

            // Multi-jump continuation: same piece must keep jumping if it can.
            // (A man that just promoted ends its turn -- standard English rule.)
            if (m.jump && !promoted) {
                List<Move> more = jumpsFrom(m.tr, m.tc);
                if (!more.isEmpty()) {
                    chainR = m.tr;
                    chainC = m.tc;
                    selR = m.tr;
                    selC = m.tc;
                    selMoves = more;
                    updateStatus();
                    repaint();
                    return;
                }
            }

            // Turn ends.
            chainR = chainC = -1;
            redTurn = !redTurn;
            checkGameEnd();
            updateStatus();
            repaint();
        }

        private void checkGameEnd() {
            int redCount = 0, blackCount = 0;
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    int p = cell[r][c];
                    if (isRed(p)) {
                        redCount++;
                    } else if (isBlack(p)) {
                        blackCount++;
                    }
                }
            }
            if (redCount == 0 || blackCount == 0) {
                gameOver = true;
                return;
            }
            // Current player (the one about to move) has no legal move -> loses.
            if (legalMovesForPlayer().isEmpty()) {
                gameOver = true;
            }
        }

        // -- move generation ---------------------------------------------

        /**
         * All legal moves for the player to move. Jumps are mandatory: if any
         * jump exists, only jumps are returned.
         */
        private List<Move> legalMovesForPlayer() {
            List<Move> jumps = new ArrayList<Move>();
            List<Move> steps = new ArrayList<Move>();
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    int p = cell[r][c];
                    if (!ownsPiece(p)) {
                        continue;
                    }
                    jumps.addAll(jumpsFrom(r, c));
                    steps.addAll(stepsFrom(r, c));
                }
            }
            return !jumps.isEmpty() ? jumps : steps;
        }

        /** Directions a piece at (r,c) may travel (row deltas). */
        private int[] rowDirs(int p) {
            if (isKing(p)) {
                return new int[] { -1, 1 };
            }
            if (isRed(p)) {
                return new int[] { -1 };   // red moves up
            }
            return new int[] { 1 };        // black moves down
        }

        private List<Move> stepsFrom(int r, int c) {
            List<Move> out = new ArrayList<Move>();
            int p = cell[r][c];
            if (isEmpty(p)) {
                return out;
            }
            int[] dr = rowDirs(p);
            for (int i = 0; i < dr.length; i++) {
                for (int dc = -1; dc <= 1; dc += 2) {
                    int nr = r + dr[i];
                    int nc = c + dc;
                    if (inBounds(nr, nc) && isEmpty(cell[nr][nc])) {
                        out.add(new Move(r, c, nr, nc, -1, -1));
                    }
                }
            }
            return out;
        }

        private List<Move> jumpsFrom(int r, int c) {
            List<Move> out = new ArrayList<Move>();
            int p = cell[r][c];
            if (isEmpty(p)) {
                return out;
            }
            int[] dr = rowDirs(p);
            for (int i = 0; i < dr.length; i++) {
                for (int dc = -1; dc <= 1; dc += 2) {
                    int mr = r + dr[i];        // mid (jumped) square
                    int mc = c + dc;
                    int nr = r + 2 * dr[i];    // landing square
                    int nc = c + 2 * dc;
                    if (!inBounds(nr, nc) || !isEmpty(cell[nr][nc])) {
                        continue;
                    }
                    if (!inBounds(mr, mc)) {
                        continue;
                    }
                    int mid = cell[mr][mc];
                    boolean opponent = redTurn ? isBlack(mid) : isRed(mid);
                    if (opponent) {
                        out.add(new Move(r, c, nr, nc, mr, mc));
                    }
                }
            }
            return out;
        }

        // -- status ------------------------------------------------------
        private void updateStatus() {
            if (gameOver) {
                // The player whose turn it would be has lost; the other won.
                // (redTurn was already toggled when the turn ended, or pieces ran out.)
                String winner;
                int redCount = 0, blackCount = 0;
                for (int r = 0; r < SIZE; r++) {
                    for (int c = 0; c < SIZE; c++) {
                        int p = cell[r][c];
                        if (isRed(p)) {
                            redCount++;
                        } else if (isBlack(p)) {
                            blackCount++;
                        }
                    }
                }
                if (redCount == 0) {
                    winner = "Black wins!";
                } else if (blackCount == 0) {
                    winner = "Red wins!";
                } else {
                    // current player to move has no moves -> they lose
                    winner = (redTurn ? "Black wins!" : "Red wins!");
                }
                status.setText("Game over - " + winner);
                return;
            }
            String who = redTurn ? "Red's turn" : "Black's turn";
            if (chainR >= 0) {
                who += "  (continue jumping)";
            }
            status.setText(who);
        }

        // -- painting ----------------------------------------------------
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            int sq = squareSize();
            if (sq <= 0) {
                return;
            }
            int ox = originX();
            int oy = originY();

            // squares
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    int x = ox + c * sq;
                    int y = oy + r * sq;
                    g2.setColor(isDarkSquare(r, c) ? dark : light);
                    g2.fillRect(x, y, sq, sq);
                }
            }

            // highlight selected piece square
            if (selR >= 0) {
                int x = ox + selC * sq;
                int y = oy + selR * sq;
                g2.setColor(selColor);
                g2.setStroke(new BasicStroke(Math.max(2f, sq * 0.07f)));
                g2.drawRect(x + 2, y + 2, sq - 4, sq - 4);
            }

            // highlight legal target squares
            g2.setColor(targetColor);
            for (Move m : selMoves) {
                int x = ox + m.tc * sq;
                int y = oy + m.tr * sq;
                int pad = sq / 6;
                g2.fillOval(x + pad, y + pad, sq - 2 * pad, sq - 2 * pad);
            }

            // pieces
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    int p = cell[r][c];
                    if (isEmpty(p)) {
                        continue;
                    }
                    drawPiece(g2, ox + c * sq, oy + r * sq, sq, p,
                            r == selR && c == selC);
                }
            }

            // board border
            g2.setColor(outline);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(ox, oy, sq * SIZE, sq * SIZE);
        }

        private void drawPiece(Graphics2D g2, int x, int y, int sq, int p,
                               boolean selected) {
            int pad = sq / 8;
            int d = sq - 2 * pad;
            int cx = x + pad;
            int cy = y + pad;

            Color body;
            if (isRed(p)) {
                body = selected ? redPieceHi : redPiece;
            } else {
                body = selected ? blackPieceHi : blackPiece;
            }

            // drop shadow
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval(cx + 2, cy + 3, d, d);

            // body
            g2.setColor(body);
            g2.fillOval(cx, cy, d, d);

            // outline
            g2.setColor(outline);
            g2.setStroke(new BasicStroke(Math.max(1.5f, sq * 0.04f)));
            g2.drawOval(cx, cy, d, d);

            // king crown: inner ring
            if (isKing(p)) {
                int kp = d / 4;
                g2.setColor(new Color(0xFFD700));
                g2.setStroke(new BasicStroke(Math.max(2f, sq * 0.05f)));
                g2.drawOval(cx + kp, cy + kp, d - 2 * kp, d - 2 * kp);

                // small "K"
                g2.setColor(new Color(0xFFD700));
                Font f = new Font(Font.SANS_SERIF, Font.BOLD,
                        Math.max(9, (int) (sq * 0.28f)));
                g2.setFont(f);
                String k = "K";
                int tw = g2.getFontMetrics().stringWidth(k);
                int th = g2.getFontMetrics().getAscent();
                g2.drawString(k, cx + d / 2 - tw / 2,
                        cy + d / 2 + th / 2 - 2);
            }
        }
    }
}