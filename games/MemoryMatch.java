import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * MemoryMatch - a Memory / Concentration game on a 4x4 grid (8 pairs).
 *
 * Each card hides one of 8 distinct symbols (circle, square, triangle, diamond,
 * plus, ring, star, chevron) drawn with Graphics2D in distinct colors. Click a
 * covered card to flip it face-up; reveal two and either keep them (a match) or
 * flip them back after ~700 ms (a mismatch). Match all 8 pairs to win.
 *
 * How to run (Java-OS4):
 *   jamvm-openjdk -cp MemoryMatch.jar MemoryMatch
 * Or on a standard JDK 8:
 *   javac -source 8 -target 8 -encoding UTF-8 MemoryMatch.java
 *   java MemoryMatch
 */
public class MemoryMatch extends JFrame {

    private final BoardPanel board;
    private final JLabel status;

    public MemoryMatch() {
        super("Memory Match");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        status = new JLabel("Find all 8 pairs!", SwingConstants.CENTER);
        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        status.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        board = new BoardPanel(status);

        JButton newGame = new JButton("New Game");
        newGame.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                board.newGame();
            }
        });

        JPanel top = new JPanel(new GridLayout(1, 2, 6, 6));
        top.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
        JPanel left = new JPanel();
        left.add(newGame);
        top.add(left);
        top.add(status);

        add(top, java.awt.BorderLayout.NORTH);
        add(board, java.awt.BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    /** The 8 distinct symbol shapes. */
    private enum Symbol {
        CIRCLE, SQUARE, TRIANGLE, DIAMOND, PLUS, RING, STAR, CHEVRON
    }

    /** A single card in the model. */
    private static final class Card {
        final Symbol symbol;
        boolean faceUp;
        boolean matched;

        Card(Symbol s) {
            this.symbol = s;
        }
    }

    /**
     * The board: a 4x4 grid of cards. Handles model state, mouse input,
     * drawing, and the mismatch flip-back Timer.
     */
    private static final class BoardPanel extends JPanel {

        private static final int COLS = 4;
        private static final int ROWS = 4;
        private static final int CARD = 110;     // logical card size
        private static final int GAP = 12;       // gap between cards
        private static final int MARGIN = 14;    // outer margin

        // Distinct color per symbol.
        private static final Color[] COLORS = {
            new Color(0xE0, 0x4A, 0x4A), // CIRCLE   - red
            new Color(0x3A, 0x86, 0xE0), // SQUARE   - blue
            new Color(0x35, 0xB0, 0x5E), // TRIANGLE - green
            new Color(0xE0, 0xA8, 0x2E), // DIAMOND  - amber
            new Color(0x9B, 0x59, 0xD0), // PLUS     - purple
            new Color(0x18, 0xB0, 0xB0), // RING     - teal
            new Color(0xE0, 0x6A, 0x1F), // STAR     - orange
            new Color(0xC8, 0x3D, 0x8B)  // CHEVRON  - magenta
        };

        private final List<Card> cards = new ArrayList<Card>();
        private final JLabel status;

        private int moves;
        private int matchedPairs;
        private int firstIndex = -1;   // index of the first face-up unmatched card
        private boolean locked;        // true while a mismatch is showing
        private boolean won;

        private final Timer flipBack;

        BoardPanel(JLabel status) {
            this.status = status;
            setBackground(new Color(0x20, 0x24, 0x2C));
            int w = MARGIN * 2 + COLS * CARD + (COLS - 1) * GAP;
            int h = MARGIN * 2 + ROWS * CARD + (ROWS - 1) * GAP;
            setPreferredSize(new Dimension(w, h));

            flipBack = new Timer(700, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doFlipBack();
                }
            });
            flipBack.setRepeats(false);

            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    handleClick(e);
                }
            });

            buildDeck();
            updateStatus();
        }

        /** Build a fresh shuffled deck of 8 pairs. */
        private void buildDeck() {
            cards.clear();
            Symbol[] all = Symbol.values();
            for (int i = 0; i < all.length; i++) {
                cards.add(new Card(all[i]));
                cards.add(new Card(all[i]));
            }
            Collections.shuffle(cards);
        }

        void newGame() {
            flipBack.stop();
            buildDeck();
            moves = 0;
            matchedPairs = 0;
            firstIndex = -1;
            locked = false;
            won = false;
            updateStatus();
            repaint();
        }

        private void handleClick(MouseEvent e) {
            if (locked || won) {
                return;
            }
            int idx = cardAt(e.getX(), e.getY());
            if (idx < 0) {
                return;
            }
            Card c = cards.get(idx);
            if (c.matched || c.faceUp) {
                return; // ignore already-revealed or matched cards
            }

            c.faceUp = true;

            if (firstIndex < 0) {
                // First card of the pair.
                firstIndex = idx;
                repaint();
                return;
            }

            // Second card of the pair -> this counts as a move (a pair tried).
            moves++;
            Card first = cards.get(firstIndex);
            if (first.symbol == c.symbol) {
                // Match: keep both revealed.
                first.matched = true;
                c.matched = true;
                matchedPairs++;
                firstIndex = -1;
                if (matchedPairs == 8) {
                    won = true;
                }
                updateStatus();
                repaint();
            } else {
                // Mismatch: lock input and schedule flip-back.
                locked = true;
                updateStatus();
                repaint();
                flipBack.restart();
            }
        }

        private void doFlipBack() {
            if (firstIndex >= 0) {
                cards.get(firstIndex).faceUp = false;
            }
            // Flip back any non-matched face-up cards (the two just tried).
            for (int i = 0; i < cards.size(); i++) {
                Card c = cards.get(i);
                if (c.faceUp && !c.matched) {
                    c.faceUp = false;
                }
            }
            firstIndex = -1;
            locked = false;
            updateStatus();
            repaint();
        }

        private void updateStatus() {
            if (won) {
                status.setText("You win in " + moves + " moves!");
            } else if (locked) {
                status.setText("No match - moves: " + moves);
            } else {
                status.setText("Pairs: " + matchedPairs + "/8   Moves: " + moves);
            }
        }

        /** Return card index at pixel (x,y), or -1 if outside any card. */
        private int cardAt(int x, int y) {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    int cx = cellX(col);
                    int cy = cellY(r);
                    if (x >= cx && x < cx + CARD && y >= cy && y < cy + CARD) {
                        return r * COLS + col;
                    }
                }
            }
            return -1;
        }

        private int cellX(int col) {
            return MARGIN + col * (CARD + GAP);
        }

        private int cellY(int row) {
            return MARGIN + row * (CARD + GAP);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);

            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    int idx = r * COLS + col;
                    Card c = cards.get(idx);
                    int x = cellX(col);
                    int y = cellY(r);
                    drawCard(g2, c, x, y);
                }
            }
        }

        private void drawCard(Graphics2D g2, Card c, int x, int y) {
            int arc = 18;
            if (!c.faceUp && !c.matched) {
                // Card back.
                g2.setColor(new Color(0x2E, 0x4A, 0x78));
                g2.fillRoundRect(x, y, CARD, CARD, arc, arc);
                g2.setColor(new Color(0x53, 0x7A, 0xC0));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(x + 2, y + 2, CARD - 4, CARD - 4, arc, arc);
                // Decorative back pattern (diagonal lattice).
                g2.setColor(new Color(0x3E, 0x60, 0x9A));
                g2.setStroke(new BasicStroke(2f));
                int step = 18;
                java.awt.Shape oldClip = g2.getClip();
                g2.setClip(x + 6, y + 6, CARD - 12, CARD - 12);
                for (int d = -CARD; d < CARD; d += step) {
                    g2.drawLine(x + d, y, x + d + CARD, y + CARD);
                }
                g2.setClip(oldClip);
                return;
            }

            // Face-up (revealed or matched) card.
            Color face = c.matched ? new Color(0xEC, 0xF6, 0xEC)
                    : new Color(0xF5, 0xF5, 0xF0);
            g2.setColor(face);
            g2.fillRoundRect(x, y, CARD, CARD, arc, arc);

            g2.setStroke(new BasicStroke(c.matched ? 4f : 2.5f));
            g2.setColor(c.matched ? new Color(0x35, 0xB0, 0x5E)
                    : new Color(0xB0, 0xB0, 0xA8));
            g2.drawRoundRect(x + 2, y + 2, CARD - 4, CARD - 4, arc, arc);

            Color color = COLORS[c.symbol.ordinal()];
            drawSymbol(g2, c.symbol, color, x, y);
        }

        /** Draw a symbol centered within the card box at (x, y). */
        private void drawSymbol(Graphics2D g2, Symbol s, Color color,
                int x, int y) {
            // Inner box for the glyph.
            int pad = 24;
            int bx = x + pad;
            int by = y + pad;
            int bw = CARD - 2 * pad;
            int bh = CARD - 2 * pad;
            int cx = bx + bw / 2;
            int cy = by + bh / 2;

            g2.setColor(color);
            Stroke thick = new BasicStroke(7f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND);

            switch (s) {
                case CIRCLE: {
                    g2.fill(new Ellipse2D.Double(bx, by, bw, bh));
                    break;
                }
                case SQUARE: {
                    g2.fillRect(bx, by, bw, bh);
                    break;
                }
                case TRIANGLE: {
                    GeneralPath p = new GeneralPath();
                    p.moveTo(cx, by);
                    p.lineTo(bx + bw, by + bh);
                    p.lineTo(bx, by + bh);
                    p.closePath();
                    g2.fill(p);
                    break;
                }
                case DIAMOND: {
                    GeneralPath p = new GeneralPath();
                    p.moveTo(cx, by);
                    p.lineTo(bx + bw, cy);
                    p.lineTo(cx, by + bh);
                    p.lineTo(bx, cy);
                    p.closePath();
                    g2.fill(p);
                    break;
                }
                case PLUS: {
                    int t = bw / 3;          // arm thickness
                    g2.fillRect(cx - t / 2, by, t, bh);
                    g2.fillRect(bx, cy - t / 2, bw, t);
                    break;
                }
                case RING: {
                    g2.setStroke(new BasicStroke(bw / 5f));
                    g2.draw(new Ellipse2D.Double(bx + bw / 10.0, by + bh / 10.0,
                            bw * 0.8, bh * 0.8));
                    break;
                }
                case STAR: {
                    GeneralPath p = star(cx, cy, bw / 2.0, bw / 4.4, 5);
                    g2.fill(p);
                    break;
                }
                case CHEVRON: {
                    g2.setStroke(thick);
                    // Two stacked chevrons pointing up.
                    int q = bh / 4;
                    g2.drawPolyline(
                            new int[] {bx, cx, bx + bw},
                            new int[] {cy, cy - q, cy},
                            3);
                    g2.drawPolyline(
                            new int[] {bx, cx, bx + bw},
                            new int[] {cy + q, cy, cy + q},
                            3);
                    break;
                }
                default:
                    break;
            }
        }

        /** Build an n-pointed star path. */
        private static GeneralPath star(double cx, double cy,
                double outer, double inner, int points) {
            GeneralPath p = new GeneralPath();
            double step = Math.PI / points;
            double angle = -Math.PI / 2.0; // start at top
            for (int i = 0; i < points * 2; i++) {
                double rad = (i % 2 == 0) ? outer : inner;
                double px = cx + Math.cos(angle) * rad;
                double py = cy + Math.sin(angle) * rad;
                if (i == 0) {
                    p.moveTo(px, py);
                } else {
                    p.lineTo(px, py);
                }
                angle += step;
            }
            p.closePath();
            return p;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                MemoryMatch frame = new MemoryMatch();
                frame.setVisible(true);
            }
        });
    }
}