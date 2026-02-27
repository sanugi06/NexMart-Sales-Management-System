package nexmart;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.Properties;
import javax.swing.*;
import javax.swing.border.*;

public class LoginScreen extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String PREFS_FILE =
        System.getProperty("user.home") + "/nexmart_prefs.properties";

    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JCheckBox      chkRemember;

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        EventQueue.invokeLater(() -> new LoginScreen().setVisible(true));
    }

    public LoginScreen() {
        initialize();
        loadSaved();
    }

    private void initialize() {
        setTitle("NexMart SMS – Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(860, 540);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridLayout(1, 2, 0, 0));
        setContentPane(root);
        root.add(buildBrandPanel());
        root.add(buildFormPanel());
    }

    // ── Left side: brand / hero ────────────────────────────────────────────────
    private JPanel buildBrandPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.NAV_BG);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(Theme.NAV_BG);
        inner.setBorder(new EmptyBorder(0, 40, 0, 40));

        // Logo icon area
        JLabel icon = new JLabel("N", SwingConstants.CENTER);
        icon.setFont(new Font("SansSerif", Font.BOLD, 64));
        icon.setForeground(new Color(100, 220, 170));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 220, 170), 3),
            new EmptyBorder(14, 30, 14, 30)));

        JLabel brand = new JLabel("NexMart", SwingConstants.CENTER);
        brand.setFont(new Font("SansSerif", Font.BOLD, 32));
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagline = new JLabel("Sales Management System", SwingConstants.CENTER);
        tagline.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tagline.setForeground(new Color(160, 210, 190));
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(60, 110, 90));
        sep.setMaximumSize(new Dimension(200, 2));

        JLabel feat1 = featureLabel("Inventory & Stock Tracking");
        JLabel feat2 = featureLabel("Sales & Customer Management");
        JLabel feat3 = featureLabel("Supplier Relations");
        JLabel feat4 = featureLabel("JasperReports Integration");

        inner.add(Box.createVerticalGlue());
        inner.add(icon);
        inner.add(Box.createVerticalStrut(18));
        inner.add(brand);
        inner.add(Box.createVerticalStrut(6));
        inner.add(tagline);
        inner.add(Box.createVerticalStrut(24));
        inner.add(sep);
        inner.add(Box.createVerticalStrut(20));
        inner.add(feat1);
        inner.add(Box.createVerticalStrut(8));
        inner.add(feat2);
        inner.add(Box.createVerticalStrut(8));
        inner.add(feat3);
        inner.add(Box.createVerticalStrut(8));
        inner.add(feat4);
        inner.add(Box.createVerticalGlue());

        p.add(inner);
        return p;
    }

    private JLabel featureLabel(String text) {
        JLabel l = new JLabel("✔  " + text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(new Color(160, 210, 190));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    // ── Right side: login form ─────────────────────────────────────────────────
    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(0, 50, 0, 50));

        JLabel welcome = new JLabel("Welcome back");
        welcome.setFont(new Font("SansSerif", Font.BOLD, 26));
        welcome.setForeground(Theme.TITLE_FG);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Sign in to your account");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(new Color(120, 150, 135));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Username
        JLabel lblUser = Theme.makeLabel("Username");
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField = Theme.makeField();
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Password
        JLabel lblPass = Theme.makeLabel("Password");
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField = new JPasswordField();
        Theme.styleField(passwordField);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Remember me
        chkRemember = new JCheckBox("Remember me");
        chkRemember.setBackground(Color.WHITE);
        chkRemember.setForeground(new Color(90, 120, 105));
        chkRemember.setFont(new Font("SansSerif", Font.PLAIN, 12));
        chkRemember.setFocusPainted(false);
        chkRemember.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Sign in button
        JButton btnLogin = Theme.makeButton("Sign In", Theme.ACCENT, Theme.ACCENT_HOVER);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.addActionListener(e -> handleLogin());

        form.add(Box.createVerticalGlue());
        form.add(welcome);
        form.add(Box.createVerticalStrut(6));
        form.add(sub);
        form.add(Box.createVerticalStrut(32));
        form.add(lblUser);
        form.add(Box.createVerticalStrut(6));
        form.add(usernameField);
        form.add(Box.createVerticalStrut(18));
        form.add(lblPass);
        form.add(Box.createVerticalStrut(6));
        form.add(passwordField);
        form.add(Box.createVerticalStrut(12));
        form.add(chkRemember);
        form.add(Box.createVerticalStrut(24));
        form.add(btnLogin);
        form.add(Box.createVerticalGlue());

        p.add(form);
        getRootPane().setDefaultButton(btnLogin);
        return p;
    }

    // ── Logic ──────────────────────────────────────────────────────────────────
    private void handleLogin() {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter both username and password.",
                "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (authenticate(u, p)) {
                if (chkRemember.isSelected()) save(u, p); else clearSaved();
                dispose();
                new Dashboard(u).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Incorrect username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Database error:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean authenticate(String u, String p) throws SQLException {
        String sql = "SELECT user_id FROM Users WHERE username = ? AND password = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u); ps.setString(2, p);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private void save(String u, String p) {
        try (FileOutputStream fos = new FileOutputStream(PREFS_FILE)) {
            Properties pr = new Properties();
            pr.setProperty("username", u);
            pr.setProperty("password", p);
            pr.store(fos, "NexMart saved credentials");
        } catch (IOException ignored) {}
    }

    private void clearSaved() { new File(PREFS_FILE).delete(); }

    private void loadSaved() {
        File f = new File(PREFS_FILE);
        if (!f.exists()) return;
        try (FileInputStream fis = new FileInputStream(f)) {
            Properties pr = new Properties();
            pr.load(fis);
            usernameField.setText(pr.getProperty("username", ""));
            passwordField.setText(pr.getProperty("password", ""));
            chkRemember.setSelected(true);
        } catch (IOException ignored) {}
    }
}
