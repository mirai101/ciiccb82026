package cli;

import model.Account;
import model.SavingsAccount;
import model.CheckingAccount;
import exception.InsufficientBalanceException;
import exception.InvalidAmountException;
import util.InputValidator;

import java.util.List;

public class AccountMenu extends Menu {

    public AccountMenu(Menu parent) {
        super(parent.scanner, parent.bankService, parent.accountService, parent.transactionService);
        this.currentCustomer = parent.getCurrentCustomer();
    }

    public void show() {
        while (true) {
            clearScreen();
            printHeader("DASHBOARD - " + currentCustomer.getFullName().toUpperCase());
            System.out.println("1. View My Accounts");
            System.out.println("2. Open New Account");
            System.out.println("3. Deposit");
            System.out.println("4. Withdraw");
            System.out.println("5. Transfer");
            System.out.println("6. View Transactions");
            System.out.println("7. Logout");
            System.out.print("\nChoice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": viewAccounts(); break;
                case "2": openAccount(); break;
                case "3": deposit(); break;
                case "4": withdraw(); break;
                case "5": transfer(); break;
                case "6": viewTransactions(); break;
                case "7": return;
                default: System.out.println("Invalid choice."); pause();
            }
        }
    }

    private void viewAccounts() {
        clearScreen();
        printHeader("MY ACCOUNTS");
        List<Account> accounts = bankService.getCustomerAccounts(currentCustomer.getCustomerId());
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
        } else {
            System.out.printf("%-12s %-12s %-12s %-15s%n", "Account ID", "Type", "Balance", "Interest Rate");
            System.out.println("───────────────────────────────────────────────────────");
            for (Account acc : accounts) {
                System.out.printf("%-12s %-12s $%-11.2f %-15.2f%%%n",
                    acc.getAccountId(), acc.getAccountType(), acc.getBalance(), acc.getInterestRate() * 100);
            }
            System.out.println("\nTotal Balance: $" + bankService.getTotalBalance(currentCustomer.getCustomerId()));
        }
        pause();
    }

    private void openAccount() {
        clearScreen();
        printHeader("OPEN NEW ACCOUNT");
        System.out.println("1. Savings Account (2.5% interest, $100 min balance)");
        System.out.println("2. Checking Account (0.5% interest, $500 overdraft)");
        System.out.print("Choice: ");
        String type = scanner.nextLine().trim();
        System.out.print("Initial Deposit: $");
        double amount;
        try {
            amount = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount."); pause(); return;
        }

        String accType = type.equals("1") ? "SAVINGS" : "CHECKING";
        Account acc = bankService.openAccount(currentCustomer.getCustomerId(), accType, amount);
        if (acc != null) {
            System.out.println("\nAccount created! ID: " + acc.getAccountId());
        } else {
            System.out.println("\nFailed to create account.");
        }
        pause();
    }

    private void deposit() {
        clearScreen();
        printHeader("DEPOSIT");
        System.out.print("Account ID: ");
        String accId = scanner.nextLine().trim();
        System.out.print("Amount: $");
        double amount;
        try {
            amount = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount."); pause(); return;
        }
        try {
            accountService.deposit(accId, amount);
            System.out.println("\nDeposit successful!");
        } catch (InvalidAmountException e) {
            System.out.println("\nError: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("\nError: " + e.getMessage());
        }
        pause();
    }

    private void withdraw() {
        clearScreen();
        printHeader("WITHDRAW");
        System.out.print("Account ID: ");
        String accId = scanner.nextLine().trim();
        System.out.print("Amount: $");
        double amount;
        try {
            amount = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount."); pause(); return;
        }
        try {
            accountService.withdraw(accId, amount);
            System.out.println("\nWithdrawal successful!");
        } catch (InsufficientBalanceException | InvalidAmountException e) {
            System.out.println("\nError: " + e.getMessage());
        }
        pause();
    }

    private void transfer() {
        clearScreen();
        printHeader("TRANSFER");
        System.out.print("From Account ID: ");
        String fromId = scanner.nextLine().trim();
        System.out.print("To Account ID: ");
        String toId = scanner.nextLine().trim();
        System.out.print("Amount: $");
        double amount;
        try {
            amount = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount."); pause(); return;
        }
        try {
            accountService.transfer(fromId, toId, amount);
            System.out.println("\nTransfer successful!");
        } catch (InsufficientBalanceException | InvalidAmountException e) {
            System.out.println("\nError: " + e.getMessage());
        }
        pause();
    }

    private void viewTransactions() {
        clearScreen();
        printHeader("TRANSACTIONS");
        System.out.print("Account ID: ");
        String accId = scanner.nextLine().trim();
        var transactions = transactionService.getAccountTransactions(accId);
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
        } else {
            System.out.printf("%-12s %-15s %-10s %-20s%n", "Trans ID", "Type", "Amount", "Date");
            System.out.println("──────────────────────────────────────────────────────────");
            for (var t : transactions) {
                System.out.printf("%-12s %-15s $%-9.2f %-20s%n",
                    t.getTransactionId(), t.getType(), t.getAmount(), t.getTimestamp());
            }
        }
        pause();
    }
}
