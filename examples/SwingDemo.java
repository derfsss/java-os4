/*
 * SwingDemo -- a small Swing example shipped with Java-OS4.
 *
 * A window with a label, a text field and a button, running in a real
 * Intuition window via the sun.awt.amiga toolkit.  Type a name and click
 * "Greet" (or press Enter).  Uses EXIT_ON_CLOSE -- closing the window quits
 * cleanly (the close-on-exit handling fixed in 0.5.1).
 *
 *     java -cp examples/SwingDemo.jar SwingDemo
 *
 * GPLv2 (java-os4 project).
 */
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class SwingDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final JFrame f = new JFrame("Swing on AmigaOS 4");
                final JLabel greeting = new JLabel("Hello, Amiga!", JLabel.CENTER);
                greeting.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));

                final JTextField name = new JTextField("Amiga", 16);
                final JButton greet = new JButton("Greet");

                ActionListener doGreet = new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String n = name.getText().trim();
                        greeting.setText(n.isEmpty() ? "Hello!" : "Hello, " + n + "!");
                    }
                };
                greet.addActionListener(doGreet);
                name.addActionListener(doGreet);      // Enter in the field

                JPanel input = new JPanel();
                input.add(new JLabel("Name:"));
                input.add(name);
                input.add(greet);

                JPanel root = new JPanel(new BorderLayout(8, 8));
                root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
                root.add(greeting, BorderLayout.CENTER);
                root.add(input, BorderLayout.SOUTH);

                f.setContentPane(root);
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setSize(360, 180);
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            }
        });
    }
}
