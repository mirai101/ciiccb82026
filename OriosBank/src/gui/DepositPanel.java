package gui;

import service.AccountService;
import service.BankService;
import model.Customer;
import model.Account;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DepositPanel extends JPanel {
    private final DashboardFrame parent;
    private final AccountService accountService;
    private final BankService bankService;
    private final Customer customer;
    private JComboBox<String> accountCombo;
    private JTextField amountField;

    public DepositPanel(DashboardFrame parent, AccountService accountService, BankService bankService, Customer customer) {
        this.parent = parent;
        this.accountService = accountService;
        this.bankService = bankService;
        this.customer = customer;
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
        initializeUI();
    }

    private void initializeUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Deposit Funds", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(30, 60, 114));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1;
        add(new JLabel("Select Account:"), gbc);
        accountCombo = new JComboBox<>();
        refreshAccounts();
        gbc.gridx = 1;
        add(accountCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Amount ($):"), gbc);
        amountField = new JTextField(15);
        gbc.gridx = 1;
        add(amountField, gbc);

        JButton depositBtn = new JButton("Deposit");
        depositBtn.setBackground(new Color(76, 175, 80));
        depositBtn.setForeground(Color.WHITE);
        depositBtn.setFocusPainted(false);
        depositBtn.addActionListener(e -> processDeposit());
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        add(depositBtn, gbc);
    }

    private void refreshAccounts() {
        accountCombo.removeAllItems();
        List<Account> accounts = bankService.getCustomerAccounts(customer.getCustomerId());
        for (Account acc : accounts) {
            accountCombo.addItem(acc.getAccountId() + " - " + acc.getAccountType() + " ($" + String.format("%.2f", acc.getBalance()) + ")");
        }
    }

    private void processDeposit() {
        if (accountCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "No account selected", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String accId = accountCombo.getSelectedItem().toString().split(" - ")[0];
        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            accountService.deposit(accId, amount);
            JOptionPane.showMessageDialog(this, "Deposit successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
            amountField.setText("");
            refreshAccounts();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
