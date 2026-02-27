package nexmart;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class Theme {

    // ── Palette ──────────────────────────────────────────────────────────────
    public static final Color NAV_BG        = new Color(18, 52, 45);
    public static final Color NAV_ACTIVE    = new Color(16, 124, 90);
    public static final Color NAV_HOVER_BG  = new Color(16, 124, 90);
    public static final Color NAV_BTN_FG    = new Color(210, 240, 230);
    public static final Color NAV_LOGOUT    = new Color(220, 80, 60);

    public static final Color ACCENT        = new Color(16, 124, 90);
    public static final Color ACCENT_HOVER  = new Color(10, 95, 68);
    public static final Color ACCENT_LIGHT  = new Color(220, 248, 238);

    public static final Color BG            = new Color(243, 247, 245);
    public static final Color CARD_BG       = Color.WHITE;

    public static final Color BTN_GREEN     = new Color(22, 163, 74);
    public static final Color BTN_GREEN_H   = new Color(15, 130, 58);
    public static final Color BTN_AMBER     = new Color(202, 138, 4);
    public static final Color BTN_AMBER_H   = new Color(161, 110, 3);
    public static final Color BTN_RED       = new Color(220, 53, 53);
    public static final Color BTN_RED_H     = new Color(175, 30, 30);
    public static final Color BTN_GRAY      = new Color(107, 114, 128);
    public static final Color BTN_GRAY_H    = new Color(75, 80, 90);

    public static final Color FIELD_BORDER  = new Color(174, 214, 196);
    public static final Color FIELD_BG      = Color.WHITE;
    public static final Color LABEL_FG      = new Color(30, 60, 45);
    public static final Color TITLE_FG      = new Color(15, 45, 35);

    public static final Font  NAV_FONT      = new Font("SansSerif", Font.BOLD, 13);

    // ── Navigation bar ───────────────────────────────────────────────────────
    public static JPanel createNavBar(String current, String user, JFrame owner) {
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 4));
        nav.setBackground(NAV_BG);
        nav.setPreferredSize(new Dimension(Short.MAX_VALUE, 48));

        JLabel brand = new JLabel("  NexMart");
        brand.setFont(new Font("SansSerif", Font.BOLD, 16));
        brand.setForeground(new Color(100, 220, 170));
        brand.setBorder(new EmptyBorder(0, 4, 0, 16));
        nav.add(brand);

        String[][] items = {
            {"Home",      "dashboard"},
            {"Products",  "products"},
            {"Suppliers", "suppliers"},
            {"Sales",     "sales"},
            {"Inventory", "inventory"},
            {"Reports",   "reports"},
        };

        for (String[] it : items) {
            boolean active = it[1].equals(current);
            JButton btn = navBtn(it[0], active);
            btn.addActionListener(e -> navigate(it[1], user, owner));
            nav.add(btn);
        }

        JPanel sp = new JPanel();
        sp.setOpaque(false);
        sp.setPreferredSize(new Dimension(160, 10));
        nav.add(sp);

        JButton logout = navBtn("Logout", false);
        logout.setForeground(new Color(255, 140, 130));
        logout.addActionListener(e -> {
            int rc = JOptionPane.showConfirmDialog(owner,
                "Log out of NexMart?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
            if (rc == JOptionPane.YES_OPTION) {
                owner.dispose();
                new LoginScreen().setVisible(true);
            }
        });
        nav.add(logout);
        return nav;
    }

    private static JButton navBtn(String label, boolean active) {
        JButton b = new JButton(label);
        b.setFont(NAV_FONT);
        b.setForeground(NAV_BTN_FG);
        b.setBackground(active ? NAV_ACTIVE : NAV_BG);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(7, 14, 7, 14));
        Color hover = active ? NAV_ACTIVE.darker() : NAV_HOVER_BG;
        Color rest  = active ? NAV_ACTIVE           : NAV_BG;
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            public void mouseExited (MouseEvent e) { b.setBackground(rest);  }
        });
        return b;
    }

    public static void navigate(String action, String user, JFrame owner) {
        switch (action) {
            case "dashboard"  -> { owner.dispose(); new Dashboard(user).setVisible(true); }
            case "products"   -> { owner.dispose(); new ProductManager(user).setVisible(true); }
            case "suppliers"  -> { owner.dispose(); new SupplierManager(user).setVisible(true); }
            case "sales"      -> { owner.dispose(); new SalesScreen(user).setVisible(true); }
            case "inventory"  -> { owner.dispose(); new InventoryScreen(user).setVisible(true); }
            case "reports"    -> { owner.dispose(); new ReportsScreen(user).setVisible(true); }
        }
    }

    // ── Widget helpers ───────────────────────────────────────────────────────
    public static JButton makeButton(String text, Color bg, Color hover) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(9, 20, 9, 20));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            public void mouseExited (MouseEvent e) { b.setBackground(bg);    }
        });
        return b;
    }

    public static JTextField makeField() {
        JTextField tf = new JTextField();
        styleField(tf);
        return tf;
    }

    public static JTextField makeField(String text) {
        JTextField tf = new JTextField(text);
        styleField(tf);
        return tf;
    }

    public static void styleField(JTextField tf) {
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setForeground(new Color(15, 40, 30));
        tf.setBackground(FIELD_BG);
        tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BORDER, 1),
            BorderFactory.createEmptyBorder(7, 10, 7, 10)));
    }

    public static void styleField(JPasswordField pf) {
        pf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        pf.setForeground(new Color(15, 40, 30));
        pf.setBackground(FIELD_BG);
        pf.setCaretColor(ACCENT);
        pf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BORDER, 1),
            BorderFactory.createEmptyBorder(7, 10, 7, 10)));
    }

    public static JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        l.setForeground(LABEL_FG);
        return l;
    }

    public static JPanel makeCard(String title) {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(160, 210, 185), 1), title,
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12), ACCENT),
            new EmptyBorder(6, 10, 10, 10)));
        return p;
    }
}
