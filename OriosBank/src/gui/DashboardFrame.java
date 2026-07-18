package gui;

import service.BankService;
import service.AccountService;
import service.TransactionService;
import model.Customer;
import model.Account;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class DashboardFrame extends JFrame {
    private final Customer customer;
    private final BankService bankService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private JPanel contentPanel;
    private JLabel totalLabel;

    public DashboardFrame(Customer customer) {
        this.customer = customer;
        this.bankService = new BankService();
        this.accountService = new AccountService();
        this.transactionService = new TransactionService();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("OriosBank - Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // Sidebar
        JPanel sidebar = new JPanel(new GridLayout(7, 1, 0, 5));
        sidebar.setBackground(new Color(30, 60, 114));
        sidebar.setPreferredSize(new Dimension(180, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JLabel brand = new JLabel("OriosBank", SwingConstants.CENTER);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 20));
        brand.setForeground(Color.WHITE);
        sidebar.add(brand);

        sidebar.add(createNavButton("Dashboard", this::showDashboard));
        sidebar.add(createNavButton("Deposit", this::showDeposit));
        sidebar.add(createNavButton("Withdraw", this::showWithdraw));
        sidebar.add(createNavButton("Transfer", this::showTransfer));
        sidebar.add(createNavButton("Accounts", this::showAccounts));
        sidebar.add(createNavButton("Logout", e -> { dispose(); new LoginFrame(); }));

        // Content area
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel);

        showDashboard(null);
        setVisible(true);
    }

    private JButton createNavButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text);
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(30, 60, 114));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.addActionListener(action);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(new Color(45, 85, 155)); }
            public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(new Color(30, 60, 114)); }
        });
        return btn;
    }

    private void showDashboard(ActionEvent e) {
        contentPanel.removeAll();
        JPanel dashboard = new JPanel(new GridLayout(3, 2, 15, 15));
        dashboard.setBackground(Color.WHITE);

        List<Account> accounts = bankService.getCustomerAccounts(customer.getCustomerId());
        double total = bankService.getTotalBalance(customer.getCustomerId());

        dashboard.add(createCard("Welcome", customer.getFullName()));
        dashboard.add(createCard("Customer ID", customer.getCustomerId()));
        dashboard.add(createCard("Total Balance", String.format("$%.2f", total)));
        dashboard.add(createCard("Accounts", String.valueOf(accounts.size())));
        dashboard.add(createCard("Email", customer.getEmail()));
        dashboard.add(createCard("Phone", customer.getPhone()));

        contentPanel.add(dashboard, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JPanel createCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(245, 247, 250));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 230)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLbl.setForeground(Color.GRAY);
        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valLbl.setForeground(new Color(30, 60, 114));
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valLbl, BorderLayout.CENTER);
        return card;
    }

    private void showDeposit(ActionEvent e) {
        contentPanel.removeAll();
        contentPanel.add(new DepositPanel(this, accountService, bankService, customer));
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showWithdraw(ActionEvent e) {
        contentPanel.removeAll();
        contentPanel.add(new WithdrawPanel(this, accountService, bankService, customer));
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showTransfer(ActionEvent e) {
        contentPanel.removeAll();
        contentPanel.add(new TransferPanel(this, accountService, bankService, customer));
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showAccounts(ActionEvent e) {
        contentPanel.removeAll();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        String[] columns = {"Account ID", "Type", "Balance", "Interest Rate"};
        List<Account> accounts = bankService.getCustomerAccounts(customer.getCustomerId());
        Object[][] data = new Object[accounts.size()][4];
        for (int i = 0; i < accounts.size(); i++) {
            Account acc = accounts.get(i);
            data[i][0] = acc.getAccountId();
            data[i][1] = acc.getAccountType();
            data[i][2] = String.format("$%.2f", acc.getBalance());
            data[i][3] = String.format("%.2f%%", acc.getInterestRate() * 100);
        }
        JTable table = new JTable(data, columns);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(30, 60, 114));
        table.getTableHeader().setForeground(Color.WHITE);
        JScrollPane scroll = new JScrollPane(table);
        panel.add(scroll, BorderLayout.CENTER);

        JButton openBtn = new JButton("Open New Account");
        openBtn.addActionListener(ev -> openNewAccount());
        panel.add(openBtn, BorderLayout.SOUTH);

        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void openNewAccount() {
        String[] options = {"Savings (2.5% interest)", "Checking (0.5% interest)"};
        int choice = JOptionPane.showOptionDialog(this, "Select account type:", "Open Account",
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice < 0) return;
        String type = choice == 0 ? "SAVINGS" : "CHECKING";
        String input = JOptionPane.showInputDialog(this, "Initial Deposit Amount:");
        try {
            double amount = Double.parseDouble(input);
            Account acc = bankService.openAccount(customer.getCustomerId(), type, amount);
            JOptionPane.showMessageDialog(this, "Account created! ID: " + acc.getAccountId());
            showAccounts(null);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
