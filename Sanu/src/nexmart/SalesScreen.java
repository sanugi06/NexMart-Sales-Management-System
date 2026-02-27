package nexmart;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class SalesScreen extends JFrame {

    private static final long serialVersionUID = 1L;
    private final String user;

    private JTextField          tfCustName, tfCustContact, tfQty, tfDiscount, tfSearch;
    private JComboBox<Object[]> cbProduct;
    private JLabel              lblStock, lblTotal, lblCartCount;
    private JSpinner            spinDate;

    private DefaultTableModel cartModel;
    private JTable            cartTable;
    private final List<Object[]> cart = new ArrayList<>();

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new SalesScreen("admin").setVisible(true));
    }

    public SalesScreen(String user) {
        this.user = user;
        initialize();
        loadProducts();
    }

    private void initialize() {
        setTitle("NexMart – Sales");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { goBack(); }
        });
        setSize(1100, 720);
        setMinimumSize(new Dimension(920, 600));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);
        root.add(Theme.createNavBar("sales", user, this), BorderLayout.NORTH);

        // ── Page header ───────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 72, 50));
        header.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel title = new JLabel("⊕  Process Sale");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        dateRow.setBackground(new Color(15, 72, 50));
        JLabel dlbl = new JLabel("Sale Date:");
        dlbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dlbl.setForeground(new Color(160, 220, 190));
        spinDate = new JSpinner(new SpinnerDateModel());
        spinDate.setEditor(new JSpinner.DateEditor(spinDate, "yyyy-MM-dd"));
        spinDate.setValue(new java.util.Date());
        spinDate.setPreferredSize(new Dimension(130, 32));

        JButton btnHistory = new JButton("View History");
        btnHistory.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnHistory.setBackground(new Color(255, 193, 7));
        btnHistory.setForeground(new Color(30, 30, 30));
        btnHistory.setOpaque(true); btnHistory.setBorderPainted(false); btnHistory.setFocusPainted(false);
        btnHistory.setBorder(new EmptyBorder(6, 12, 6, 12));
        btnHistory.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnHistory.addActionListener(e -> viewHistory());
        dateRow.add(dlbl); dateRow.add(spinDate); dateRow.add(btnHistory);
        header.add(dateRow, BorderLayout.EAST);

        // ── Two-column body ───────────────────────────────────────────────────
        JPanel body = new JPanel(new GridLayout(1, 2, 12, 0));
        body.setBackground(Theme.BG);
        body.setBorder(new EmptyBorder(10, 14, 0, 14));

        body.add(buildOrderPanel());
        body.add(buildCartPanel());

        // ── Checkout bar ──────────────────────────────────────────────────────
        JPanel checkout = new JPanel(new BorderLayout());
        checkout.setBackground(new Color(10, 60, 40));
        checkout.setBorder(new EmptyBorder(12, 20, 12, 20));

        lblTotal = new JLabel("Total:  LKR 0.00");
        lblTotal.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTotal.setForeground(new Color(100, 240, 170));
        checkout.add(lblTotal, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setBackground(new Color(10, 60, 40));
        JButton bCancel   = cBtn("Cancel",        new Color(180,50,50),  new Color(140,30,30));
        JButton bComplete = cBtn("Complete Sale ✓", new Color(22,163,74), new Color(15,130,58));
        bComplete.setFont(new Font("SansSerif", Font.BOLD, 14));
        bCancel.addActionListener(e   -> cancel());
        bComplete.addActionListener(e -> complete());
        btns.add(bCancel); btns.add(bComplete);
        checkout.add(btns, BorderLayout.EAST);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BG);
        main.add(header,   BorderLayout.NORTH);
        main.add(body,     BorderLayout.CENTER);
        main.add(checkout, BorderLayout.SOUTH);

        root.add(main, BorderLayout.CENTER);
    }

    // ── Left: order builder ───────────────────────────────────────────────────
    private JPanel buildOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Theme.BG);

        // Customer card
        JPanel custCard = card("Customer  (optional)");
        JPanel custForm = new JPanel(new GridBagLayout());
        custForm.setBackground(Color.WHITE);
        custForm.setBorder(new EmptyBorder(12, 14, 14, 14));
        custForm.add(fl("Name:"),    gbc(0,0,0)); tfCustName    = Theme.makeField(); custForm.add(tfCustName,    gbc(1,0,1));
        custForm.add(fl("Contact:"), gbc(0,1,0)); tfCustContact = Theme.makeField(); custForm.add(tfCustContact, gbc(1,1,1));
        custCard.add(custForm, BorderLayout.CENTER);
        panel.add(custCard, BorderLayout.NORTH);

        // Item picker card
        JPanel pickerCard = card("Add Item to Cart");
        JPanel pickerForm = new JPanel(new GridBagLayout());
        pickerForm.setBackground(Color.WHITE);
        pickerForm.setBorder(new EmptyBorder(12, 14, 14, 14));

        pickerForm.add(fl("Product:"), gbc(0,0,0));
        cbProduct = new JComboBox<>();
        cbProduct.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cbProduct.setBackground(Color.WHITE);
        cbProduct.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                if (v instanceof Object[]) v = ((Object[]) v)[1];
                return super.getListCellRendererComponent(l, v, i, s, f);
            }
        });
        cbProduct.addActionListener(e -> updateStock());
        GridBagConstraints gp = gbc(1,0,1); gp.gridwidth = 2; pickerForm.add(cbProduct, gp);

        pickerForm.add(fl("In Stock:"), gbc(0,1,0));
        lblStock = new JLabel("–");
        lblStock.setFont(new Font("SansSerif", Font.BOLD, 13));
        pickerForm.add(lblStock, gbc(1,1,1));

        pickerForm.add(fl("Qty:"),       gbc(0,2,0)); tfQty      = Theme.makeField("1"); pickerForm.add(tfQty,      gbc(1,2,1));
        pickerForm.add(fl("Disc %:"),    gbc(0,3,0)); tfDiscount = Theme.makeField("0"); pickerForm.add(tfDiscount, gbc(1,3,1));

        JButton btnAdd = Theme.makeButton("+ Add to Cart", new Color(22,163,74), new Color(15,130,58));
        btnAdd.setFont(new Font("SansSerif", Font.BOLD, 13));
        GridBagConstraints gBtn = gbc(0,4,1); gBtn.gridwidth = 3;
        btnAdd.addActionListener(e -> addToCart());
        pickerForm.add(btnAdd, gBtn);
        pickerCard.add(pickerForm, BorderLayout.CENTER);
        panel.add(pickerCard, BorderLayout.CENTER);

        return panel;
    }

    // ── Right: cart ───────────────────────────────────────────────────────────
    private JPanel buildCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(new Color(180, 220, 200), 1));

        // Cart header
        JPanel cartHeader = new JPanel(new BorderLayout());
        cartHeader.setBackground(new Color(20, 90, 60));
        cartHeader.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel cartTitle = new JLabel("Cart Items");
        cartTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        cartTitle.setForeground(Color.WHITE);
        lblCartCount = new JLabel("0 items");
        lblCartCount.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblCartCount.setForeground(new Color(160, 220, 190));
        cartHeader.add(cartTitle,    BorderLayout.WEST);
        cartHeader.add(lblCartCount, BorderLayout.EAST);
        panel.add(cartHeader, BorderLayout.NORTH);

        String[] cols = {"Product", "Qty", "Unit Price", "Disc %", "Subtotal (LKR)"};
        cartModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel);
        cartTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cartTable.setRowHeight(30);
        cartTable.setShowVerticalLines(false);
        cartTable.setIntercellSpacing(new Dimension(0, 1));
        cartTable.setSelectionBackground(new Color(210,240,228));
        cartTable.setDefaultRenderer(Object.class, new StripedRenderer());
        cartTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        cartTable.getTableHeader().setBackground(new Color(240, 250, 245));
        cartTable.getTableHeader().setForeground(Theme.ACCENT);

        JScrollPane sc = new JScrollPane(cartTable);
        sc.getViewport().setBackground(Color.WHITE);
        sc.setBorder(null);
        panel.add(sc, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bottom.setBackground(Color.WHITE);
        JButton btnRm = Theme.makeButton("Remove Selected", Theme.BTN_RED, Theme.BTN_RED_H);
        btnRm.addActionListener(e -> removeItem());
        bottom.add(btnRm);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // ── Card helper ───────────────────────────────────────────────────────────
    private JPanel card(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createLineBorder(new Color(180, 220, 200), 1));
        JLabel hdr = new JLabel("  " + title);
        hdr.setFont(new Font("SansSerif", Font.BOLD, 13));
        hdr.setForeground(Color.WHITE);
        hdr.setBackground(Theme.ACCENT);
        hdr.setOpaque(true);
        hdr.setBorder(new EmptyBorder(9, 10, 9, 10));
        p.add(hdr, BorderLayout.NORTH);
        return p;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────
    private void loadProducts() {
        cbProduct.removeAllItems();
        try (Connection con = DB.getConnection(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT product_id, name, price, stock FROM Products WHERE stock > 0")) {
            while (rs.next())
                cbProduct.addItem(new Object[]{
                    rs.getInt("product_id"), rs.getString("name"),
                    rs.getDouble("price"), rs.getInt("stock")
                });
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Cannot load products:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
        updateStock();
    }

    private void updateStock() {
        Object item = cbProduct.getSelectedItem();
        if (!(item instanceof Object[])) return;
        int s = (int) ((Object[]) item)[3];
        lblStock.setText(s + " units");
        lblStock.setForeground(s < 5 ? Theme.BTN_RED : new Color(10,100,50));
    }

    private void addToCart() {
        Object sel = cbProduct.getSelectedItem();
        if (sel == null) { warn("No products available."); return; }
        Object[] prod = (Object[]) sel;
        int    id    = (int)    prod[0];
        String name  = (String) prod[1];
        double price = (double) prod[2];
        int    avail = (int)    prod[3];
        try {
            int    qty  = Integer.parseInt(tfQty.getText().trim());
            double disc = Double.parseDouble(tfDiscount.getText().trim());
            if (qty <= 0)    throw new IllegalArgumentException("Qty must be at least 1.");
            if (qty > avail) throw new IllegalArgumentException("Only " + avail + " units in stock.");
            if (disc < 0 || disc > 100) throw new IllegalArgumentException("Discount must be 0–100.");
            double discPrice = price * (1.0 - disc / 100.0);
            double subtotal  = discPrice * qty;
            cart.add(new Object[]{id, name, qty, price, discPrice, disc});
            cartModel.addRow(new Object[]{
                name, qty, String.format("%.2f", price),
                String.format("%.1f%%", disc), String.format("%.2f", subtotal)
            });
            updateTotal();
        } catch (NumberFormatException ex) { warn("Qty and Discount must be valid numbers."); }
        catch (IllegalArgumentException ex) { warn(ex.getMessage()); }
    }

    private void removeItem() {
        int row = cartTable.getSelectedRow();
        if (row < 0) { warn("Select a cart row to remove."); return; }
        cart.remove(row);
        cartModel.removeRow(row);
        updateTotal();
    }

    private void updateTotal() {
        double t = 0;
        for (Object[] it : cart) t += (int) it[2] * (double) it[4];
        lblTotal.setText(String.format("Total:  LKR  %.2f", t));
        lblCartCount.setText(cart.size() + (cart.size() == 1 ? " item" : " items"));
    }

    private void cancel() {
        if (!cart.isEmpty() && JOptionPane.showConfirmDialog(this,
                "Clear cart and start over?", "Cancel Sale", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        cart.clear(); cartModel.setRowCount(0); updateTotal();
        tfCustName.setText(""); tfCustContact.setText("");
    }

    private void complete() {
        if (cart.isEmpty()) { warn("Cart is empty."); return; }
        String custName    = tfCustName.getText().trim();
        String custContact = tfCustContact.getText().trim();
        String dateStr     = new SimpleDateFormat("yyyy-MM-dd").format((java.util.Date) spinDate.getValue());
        double total       = 0;
        for (Object[] it : cart) total += (int) it[2] * (double) it[4];

        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);
            try {
                int custId = upsertCustomer(con, custName, custContact);
                int saleId;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Sales (sale_date, customer_id, total) VALUES (?,?,?)",
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, dateStr);
                    if (custId > 0) ps.setInt(2, custId); else ps.setNull(2, java.sql.Types.INTEGER);
                    ps.setDouble(3, total);
                    ps.executeUpdate();
                    ResultSet k = ps.getGeneratedKeys(); k.next(); saleId = k.getInt(1);
                }
                StringBuilder warnings = new StringBuilder();
                for (Object[] it : cart) {
                    int prodId = (int) it[0], qty = (int) it[2];
                    double price = (double) it[4];
                    int cur = currentStock(con, prodId);
                    if (qty > cur) throw new IllegalStateException("Stock changed for '" + it[1] + "'.");
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO SaleItems (sale_id,product_id,quantity,price) VALUES(?,?,?,?)")) {
                        ps.setInt(1,saleId); ps.setInt(2,prodId); ps.setInt(3,qty); ps.setDouble(4,price);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE Products SET stock=stock-? WHERE product_id=?")) {
                        ps.setInt(1,qty); ps.setInt(2,prodId); ps.executeUpdate();
                    }
                    int newStock = cur - qty;
                    if (newStock < 5)
                        warnings.append("⚠ Low stock: ").append(it[1])
                            .append(" (").append(newStock).append(" left)\n");
                }
                con.commit();
                String msg = String.format("✔ Sale #%d complete!\nTotal: LKR %.2f", saleId, total);
                if (warnings.length() > 0) msg += "\n\n" + warnings.toString().trim();
                JOptionPane.showMessageDialog(this, msg, "Sale Complete", JOptionPane.INFORMATION_MESSAGE);
                cancel(); loadProducts();
            } catch (Exception ex) {
                con.rollback();
                JOptionPane.showMessageDialog(this, "Sale rolled back:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            } finally { con.setAutoCommit(true); }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewHistory() {
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format((java.util.Date) spinDate.getValue());
        DefaultTableModel m = new DefaultTableModel(
            new String[]{"Sale ID", "Customer", "Contact", "Total (LKR)", "Date"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT s.sale_id, c.name, c.contact, s.total, s.sale_date " +
                 "FROM Sales s LEFT JOIN Customers c ON s.customer_id = c.customer_id " +
                 "WHERE s.sale_date = ?")) {
            ps.setString(1, dateStr);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                m.addRow(new Object[]{
                    rs.getInt("sale_id"),
                    rs.getString("name") != null ? rs.getString("name") : "Walk-in",
                    rs.getString("contact") != null ? rs.getString("contact") : "–",
                    String.format("%.2f", rs.getDouble("total")),
                    rs.getString("sale_date")
                });
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JTable tbl = new JTable(m);
        tbl.setFont(new Font("SansSerif", Font.PLAIN, 13)); tbl.setRowHeight(28);
        tbl.setAutoCreateRowSorter(true);
        tbl.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tbl.getTableHeader().setBackground(new Color(15,72,50)); tbl.getTableHeader().setForeground(Color.WHITE);
        JScrollPane sc = new JScrollPane(tbl); sc.setPreferredSize(new Dimension(520, 280));
        sc.getViewport().setBackground(Color.WHITE);
        JPanel p = new JPanel(new BorderLayout(0,6)); p.setBackground(Color.WHITE);
        JLabel hdr = new JLabel("Sales on " + dateStr + "  (" + m.getRowCount() + " records)", SwingConstants.CENTER);
        hdr.setFont(new Font("SansSerif", Font.BOLD, 13)); hdr.setForeground(Theme.ACCENT);
        p.add(hdr, BorderLayout.NORTH); p.add(sc, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, p, "Sales History", JOptionPane.PLAIN_MESSAGE);
    }

    private int upsertCustomer(Connection con, String name, String contact) throws SQLException {
        if (name.isEmpty()) return -1;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT customer_id FROM Customers WHERE name=? AND contact=?")) {
            ps.setString(1, name); ps.setString(2, contact);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Customers (name,contact) VALUES(?,?)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name); ps.setString(2, contact);
            ps.executeUpdate();
            ResultSet k = ps.getGeneratedKeys(); k.next(); return k.getInt(1);
        }
    }

    private int currentStock(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT stock FROM Products WHERE product_id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private GridBagConstraints gbc(int x, int y, double wx) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 6); g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = x; g.gridy = y; g.weightx = wx;
        return g;
    }

    private JLabel fl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(new Color(60,100,80));
        return l;
    }

    private JButton cBtn(String text, Color bg, Color hover) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setOpaque(true); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(10, 22, 10, 22));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            public void mouseExited (MouseEvent e) { b.setBackground(bg);    }
        });
        return b;
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Sales", JOptionPane.WARNING_MESSAGE);
    }

    private void goBack() { dispose(); new Dashboard(user).setVisible(true); }

    private static class StripedRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, focus, row, col);
            if (!sel) setBackground(row % 2 == 0 ? Color.WHITE : new Color(240,250,245));
            return this;
        }
    }
}
