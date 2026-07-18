package repository;

import model.Customer;
import util.FileUtil;

import java.util.ArrayList;
import java.util.List;

public class CustomerRepository {
    private static final String FILE_PATH = "data/customers.txt";

    public void save(Customer customer) {
        FileUtil.appendLine(FILE_PATH, customer.toString());
    }

    public List<Customer> findAll() {
        List<Customer> customers = new ArrayList<>();
        List<String> lines = FileUtil.readLines(FILE_PATH);
        for (String line : lines) {
            Customer c = parseCustomer(line);
            if (c != null) customers.add(c);
        }
        return customers;
    }

    public Customer findById(String customerId) {
        for (Customer c : findAll()) {
            if (c.getCustomerId().equals(customerId)) return c;
        }
        return null;
    }

    public Customer findByEmail(String email) {
        for (Customer c : findAll()) {
            if (c.getEmail().equalsIgnoreCase(email)) return c;
        }
        return null;
    }

    private Customer parseCustomer(String line) {
        String[] parts = line.split(",");
        if (parts.length < 6) return null;
        String id = parts[0];
        String name = parts[1];
        String email = parts[2];
        String hash = parts[3];
        String phone = parts[4];
        java.time.LocalDateTime registeredAt = java.time.LocalDateTime.parse(parts[5]);
        return new Customer(id, name, email, hash, phone, registeredAt);
    }
}
