package nexmart;

import java.awt.*;
import java.sql.*;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class Dashboard extends JFrame {

    private static final long serialVersionUID = 1L;
    private final String user;

    private DefaultTableModel salesModel;
    private DefaultTableModel alertsModel;
    private DefaultTableModel topModel;
    private JLabel            lblTotalRev, lblTotalTxn, lblLowCount;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new Dashboard("admin").setVisible(true));
    }

    public Dashboard(String user) {
        this.user = user;
        initialize();
        refresh();
        new Timer(30_000, e -> refresh()).start();
    }

    private void initialize() {
        setTitle("NexMart SMS – Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 680);
        setMinimumSize(new Dimension(840, 560));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);

        // Nav bar — NORTH of root, nothing else touches NORTH
        root.add(Theme.createNavBar("dashboard", user, this), BorderLayout.NORTH);

        // All content below the nav goes into this wrapper at CENTER
        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(Theme.BG);
        root.add(content, BorderLayout.CENTER);

        // ── Summary stat strip ───────────────────────────────────────────────
        JPanel strip = new JPanel(new GridLayout(1, 3, 10, 0));
        strip.setBackground(Theme.BG);
        strip.setBorder(new EmptyBorder(10, 14, 6, 14));

        lblTotalRev = statLabel("—", new Color(16, 124, 90));
        lblTotalTxn = statLabel("—", new Color(202, 138, 4));
        lblLowCount = statLabel("—", new Color(220, 53, 53));

        strip.add(wrapStat("Month Revenue (LKR)", lblTotalRev, new Color(16, 124, 90)));
        strip.add(wrapStat("Month Transactions",  lblTotalTxn, new Color(202, 138, 4)));
        strip.add(wrapStat("Low-Stock Alerts",    lblLowCount, new Color(220, 53, 53)));

        content.add(strip, BorderLayout.NORTH);

        // ── Three data tables ────────────────────────────────────────────────
        JPanel tables = new JPanel(new GridLayout(1, 3, 10, 0));
        tables.setBackground(Theme.BG);
        tables.setBorder(new EmptyBorder(0, 14, 6, 14));

        salesModel = new DefaultTableModel(
            new String[]{"Date", "Transactions", "Revenue (LKR)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tables.add(tableCard("Recent Sales  (Last 30 Days)", salesModel, false));

        alertsModel = new DefaultTableModel(
            new String[]{"Product", "Stock", "Threshold", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tables.add(tableCard("Low Stock Alerts", alertsModel, true));

        topModel = new DefaultTableModel(
            new String[]{"Product", "Units Sold", "Revenue (LKR)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tables.add(tableCard("Top Selling Products  (30 Days)", topModel, false));

        content.add(tables, BorderLayout.CENTER);

        // ── Refresh button bar ───────────────────────────────────────────────
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 6));
        south.setBackground(Theme.BG);
        JButton btnRefresh = Theme.makeButton("Refresh", Theme.ACCENT, Theme.ACCENT_HOVER);
        btnRefresh.addActionListener(e -> refresh());
        south.add(btnRefresh);
        content.add(south, BorderLayout.SOUTH);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private JLabel statLabel(String val, Color color) {
        JLabel l = new JLabel(val, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 22));
        l.setForeground(color);
        return l;
    }

    private JPanel wrapStat(String title, JLabel valLabel, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 224, 212), 1),
            new EmptyBorder(14, 16, 14, 16)));
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(new Font("SansSerif", Font.BOLD, 12));
        t.setForeground(new Color(90, 110, 100));
        p.add(t, BorderLayout.NORTH);
        p.add(valLabel, BorderLayout.CENTER);
        return p;
    }

    private JPanel tableCard(String title, DefaultTableModel model, boolean alert) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createLineBorder(new Color(200, 224, 212), 1));

        JLabel hdr = new JLabel("  " + title);
        hdr.setFont(new Font("SansSerif", Font.BOLD, 13));
        hdr.setForeground(Color.WHITE);
        hdr.setBackground(alert ? new Color(180, 50, 50) : Theme.ACCENT);
        hdr.setOpaque(true);
        hdr.setBorder(new EmptyBorder(8, 8, 8, 8));
        p.add(hdr, BorderLayout.NORTH);

        JTable tbl = new JTable(model);
        tbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tbl.setRowHeight(28);
        tbl.setAutoCreateRowSorter(true);
        tbl.setSelectionBackground(Theme.ACCENT_LIGHT);
        tbl.setGridColor(new Color(220, 235, 228));
        tbl.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tbl.getTableHeader().setBackground(new Color(235, 248, 242));
        tbl.getTableHeader().setForeground(Theme.ACCENT);
        if (alert) tbl.setDefaultRenderer(Object.class, new AlertRenderer());

        JScrollPane sc = new JScrollPane(tbl);
        sc.getViewport().setBackground(Color.WHITE);
        sc.setBorder(null);
        p.add(sc, BorderLayout.CENTER);
        return p;
    }

    // ── Data loading ─────────────────────────────────────────────────────────
    void refresh() {
        salesModel.setRowCount(0);
        alertsModel.setRowCount(0);
        topModel.setRowCount(0);
        try (Connection con = DB.getConnection()) {
            loadSales(con);
            loadAlerts(con);
            loadTop(con);
        } catch (SQLException ex) {
            loadMock();
        }
    }

    private void loadSales(Connection con) throws SQLException {
        String sql =
            "SELECT sale_date, COUNT(*) AS txns, SUM(total) AS rev " +
            "FROM Sales WHERE sale_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY sale_date ORDER BY sale_date DESC";
        double totalRev = 0; int totalTxn = 0;
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                double r = rs.getDouble("rev"); int t = rs.getInt("txns");
                totalRev += r; totalTxn += t;
                salesModel.addRow(new Object[]{rs.getString("sale_date"), t, String.format("%.2f", r)});
            }
        }
        lblTotalRev.setText(String.format("%,.2f", totalRev));
        lblTotalTxn.setText(String.valueOf(totalTxn));
    }

    private void loadAlerts(Connection con) throws SQLException {
        int count = 0;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT name, stock, reorder_threshold FROM Products " +
                 "WHERE stock < reorder_threshold ORDER BY stock ASC")) {
            while (rs.next()) {
                count++;
                int s = rs.getInt("stock"), th = rs.getInt("reorder_threshold");
                String status = s == 0 ? "Out of Stock" : s < 5 ? "Critical" : "Low Stock";
                alertsModel.addRow(new Object[]{rs.getString("name"), s, th, status});
            }
        }
        lblLowCount.setText(String.valueOf(count));
    }

    private void loadTop(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT p.name, SUM(si.quantity) AS units, SUM(si.quantity * si.price) AS rev " +
                 "FROM SaleItems si JOIN Products p ON si.product_id = p.product_id " +
                 "JOIN Sales s ON si.sale_id = s.sale_id " +
                 "WHERE s.sale_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
                 "GROUP BY p.name ORDER BY units DESC LIMIT 10")) {
            while (rs.next())
                topModel.addRow(new Object[]{
                    rs.getString("name"), rs.getInt("units"),
                    String.format("%.2f", rs.getDouble("rev"))
                });
        }
    }

    private void loadMock() {
        String[] prods = {"Basmati Rice", "Cane Sugar", "Extra Virgin Oil", "A4 Paper", "Herbal Soap"};
        Random rnd = new Random(1);
        java.time.LocalDate today = java.time.LocalDate.now();
        double totalRev = 0; int totalTxn = 0;
        for (int i = 6; i >= 0; i--) {
            int t = 1 + rnd.nextInt(8); double r = 500 + rnd.nextInt(5000);
            totalRev += r; totalTxn += t;
            salesModel.addRow(new Object[]{today.minusDays(i).toString(), t, String.format("%.2f", r)});
        }
        lblTotalRev.setText(String.format("%,.2f", totalRev));
        lblTotalTxn.setText(String.valueOf(totalTxn));

        alertsModel.addRow(new Object[]{"Herbal Soap",      3, 5,  "Critical"});
        alertsModel.addRow(new Object[]{"Extra Virgin Oil", 7, 10, "Low Stock"});
        lblLowCount.setText("2");

        for (String p : prods)
            topModel.addRow(new Object[]{p, rnd.nextInt(50) + 5, String.format("%.2f", rnd.nextInt(8000) + 1000.0)});
    }

    // ── Alert row colouring ──────────────────────────────────────────────────
    private static class AlertRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
            if (!sel) {
                String s = t.getModel().getValueAt(t.convertRowIndexToModel(row), 3).toString();
                if (s.equals("Out of Stock")) {
                    c.setBackground(new Color(255, 200, 200)); c.setForeground(Color.BLACK);
                } else if (s.equals("Critical")) {
                    c.setBackground(new Color(255, 225, 180)); c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(new Color(255, 248, 200)); c.setForeground(Color.BLACK);
                }
            }
            return c;
        }
    }
}
