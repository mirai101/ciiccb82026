package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class Customer implements Serializable {
    private String customerId;
    private String fullName;
    private String email;
    private String hashedPassword;
    private String phone;
    private LocalDateTime registeredAt;

    public Customer(String fullName, String email, String password, String phone) {
        this(UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
             fullName, email, util.PasswordHasher.hash(password), phone, LocalDateTime.now());
    }

    public Customer(String customerId, String fullName, String email, String hashedPassword, String phone, LocalDateTime registeredAt) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.phone = phone;
        this.registeredAt = registeredAt;
    }

    public boolean verifyPassword(String password) {
        return util.PasswordHasher.verify(password, hashedPassword);
    }

    // Getters and Setters
    public String getCustomerId() { return customerId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public String getHashedPassword() { return hashedPassword; }

    @Override
    public String toString() {
        return customerId + "," + fullName + "," + email + "," + hashedPassword + "," + phone + "," + registeredAt;
    }
}
