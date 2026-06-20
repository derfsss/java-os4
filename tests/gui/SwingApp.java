/*
 * Phase 4 Swing smoke test: a real multi-component app -- JList + JTable in
 * JScrollPanes + JTextField -- driven by REAL input (injin).
 *
 * Self-verifies: list selection by real click, table cell selection by real
 * click, real typing into the text field.  Big fixed cell sizes keep the
 * injin click targets robust (window at 80,60; inner origin ~ +5,+28).
 */
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class SwingApp {

    static JFrame frame;
    static JList<String> list;
    static JTable table;
    static JTextField field;
    static volatile boolean listPicked, cellPicked, typed;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame("Swing App");

                list = new JList<String>(new String[] {
                    "Workbench", "Shell", "MultiView", "NotePad", "Calculator" });
                list.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
                list.setFixedCellHeight(36);
                list.addListSelectionListener(
                        new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting() && list.getSelectedIndex() >= 0) {
                            listPicked = true;
                            System.out.println("[PASS] list selection: "
                                + list.getSelectedValue());
                        }
                    }
                });
                JScrollPane listScroll = new JScrollPane(list);
                listScroll.setPreferredSize(new Dimension(160, 10));

                table = new JTable(new Object[][] {
                    { "exec", "53.89", "kernel" },
                    { "dos", "53.23", "disk" },
                    { "intuition", "53.62", "GUI" },
                    { "graphics", "54.31", "render" },
                    { "keymap", "53.9", "input" } },
                    new Object[] { "Library", "Version", "Role" });
                table.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
                table.setRowHeight(28);
                table.getSelectionModel().addListSelectionListener(
                        new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                            cellPicked = true;
                            System.out.println("[PASS] table selection: row "
                                + table.getSelectedRow() + " = "
                                + table.getValueAt(table.getSelectedRow(), 0));
                        }
                    }
                });

                field = new JTextField();
                field.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
                field.getDocument().addDocumentListener(
                        new javax.swing.event.DocumentListener() {
                    public void insertUpdate(javax.swing.event.DocumentEvent e) {
                        typed = true;
                        System.out.println("[PASS] typed into field: \""
                            + field.getText() + "\"");
                    }
                    public void removeUpdate(javax.swing.event.DocumentEvent e) { }
                    public void changedUpdate(javax.swing.event.DocumentEvent e) { }
                });

                frame.setContentPane(new javax.swing.JPanel(new BorderLayout(4, 4)));
                frame.getContentPane().add(listScroll, BorderLayout.WEST);
                frame.getContentPane().add(new JScrollPane(table),
                                           BorderLayout.CENTER);
                frame.getContentPane().add(field, BorderLayout.SOUTH);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(500, 340);
                frame.setVisible(true);
            }
        });
        System.out.println("[INFO] frame up - inject input now");

        for (int i = 0; i < 280; i++) {   /* up to 140s */
            Thread.sleep(500);
            if (listPicked && cellPicked && typed)
                break;
        }
        Thread.sleep(3000);   /* final repaint for screenshots */
        System.out.println("[INFO] list=" + listPicked + " table=" + cellPicked
            + " typed=" + typed);
        if (!(listPicked && cellPicked && typed))
            System.out.println("[FAIL] smoke incomplete");

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.dispose();
            }
        });
        System.out.println("SwingApp DONE");
        System.exit(0);
    }
}
