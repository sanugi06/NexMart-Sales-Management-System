package nexmart;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class SupplierManager extends JFrame {

    private static final long serialVersionUID = 1L;
    private final String user;

    private JTextField tfId, tfName, tfContact, tfSearch;
    private JTextArea  taAddress;
    private JLabel     lblFormTitle;
    private DefaultTableModel tableModel;
    private JTable table;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new SupplierManager("admin").setVisible(true));
    }

    public SupplierManager(String user) {
        this.user = user;
        initialize();
        load("");
    }

    private void initialize() {
        setTitle("NexMart – Suppliers");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { goBack(); }
        });
        setSize(1060, 680);
        setMinimumSize(new Dimension(880, 560));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);
        root.add(Theme.createNavBar("suppliers", user, this), BorderLayout.NORTH);

        // ── Page header with CRUD buttons ─────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(20, 70, 52));
        header.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel title = new JLabel("Supplier Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnBar.setBackground(new Color(20, 70, 52));
        JButton bClear  = hBtn("Clear",  new Color(107,114,128), new Color(75,80,90));
        JButton bAdd    = hBtn("+ Add",  new Color(22,163,74),  new Color(15,130,58));
        JButton bUpdate = hBtn("Update", new Color(202,138,4),  new Color(161,110,3));
        JButton bDelete = hBtn("Delete", new Color(220,53,53),  new Color(175,30,30));
        bClear.addActionListener(e  -> clear());
        bAdd.addActionListener(e    -> add());
        bUpdate.addActionListener(e -> update());
        bDelete.addActionListener(e -> delete());
        btnBar.add(bClear); btnBar.add(bAdd); btnBar.add(bUpdate); btnBar.add(bDelete);
        header.add(btnBar, BorderLayout.EAST);

        // ── Body ─────────────────────────────────────────────────────────────
        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setBackground(Theme.BG);
        body.setBorder(new EmptyBorder(10, 14, 10, 14));

        body.add(buildTable(), BorderLayout.CENTER);
        body.add(buildSidebar(), BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BG);
        content.add(header, BorderLayout.NORTH);
        content.add(body,   BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(280, 0));
        sidebar.setBorder(new EmptyBorder(0, 10, 0, 0));
        sidebar.setBackground(Theme.BG);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(160, 210, 185), 1));

        lblFormTitle = new JLabel("  Supplier Details", SwingConstants.LEFT);
        lblFormTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblFormTitle.setForeground(Color.WHITE);
        lblFormTitle.setBackground(Theme.ACCENT);
        lblFormTitle.setOpaque(true);
        lblFormTitle.setBorder(new EmptyBorder(9, 10, 9, 10));
        card.add(lblFormTitle, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(14, 14, 8, 14));

        int r = 0;
        tfId = fld(form, r++, "Supplier ID");
        tfId.setEditable(false);
        tfId.setBackground(new Color(240, 247, 244));
        tfName    = fld(form, r++, "Name");
        tfContact = fld(form, r++, "Contact (phone)");

        form.add(lbl("Address:"), gbc(0, r, 0));
        taAddress = new JTextArea(5, 14);
        taAddress.setFont(new Font("SansSerif", Font.PLAIN, 12));
        taAddress.setForeground(new Color(15, 40, 30));
        taAddress.setBackground(Theme.FIELD_BG);
        taAddress.setLineWrap(true);
        taAddress.setWrapStyleWord(true);
        taAddress.setCaretColor(Theme.ACCENT);
        JScrollPane as = new JScrollPane(taAddress);
        as.setBorder(BorderFactory.createLineBorder(Theme.FIELD_BORDER, 1));
        GridBagConstraints gArea = gbc(1, r, 1);
        gArea.fill = GridBagConstraints.BOTH;
        gArea.weighty = 1;
        form.add(as, gArea);

        card.add(form, BorderLayout.CENTER);

        JLabel hint = new JLabel("  Click a row to select", SwingConstants.LEFT);
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(new Color(130, 160, 145));
        hint.setBorder(new EmptyBorder(6, 10, 8, 10));
        card.add(hint, BorderLayout.SOUTH);

        sidebar.add(card, BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel buildTable() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Theme.BG);

        JPanel bar = new JPanel(new BorderLayout(6, 0));
        bar.setBackground(Theme.BG);
        tfSearch = Theme.makeField();
        tfSearch.setToolTipText("Search by name or contact");
        JButton btnS = Theme.makeButton("Search", Theme.ACCENT, Theme.ACCENT_HOVER);
        btnS.addActionListener(e -> load(tfSearch.getText().trim()));
        bar.add(Theme.makeLabel("Search Suppliers:"), BorderLayout.WEST);
        bar.add(tfSearch, BorderLayout.CENTER);
        bar.add(btnS, BorderLayout.EAST);
        p.add(bar, BorderLayout.NORTH);

        String[] cols = {"ID", "Name", "Contact", "Address"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(210, 240, 228));
        table.setSelectionForeground(new Color(10, 60, 40));
        table.setDefaultRenderer(Object.class, new StripedRenderer());
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(20, 70, 52));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) fill();
        });

        JScrollPane sc = new JScrollPane(table);
        sc.getViewport().setBackground(Color.WHITE);
        sc.setBorder(BorderFactory.createLineBorder(new Color(180, 220, 200), 1));
        p.add(sc, BorderLayout.CENTER);
        return p;
    }

    // ── Data ─────────────────────────────────────────────────────────────────
    private void load(String q) {
        tableModel.setRowCount(0);
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT * FROM Suppliers WHERE name LIKE ? OR contact LIKE ? ORDER BY name")) {
            String like = "%" + q + "%";
            ps.setString(1, like); ps.setString(2, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                tableModel.addRow(new Object[]{
                    rs.getInt("supplier_id"), rs.getString("name"),
                    rs.getString("contact"), rs.getString("address")
                });
        } catch (SQLException ex) { err("Load failed:\n" + ex.getMessage()); }
    }

    private void add() {
        try {
            String name    = need(tfName, "Name");
            String contact = phone(tfContact);
            String addr    = taAddress.getText().trim();
            try (Connection con = DB.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO Suppliers (name, contact, address) VALUES (?,?,?)")) {
                ps.setString(1, name); ps.setString(2, contact); ps.setString(3, addr);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Supplier added.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clear(); load("");
        } catch (Exception ex) { err(ex.getMessage()); }
    }

    private void update() {
        if (tfId.getText().isBlank()) { err("Select a supplier first."); return; }
        try {
            String name    = need(tfName, "Name");
            String contact = phone(tfContact);
            String addr    = taAddress.getText().trim();
            try (Connection con = DB.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                     "UPDATE Suppliers SET name=?,contact=?,address=? WHERE supplier_id=?")) {
                ps.setString(1, name); ps.setString(2, contact);
                ps.setString(3, addr); ps.setInt(4, Integer.parseInt(tfId.getText()));
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Supplier updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clear(); load("");
        } catch (Exception ex) { err(ex.getMessage()); }
    }

    private void delete() {
        if (tfId.getText().isBlank()) { err("Select a supplier first."); return; }
        if (JOptionPane.showConfirmDialog(this, "Delete this supplier?", "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Suppliers WHERE supplier_id=?")) {
            ps.setInt(1, Integer.parseInt(tfId.getText()));
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Deleted.");
            clear(); load("");
        } catch (SQLException ex) { err("Delete failed (linked to products?):\n" + ex.getMessage()); }
    }

    private void fill() {
        int vr = table.getSelectedRow(); if (vr < 0) return;
        int mr = table.convertRowIndexToModel(vr);
        tfId.setText(tableModel.getValueAt(mr, 0).toString());
        tfName.setText(tableModel.getValueAt(mr, 1).toString());
        Object c = tableModel.getValueAt(mr, 2); tfContact.setText(c == null ? "" : c.toString());
        Object a = tableModel.getValueAt(mr, 3); taAddress.setText(a == null ? "" : a.toString());
        lblFormTitle.setText("  Editing: " + tableModel.getValueAt(mr, 1));
    }

    private void clear() {
        tfId.setText(""); tfName.setText(""); tfContact.setText(""); taAddress.setText("");
        table.clearSelection();
        lblFormTitle.setText("  Supplier Details");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JTextField fld(JPanel p, int row, String label) {
        p.add(lbl(label + ":"), gbc(0, row, 0));
        JTextField tf = Theme.makeField();
        tf.setFont(new Font("SansSerif", Font.PLAIN, 12));
        p.add(tf, gbc(1, row, 1));
        return tf;
    }

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(new Color(60, 100, 80));
        return l;
    }

    private GridBagConstraints gbc(int x, int y, double wx) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 4, 5, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.gridx = x; g.gridy = y; g.weightx = wx;
        return g;
    }

    private JButton hBtn(String text, Color bg, Color hover) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setOpaque(true); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(7, 16, 7, 16));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            public void mouseExited (MouseEvent e) { b.setBackground(bg);    }
        });
        return b;
    }

    private String need(JTextField tf, String f) {
        String v = tf.getText().trim();
        if (v.isEmpty()) throw new IllegalArgumentException(f + " cannot be empty.");
        return v;
    }

    private String phone(JTextField tf) {
        String v = tf.getText().trim();
        if (!v.isEmpty() && !v.matches("^[\\d+\\-() ]{7,20}$"))
            throw new IllegalArgumentException("Contact must be a valid phone number.");
        return v;
    }

    private void err(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void goBack() { dispose(); new Dashboard(user).setVisible(true); }

    private static class StripedRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, focus, row, col);
            if (!sel) setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 250, 245));
            return this;
        }
    }
}
