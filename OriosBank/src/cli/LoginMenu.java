package cli;

import model.Customer;
import util.InputValidator;

public class LoginMenu extends Menu {

    public void show() {
        while (true) {
            clearScreen();
            printHeader("LOGIN / REGISTER");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("\nChoice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": login(); break;
                case "2": register(); break;
                case "3": System.out.println("Goodbye!"); System.exit(0);
                default: System.out.println("Invalid choice."); pause();
            }
        }
    }

    private void login() {
        clearScreen();
        printHeader("LOGIN");
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        Customer customer = bankService.login(email, password);
        if (customer != null) {
            setCurrentCustomer(customer);
            System.out.println("\nWelcome, " + customer.getFullName() + "!");
            pause();
            new AccountMenu(this).show();
        } else {
            System.out.println("\nInvalid credentials.");
            pause();
        }
    }

    private void register() {
        clearScreen();
        printHeader("REGISTER");
        System.out.print("Full Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Password (min 6 chars): ");
        String password = scanner.nextLine().trim();
        System.out.print("Phone: ");
        String phone = scanner.nextLine().trim();

        if (!InputValidator.isValidName(name)) {
            System.out.println("Invalid name."); pause(); return;
        }
        if (!InputValidator.isValidEmail(email)) {
            System.out.println("Invalid email."); pause(); return;
        }
        if (!InputValidator.isValidPassword(password)) {
            System.out.println("Password too short."); pause(); return;
        }

        Customer c = bankService.registerCustomer(name, email, password, phone);
        if (c != null) {
            System.out.println("\nRegistration successful! Your ID: " + c.getCustomerId());
        } else {
            System.out.println("\nRegistration failed. Email may already exist.");
        }
        pause();
    }
}
