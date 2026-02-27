package nexmart;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class ProductManager extends JFrame {

    private static final long serialVersionUID = 1L;
    private final String user;

    private JTextField          tfId, tfName, tfPrice, tfStock, tfReorder, tfSearch;
    private JComboBox<String>   cbCategory;
    private JComboBox<Object[]> cbSupplier;
    private DefaultTableModel   tableModel;
    private JTable              table;
    private JLabel              lblFormTitle;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new ProductManager("admin").setVisible(true));
    }

    public ProductManager(String user) {
        this.user = user;
        initialize();
        loadSuppliers();
        loadProducts("");
    }

    private void initialize() {
        setTitle("NexMart – Products");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { goBack(); }
        });
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);

        root.add(Theme.createNavBar("products", user, this), BorderLayout.NORTH);

        // ── Coloured page header bar with title + all action buttons ──────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 80, 58));
        header.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel title = new JLabel("Product Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnBar.setBackground(new Color(15, 80, 58));

        JButton bClear  = headerBtn("Clear",  new Color(107, 114, 128), new Color(75, 80, 90));
        JButton bAdd    = headerBtn("+ Add",  new Color(22, 163, 74),   new Color(15, 130, 58));
        JButton bUpdate = headerBtn("Update", new Color(202, 138, 4),   new Color(161, 110, 3));
        JButton bDelete = headerBtn("Delete", new Color(220, 53, 53),   new Color(175, 30, 30));
        bClear.addActionListener(e  -> clear());
        bAdd.addActionListener(e    -> add());
        bUpdate.addActionListener(e -> update());
        bDelete.addActionListener(e -> delete());
        btnBar.add(bClear); btnBar.add(bAdd); btnBar.add(bUpdate); btnBar.add(bDelete);
        header.add(btnBar, BorderLayout.EAST);

        // ── Body: center table + right sidebar form ───────────────────────────
        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setBackground(Theme.BG);
        body.setBorder(new EmptyBorder(10, 14, 10, 14));

        body.add(buildTablePanel(), BorderLayout.CENTER);
        body.add(buildFormSidebar(), BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BG);
        content.add(header, BorderLayout.NORTH);
        content.add(body,   BorderLayout.CENTER);

        root.add(content, BorderLayout.CENTER);
    }

    // ── Right sidebar: form ───────────────────────────────────────────────────
    private JPanel buildFormSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(290, 0));
        sidebar.setBorder(new EmptyBorder(0, 10, 0, 0));
        sidebar.setBackground(Theme.BG);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(160, 210, 185), 1));

        // Card header
        lblFormTitle = new JLabel("  Product Details", SwingConstants.LEFT);
        lblFormTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblFormTitle.setForeground(Color.WHITE);
        lblFormTitle.setBackground(Theme.ACCENT);
        lblFormTitle.setOpaque(true);
        lblFormTitle.setBorder(new EmptyBorder(9, 10, 9, 10));
        card.add(lblFormTitle, BorderLayout.NORTH);

        // Form fields
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(14, 14, 14, 14));

        int r = 0;
        tfId     = addField(form, r++, "Product ID (auto)");
        tfId.setEditable(false);
        tfId.setBackground(new Color(240, 247, 244));

        tfName    = addField(form, r++, "Product Name");
        tfPrice   = addField(form, r++, "Price (LKR)");
        tfStock   = addField(form, r++, "Stock Qty");
        tfReorder = addField(form, r++, "Reorder Level");
        tfReorder.setText("10");

        form.add(sideLabel("Category"), gbc(0, r, 0));
        cbCategory = new JComboBox<>(new String[]{
            "Groceries","Electronics","Stationery","Beverages","Household","Clothing","Other"
        });
        cbCategory.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cbCategory.setBackground(Color.WHITE);
        form.add(cbCategory, gbc(1, r++, 1));

        form.add(sideLabel("Supplier"), gbc(0, r, 0));
        cbSupplier = new JComboBox<>();
        cbSupplier.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cbSupplier.setBackground(Color.WHITE);
        cbSupplier.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                if (v instanceof Object[]) v = ((Object[]) v)[1];
                return super.getListCellRendererComponent(l, v, i, s, f);
            }
        });
        form.add(cbSupplier, gbc(1, r, 1));

        card.add(form, BorderLayout.CENTER);

        // Hint label at bottom
        JLabel hint = new JLabel("  Click a row to load it here", SwingConstants.LEFT);
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(new Color(130, 160, 145));
        hint.setBorder(new EmptyBorder(6, 10, 8, 10));
        card.add(hint, BorderLayout.SOUTH);

        sidebar.add(card, BorderLayout.CENTER);
        return sidebar;
    }

    // ── Center: search bar + table ────────────────────────────────────────────
    private JPanel buildTablePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Theme.BG);

        // Search bar
        JPanel searchBar = new JPanel(new BorderLayout(6, 0));
        searchBar.setBackground(Theme.BG);
        tfSearch = Theme.makeField();
        tfSearch.setToolTipText("Search by name or category");
        JButton btnS = Theme.makeButton("Search", Theme.ACCENT, Theme.ACCENT_HOVER);
        btnS.addActionListener(e -> loadProducts(tfSearch.getText().trim()));
        searchBar.add(Theme.makeLabel("Search Products:"), BorderLayout.WEST);
        searchBar.add(tfSearch, BorderLayout.CENTER);
        searchBar.add(btnS, BorderLayout.EAST);
        p.add(searchBar, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Name", "Price (LKR)", "Stock", "Reorder", "Category", "Supplier"};
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
        table.setGridColor(new Color(230, 242, 236));
        table.setDefaultRenderer(Object.class, new StripedRenderer());
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(15, 80, 58));
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
    private void loadProducts(String q) {
        tableModel.setRowCount(0);
        String sql =
            "SELECT p.product_id, p.name, p.price, p.stock, p.reorder_threshold, p.category, s.name AS supplier " +
            "FROM Products p LEFT JOIN Suppliers s ON p.supplier_id = s.supplier_id " +
            "WHERE p.name LIKE ? OR p.category LIKE ?";
        try (Connection con = DB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            String like = "%" + q + "%";
            ps.setString(1, like); ps.setString(2, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                tableModel.addRow(new Object[]{
                    rs.getInt("product_id"), rs.getString("name"),
                    String.format("%.2f", rs.getDouble("price")),
                    rs.getInt("stock"), rs.getInt("reorder_threshold"),
                    rs.getString("category"), rs.getString("supplier")
                });
        } catch (SQLException ex) { err(ex.getMessage()); }
    }

    private void loadSuppliers() {
        cbSupplier.removeAllItems();
        try (Connection con = DB.getConnection(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT supplier_id, name FROM Suppliers ORDER BY name")) {
            while (rs.next())
                cbSupplier.addItem(new Object[]{rs.getInt(1), rs.getString(2)});
        } catch (SQLException ex) { err(ex.getMessage()); }
    }

    private void add() {
        try {
            String name  = need(tfName, "Product Name");
            double price = posDouble(tfPrice, "Price");
            int stock    = nonNegInt(tfStock, "Stock");
            int reord    = nonNegInt(tfReorder, "Reorder Level");
            String cat   = (String) cbCategory.getSelectedItem();
            int supId    = supId();
            try (Connection con = DB.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO Products (name,price,stock,category,supplier_id,reorder_threshold) VALUES(?,?,?,?,?,?)")) {
                ps.setString(1,name); ps.setDouble(2,price); ps.setInt(3,stock);
                ps.setString(4,cat);  ps.setInt(5,supId);    ps.setInt(6,reord);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Product added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clear(); loadProducts("");
        } catch (Exception ex) { err(ex.getMessage()); }
    }

    private void update() {
        if (tfId.getText().isBlank()) { err("Click a product row first."); return; }
        try {
            int id       = Integer.parseInt(tfId.getText().trim());
            String name  = need(tfName, "Product Name");
            double price = posDouble(tfPrice, "Price");
            int stock    = nonNegInt(tfStock, "Stock");
            int reord    = nonNegInt(tfReorder, "Reorder Level");
            String cat   = (String) cbCategory.getSelectedItem();
            int supId    = supId();
            try (Connection con = DB.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                     "UPDATE Products SET name=?,price=?,stock=?,category=?,supplier_id=?,reorder_threshold=? WHERE product_id=?")) {
                ps.setString(1,name); ps.setDouble(2,price); ps.setInt(3,stock);
                ps.setString(4,cat);  ps.setInt(5,supId);    ps.setInt(6,reord); ps.setInt(7,id);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Product updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clear(); loadProducts("");
        } catch (Exception ex) { err(ex.getMessage()); }
    }

    private void delete() {
        if (tfId.getText().isBlank()) { err("Click a product row first."); return; }
        if (JOptionPane.showConfirmDialog(this, "Delete this product permanently?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Products WHERE product_id=?")) {
            ps.setInt(1, Integer.parseInt(tfId.getText().trim()));
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Product deleted.");
            clear(); loadProducts("");
        } catch (SQLException ex) { err("Cannot delete — may be linked to sales:\n" + ex.getMessage()); }
    }

    private void fill() {
        int vr = table.getSelectedRow(); if (vr < 0) return;
        int mr = table.convertRowIndexToModel(vr);
        tfId.setText(tableModel.getValueAt(mr, 0).toString());
        tfName.setText(tableModel.getValueAt(mr, 1).toString());
        tfPrice.setText(tableModel.getValueAt(mr, 2).toString());
        tfStock.setText(tableModel.getValueAt(mr, 3).toString());
        tfReorder.setText(tableModel.getValueAt(mr, 4).toString());
        cbCategory.setSelectedItem(tableModel.getValueAt(mr, 5));
        lblFormTitle.setText("  Editing: " + tableModel.getValueAt(mr, 1));
    }

    private void clear() {
        tfId.setText(""); tfName.setText(""); tfPrice.setText("");
        tfStock.setText(""); tfReorder.setText("10");
        cbCategory.setSelectedIndex(0); table.clearSelection();
        lblFormTitle.setText("  Product Details");
    }

    // ── Widget helpers ────────────────────────────────────────────────────────
    private JTextField addField(JPanel p, int row, String label) {
        p.add(sideLabel(label), gbc(0, row, 0));
        JTextField tf = Theme.makeField();
        tf.setFont(new Font("SansSerif", Font.PLAIN, 12));
        p.add(tf, gbc(1, row, 1));
        return tf;
    }

    private JLabel sideLabel(String text) {
        JLabel l = new JLabel(text + ":");
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(new Color(60, 100, 80));
        return l;
    }

    private GridBagConstraints gbc(int x, int y, double wx) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 4, 5, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = x; g.gridy = y; g.weightx = wx;
        return g;
    }

    private JButton headerBtn(String text, Color bg, Color hover) {
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

    private int supId() {
        Object item = cbSupplier.getSelectedItem();
        if (item == null) throw new IllegalArgumentException("Add at least one supplier first.");
        return (int) ((Object[]) item)[0];
    }

    private String need(JTextField tf, String f) {
        String v = tf.getText().trim();
        if (v.isEmpty()) throw new IllegalArgumentException(f + " cannot be empty.");
        return v;
    }

    private double posDouble(JTextField tf, String f) {
        try { return Double.parseDouble(tf.getText().trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(f + " must be a number."); }
    }

    private int nonNegInt(JTextField tf, String f) {
        try { return Integer.parseInt(tf.getText().trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(f + " must be a whole number."); }
    }

    private void err(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void goBack() { dispose(); new Dashboard(user).setVisible(true); }

    // ── Striped row renderer ──────────────────────────────────────────────────
    private static class StripedRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, focus, row, col);
            if (!sel) setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 250, 245));
            return this;
        }
    }
}
