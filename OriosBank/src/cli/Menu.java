package cli;

import service.BankService;
import service.AccountService;
import service.TransactionService;
import model.Customer;
import model.Account;
import java.util.Scanner;

public class Menu {
    protected final Scanner scanner;
    protected final BankService bankService;
    protected final AccountService accountService;
    protected final TransactionService transactionService;
    protected Customer currentCustomer;

    public Menu() {
        this(new Scanner(System.in), new BankService(), new AccountService(), new TransactionService());
    }

    public Menu(Scanner scanner, BankService bankService, AccountService accountService, TransactionService transactionService) {
        this.scanner = scanner;
        this.bankService = bankService;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void printHeader(String title) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║           ORIOS BANK SYSTEM              ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.printf ("║  %-38s  ║%n", title);
        System.out.println("╚══════════════════════════════════════════╝");
    }

    public void pause() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    public Customer getCurrentCustomer() { return currentCustomer; }
    public void setCurrentCustomer(Customer customer) { this.currentCustomer = customer; }
}
