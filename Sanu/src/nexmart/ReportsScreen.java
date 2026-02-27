package nexmart;

import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.swing.JRViewer;

public class ReportsScreen extends JFrame {

    private static final long serialVersionUID = 1L;
    private final String user;

    private JTextField tfFrom, tfTo;
    private JTable     previewTable;
    private DefaultTableModel previewModel;
    private JLabel     lblPreviewInfo;
    private int        selectedReport = 0; // 0 = Monthly, 1 = Customer

    private static final String RPT_MONTHLY  = "/nexmart/MonthlySalesSummary.jrxml";
    private static final String RPT_CUSTOMER = "/nexmart/CustomerPurchaseHistory.jrxml";

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new ReportsScreen("admin").setVisible(true));
    }

    public ReportsScreen(String user) {
        this.user = user;
        initialize();
    }

    private void initialize() {
        setTitle("NexMart – Reports");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { goBack(); }
        });
        setSize(1040, 700);
        setMinimumSize(new Dimension(780, 560));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);
        root.add(Theme.createNavBar("reports", user, this), BorderLayout.NORTH);

        // ── Page header ───────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(14, 68, 50));
        header.setBorder(new EmptyBorder(10, 16, 10, 16));
        JLabel title = new JLabel("Reports & Analytics");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        // ── Body ──────────────────────────────────────────────────────────────
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(Theme.BG);
        body.setBorder(new EmptyBorder(14, 18, 12, 18));

        // ── Two report type cards (clickable) ─────────────────────────────────
        JPanel typeRow = new JPanel(new GridLayout(1, 2, 14, 0));
        typeRow.setBackground(Theme.BG);
        typeRow.setPreferredSize(new Dimension(0, 120));

        JPanel cardMonthly  = buildReportCard("Monthly Sales Summary",
            "Revenue per day, transaction count & average sale value.",
            "📊", 0);
        JPanel cardCustomer = buildReportCard("Customer Purchase History",
            "Total spend, visits & last purchase date per customer.",
            "👤", 1);

        typeRow.add(cardMonthly);
        typeRow.add(cardCustomer);
        body.add(typeRow, BorderLayout.NORTH);

        // ── Date range row ────────────────────────────────────────────────────
        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        dateRow.setBackground(new Color(240, 250, 245));
        dateRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 220, 200), 1),
            new EmptyBorder(4, 6, 4, 6)));

        dateRow.add(fl("From:"));
        tfFrom = Theme.makeField(thirtyDaysAgo());
        tfFrom.setPreferredSize(new Dimension(130, 34));
        dateRow.add(tfFrom);
        dateRow.add(fl("To:"));
        tfTo = Theme.makeField(today());
        tfTo.setPreferredSize(new Dimension(130, 34));
        dateRow.add(tfTo);

        JButton btnPreview = Theme.makeButton("Quick Preview", Theme.BTN_AMBER, Theme.BTN_AMBER_H);
        JButton btnJasper  = Theme.makeButton("Full Jasper Report ↗", Theme.ACCENT, Theme.ACCENT_HOVER);
        btnPreview.addActionListener(e -> quickPreview());
        btnJasper.addActionListener(e  -> generateJasper());
        dateRow.add(btnPreview);
        dateRow.add(btnJasper);

        body.add(dateRow, BorderLayout.CENTER);

        // ── Preview table card ─────────────────────────────────────────────────
        JPanel previewCard = new JPanel(new BorderLayout());
        previewCard.setBackground(Color.WHITE);
        previewCard.setBorder(BorderFactory.createLineBorder(new Color(180, 220, 200), 1));

        lblPreviewInfo = new JLabel("  Select a report type above, set date range, then click Quick Preview");
        lblPreviewInfo.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblPreviewInfo.setForeground(Color.WHITE);
        lblPreviewInfo.setBackground(Theme.ACCENT);
        lblPreviewInfo.setOpaque(true);
        lblPreviewInfo.setBorder(new EmptyBorder(9, 10, 9, 10));
        previewCard.add(lblPreviewInfo, BorderLayout.NORTH);

        previewModel = new DefaultTableModel(new String[]{"—"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        previewTable = new JTable(previewModel);
        previewTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        previewTable.setRowHeight(28);
        previewTable.setAutoCreateRowSorter(true);
        previewTable.setShowVerticalLines(false);
        previewTable.setSelectionBackground(new Color(210,240,228));
        previewTable.setDefaultRenderer(Object.class, new StripedRenderer());
        previewTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        previewTable.getTableHeader().setBackground(new Color(235,248,242));
        previewTable.getTableHeader().setForeground(Theme.ACCENT);

        JScrollPane sc = new JScrollPane(previewTable);
        sc.getViewport().setBackground(Color.WHITE);
        sc.setBorder(null);
        previewCard.add(sc, BorderLayout.CENTER);

        body.add(previewCard, BorderLayout.SOUTH);

        // Make the previewCard expand vertically
        JPanel split = new JPanel(new BorderLayout(0, 12));
        split.setBackground(Theme.BG);
        split.add(typeRow,  BorderLayout.NORTH);
        split.add(dateRow,  BorderLayout.CENTER);
        split.add(previewCard, BorderLayout.SOUTH);

        // Use a BoxLayout-like arrangement for proper spacing
        JPanel fullBody = new JPanel();
        fullBody.setBackground(Theme.BG);
        fullBody.setLayout(new BoxLayout(fullBody, BoxLayout.Y_AXIS));
        fullBody.setBorder(new EmptyBorder(14, 18, 12, 18));

        typeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        dateRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        fullBody.add(typeRow);
        fullBody.add(Box.createVerticalStrut(12));
        fullBody.add(dateRow);
        fullBody.add(Box.createVerticalStrut(12));
        fullBody.add(previewCard);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BG);
        main.add(header,   BorderLayout.NORTH);
        main.add(fullBody, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);

        // Default select first card
        selectCard(cardMonthly);
    }

    // ── Clickable report type card ─────────────────────────────────────────────
    private JPanel activeCard = null;

    private JPanel buildReportCard(String title, String desc, String icon, int rpt) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 220, 200), 2),
            new EmptyBorder(14, 16, 14, 16)));
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel ico = new JLabel(icon);
        ico.setFont(new Font("SansSerif", Font.PLAIN, 30));
        p.add(ico, BorderLayout.WEST);

        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setBackground(Color.WHITE);
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 14));
        t.setForeground(Theme.ACCENT);
        JLabel d = new JLabel("<html><body style='width:260px'>" + desc + "</body></html>");
        d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        d.setForeground(new Color(100, 130, 115));
        text.add(t, BorderLayout.NORTH);
        text.add(d, BorderLayout.CENTER);
        p.add(text, BorderLayout.CENTER);

        p.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e)  { selectedReport = rpt; selectCard(p); }
            public void mouseEntered(MouseEvent e)  { if (p != activeCard) p.setBackground(new Color(245,255,250)); text.setBackground(new Color(245,255,250)); }
            public void mouseExited (MouseEvent e)  { if (p != activeCard) p.setBackground(Color.WHITE); text.setBackground(Color.WHITE); }
        });
        return p;
    }

    private void selectCard(JPanel card) {
        if (activeCard != null) {
            activeCard.setBackground(Color.WHITE);
            activeCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 220, 200), 2),
                new EmptyBorder(14, 16, 14, 16)));
        }
        activeCard = card;
        card.setBackground(new Color(230, 250, 240));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.ACCENT, 2),
            new EmptyBorder(14, 16, 14, 16)));
    }

    // ── Quick Preview ────────────────────────────────────────────────────────
    private void quickPreview() {
        if (!validateDates()) return;
        String from = tfFrom.getText().trim(), to = tfTo.getText().trim();
        if (selectedReport == 0) previewMonthly(from, to);
        else                      previewCustomer(from, to);
    }

    private void previewMonthly(String from, String to) {
        previewModel.setRowCount(0); previewModel.setColumnCount(0);
        previewModel.addColumn("Sale Date"); previewModel.addColumn("Transactions");
        previewModel.addColumn("Revenue (LKR)"); previewModel.addColumn("Avg Sale (LKR)");
        lblPreviewInfo.setText("  Monthly Sales Summary  —  " + from + "  to  " + to);
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT sale_date, COUNT(*) AS txns, SUM(total) AS rev, AVG(total) AS avg_ " +
                 "FROM Sales WHERE sale_date BETWEEN ? AND ? GROUP BY sale_date ORDER BY sale_date DESC")) {
            ps.setString(1, from); ps.setString(2, to);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                previewModel.addRow(new Object[]{
                    rs.getString("sale_date"), rs.getInt("txns"),
                    String.format("%.2f", rs.getDouble("rev")),
                    String.format("%.2f", rs.getDouble("avg_"))
                });
            if (previewModel.getRowCount() == 0)
                previewModel.addRow(new Object[]{"No data found", "—", "—", "—"});
        } catch (SQLException ex) { err("DB error: " + ex.getMessage()); }
    }

    private void previewCustomer(String from, String to) {
        previewModel.setRowCount(0); previewModel.setColumnCount(0);
        previewModel.addColumn("Customer"); previewModel.addColumn("Contact");
        previewModel.addColumn("Purchases"); previewModel.addColumn("Total Spent (LKR)");
        previewModel.addColumn("Last Purchase");
        lblPreviewInfo.setText("  Customer Purchase History  —  " + from + "  to  " + to);
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT c.name, c.contact, COUNT(s.sale_id) AS purchases, " +
                 "SUM(s.total) AS spent, MAX(s.sale_date) AS last_date " +
                 "FROM Sales s JOIN Customers c ON s.customer_id = c.customer_id " +
                 "WHERE s.sale_date BETWEEN ? AND ? GROUP BY c.customer_id ORDER BY spent DESC")) {
            ps.setString(1, from); ps.setString(2, to);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                previewModel.addRow(new Object[]{
                    rs.getString("name"), rs.getString("contact"),
                    rs.getInt("purchases"),
                    String.format("%.2f", rs.getDouble("spent")),
                    rs.getString("last_date")
                });
            if (previewModel.getRowCount() == 0)
                previewModel.addRow(new Object[]{"No data found", "—", "—", "—", "—"});
        } catch (SQLException ex) { err("DB error: " + ex.getMessage()); }
    }

    // ── JasperReports ────────────────────────────────────────────────────────
    private void generateJasper() {
        if (!validateDates()) return;
        String from  = tfFrom.getText().trim(), to = tfTo.getText().trim();
        String path  = selectedReport == 0 ? RPT_MONTHLY : RPT_CUSTOMER;
        String label = selectedReport == 0 ? "Monthly Sales Summary" : "Customer Purchase History";
        try (Connection con = DB.getConnection()) {
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                JOptionPane.showMessageDialog(this,
                    "JRXML file not found: " + path +
                    "\n\nPlace the .jrxml files in src/nexmart/ and press F5 in Eclipse.",
                    "JasperReports Setup", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JasperReport compiled = JasperCompileManager.compileReport(stream);
            Map<String, Object> params = new HashMap<>();
            params.put("From_Date", from);
            params.put("To_Date",   to);
            JasperPrint print = JasperFillManager.fillReport(compiled, params, con);
            JFrame viewer = new JFrame(label + "  [" + from + " – " + to + "]");
            viewer.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            viewer.setSize(1100, 740);
            viewer.setLocationRelativeTo(this);
            viewer.getContentPane().add(new JRViewer(print));
            viewer.setVisible(true);
        } catch (JRException ex) { err("JasperReports error:\n" + ex.getMessage()); }
        catch (SQLException ex)  { err("Database error:\n" + ex.getMessage()); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private boolean validateDates() {
        String from = tfFrom.getText().trim(), to = tfTo.getText().trim();
        if (!from.matches("\\d{4}-\\d{2}-\\d{2}") || !to.matches("\\d{4}-\\d{2}-\\d{2}")) {
            JOptionPane.showMessageDialog(this, "Use YYYY-MM-DD format for dates.",
                "Validation", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private String today() { return new SimpleDateFormat("yyyy-MM-dd").format(new Date()); }

    private String thirtyDaysAgo() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -30);
        return new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
    }

    private JLabel fl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(Theme.LABEL_FG);
        return l;
    }

    private void err(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
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
