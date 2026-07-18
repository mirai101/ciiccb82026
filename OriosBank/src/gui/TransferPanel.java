package gui;

import service.AccountService;
import service.BankService;
import model.Customer;
import model.Account;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TransferPanel extends JPanel {
    private final DashboardFrame parent;
    private final AccountService accountService;
    private final BankService bankService;
    private final Customer customer;
    private JComboBox<String> fromCombo;
    private JTextField toField;
    private JTextField amountField;

    public TransferPanel(DashboardFrame parent, AccountService accountService, BankService bankService, Customer customer) {
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

        JLabel title = new JLabel("Transfer Funds", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(30, 60, 114));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1;
        add(new JLabel("From Account:"), gbc);
        fromCombo = new JComboBox<>();
        refreshAccounts();
        gbc.gridx = 1;
        add(fromCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("To Account ID:"), gbc);
        toField = new JTextField(15);
        gbc.gridx = 1;
        add(toField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("Amount ($):"), gbc);
        amountField = new JTextField(15);
        gbc.gridx = 1;
        add(amountField, gbc);

        JButton transferBtn = new JButton("Transfer");
        transferBtn.setBackground(new Color(30, 60, 114));
        transferBtn.setForeground(Color.WHITE);
        transferBtn.setFocusPainted(false);
        transferBtn.addActionListener(e -> processTransfer());
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        add(transferBtn, gbc);
    }

    private void refreshAccounts() {
        fromCombo.removeAllItems();
        List<Account> accounts = bankService.getCustomerAccounts(customer.getCustomerId());
        for (Account acc : accounts) {
            fromCombo.addItem(acc.getAccountId() + " - " + acc.getAccountType() + " ($" + String.format("%.2f", acc.getBalance()) + ")");
        }
    }

    private void processTransfer() {
        if (fromCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "No account selected", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String fromId = fromCombo.getSelectedItem().toString().split(" - ")[0];
        String toId = toField.getText().trim();
        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            accountService.transfer(fromId, toId, amount);
            JOptionPane.showMessageDialog(this, "Transfer successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
            amountField.setText("");
            toField.setText("");
            refreshAccounts();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
