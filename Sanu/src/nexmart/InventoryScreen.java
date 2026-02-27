package nexmart;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class InventoryScreen extends JFrame {

    private static final long serialVersionUID = 1L;
    private final String user;

    private JTextField    tfSearch;
    private JSlider       sliderMin, sliderMax;
    private JLabel        lblMin, lblMax, lblCount;
    private DefaultTableModel tableModel;
    private JTable        table;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new InventoryScreen("admin").setVisible(true));
    }

    public InventoryScreen(String user) {
        this.user = user;
        initialize();
        load("", 0, 9999);
    }

    private void initialize() {
        setTitle("NexMart – Inventory");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { goBack(); }
        });
        setSize(1080, 680);
        setMinimumSize(new Dimension(820, 520));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);
        root.add(Theme.createNavBar("inventory", user, this), BorderLayout.NORTH);

        // ── Page header ───────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(10, 65, 50));
        header.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel title = new JLabel("Inventory View");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        lblCount = new JLabel("0 products", SwingConstants.RIGHT);
        lblCount.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblCount.setForeground(new Color(160, 220, 190));
        header.add(lblCount, BorderLayout.EAST);

        // ── Filter bar ────────────────────────────────────────────────────────
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filter.setBackground(new Color(235, 248, 242));
        filter.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(180, 220, 200)));

        filter.add(lbl("Search:"));
        tfSearch = Theme.makeField();
        tfSearch.setPreferredSize(new Dimension(200, 34));
        filter.add(tfSearch);

        filter.add(lbl("  Stock >="));
        sliderMin = slider(0);
        lblMin = sLbl("0");
        sliderMin.addChangeListener(e -> lblMin.setText(String.valueOf(sliderMin.getValue())));
        filter.add(sliderMin); filter.add(lblMin);

        filter.add(lbl("  Stock <="));
        sliderMax = slider(500);
        lblMax = sLbl("500");
        sliderMax.addChangeListener(e -> lblMax.setText(String.valueOf(sliderMax.getValue())));
        filter.add(sliderMax); filter.add(lblMax);

        JButton btnFilter = Theme.makeButton("Apply Filter", Theme.ACCENT, Theme.ACCENT_HOVER);
        btnFilter.addActionListener(e -> load(tfSearch.getText().trim(), sliderMin.getValue(), sliderMax.getValue()));
        JButton btnReset  = Theme.makeButton("Reset", Theme.BTN_GRAY, Theme.BTN_GRAY_H);
        btnReset.addActionListener(e -> { tfSearch.setText(""); sliderMin.setValue(0); sliderMax.setValue(500); load("", 0, 9999); });
        JButton btnExport = Theme.makeButton("Export CSV", new Color(40,140,90), new Color(25,110,70));
        btnExport.addActionListener(e -> exportCSV());
        filter.add(btnFilter); filter.add(btnReset); filter.add(btnExport);

        // ── Table ─────────────────────────────────────────────────────────────
        String[] cols = {"ID", "Product Name", "Category", "Price (LKR)", "Stock", "Reorder", "Supplier", "Status"};
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
        table.setDefaultRenderer(Object.class, new StatusRenderer());
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(10, 65, 50));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));

        JScrollPane sc = new JScrollPane(table);
        sc.getViewport().setBackground(Color.WHITE);
        sc.setBorder(null);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Theme.BG);
        body.setBorder(new EmptyBorder(0, 14, 10, 14));
        body.add(sc, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BG);
        content.add(header, BorderLayout.NORTH);
        content.add(filter, BorderLayout.CENTER);
        content.add(body,   BorderLayout.SOUTH);

        // Re-layout: header → filter → table fills remaining space
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BG);
        main.add(header, BorderLayout.NORTH);

        JPanel below = new JPanel(new BorderLayout());
        below.setBackground(Theme.BG);
        below.add(filter, BorderLayout.NORTH);
        below.add(body,   BorderLayout.CENTER);

        main.add(below, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);
    }

    private void load(String q, int min, int max) {
        tableModel.setRowCount(0);
        String sql =
            "SELECT p.product_id, p.name, p.category, p.price, p.stock, p.reorder_threshold, " +
            "s.name AS supplier FROM Products p " +
            "LEFT JOIN Suppliers s ON p.supplier_id = s.supplier_id " +
            "WHERE (p.name LIKE ? OR p.category LIKE ?) AND p.stock >= ? AND p.stock <= ? ORDER BY p.name";
        try (Connection con = DB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            String like = "%" + q + "%";
            ps.setString(1, like); ps.setString(2, like);
            ps.setInt(3, min); ps.setInt(4, max);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int stock = rs.getInt("stock"), thresh = rs.getInt("reorder_threshold");
                String status = stock == 0 ? "Out of Stock" : stock < 5 ? "Critical"
                              : stock < thresh ? "Low Stock" : "OK";
                tableModel.addRow(new Object[]{
                    rs.getInt("product_id"), rs.getString("name"),
                    rs.getString("category"),
                    String.format("%.2f", rs.getDouble("price")),
                    stock, thresh, rs.getString("supplier"), status
                });
            }
            lblCount.setText(tableModel.getRowCount() + " products  ");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Load failed:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("nexmart_inventory.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
            StringBuilder hdr = new StringBuilder();
            for (int c = 0; c < tableModel.getColumnCount(); c++) {
                if (c > 0) hdr.append(",");
                hdr.append(tableModel.getColumnName(c));
            }
            pw.println(hdr);
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                StringBuilder row = new StringBuilder();
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    if (c > 0) row.append(",");
                    Object v = tableModel.getValueAt(r, c);
                    row.append("\"").append(v == null ? "" : v.toString().replace("\"","'")).append("\"");
                }
                pw.println(row);
            }
            JOptionPane.showMessageDialog(this, "Exported to:\n" + fc.getSelectedFile().getAbsolutePath(),
                "Export OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Export failed:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JSlider slider(int val) {
        JSlider s = new JSlider(0, 500, val);
        s.setPreferredSize(new Dimension(110, 30));
        s.setBackground(new Color(235, 248, 242));
        return s;
    }

    private JLabel sLbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(Theme.ACCENT);
        l.setPreferredSize(new Dimension(36, 20));
        return l;
    }

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(Theme.LABEL_FG);
        return l;
    }

    private void goBack() { dispose(); new Dashboard(user).setVisible(true); }

    // ── Status-aware colour renderer ──────────────────────────────────────────
    private static class StatusRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, focus, row, col);
            if (!sel) {
                String s = t.getModel().getValueAt(t.convertRowIndexToModel(row), 7).toString();
                if (s.equals("Out of Stock")) {
                    setBackground(new Color(255, 200, 200)); setForeground(new Color(140,0,0));
                } else if (s.equals("Critical")) {
                    setBackground(new Color(255, 225, 180)); setForeground(new Color(130,60,0));
                } else if (s.equals("Low Stock")) {
                    setBackground(new Color(255, 248, 200)); setForeground(new Color(100,80,0));
                } else {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(240,250,245)); setForeground(Color.BLACK);
                }
                if (col == 7) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setFont(new Font("SansSerif", Font.BOLD, 12));
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                    setFont(new Font("SansSerif", Font.PLAIN, 13));
                }
            }
            return this;
        }
    }
}
