package util;

public class InputValidator {
    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+[A-Za-z]{2,}$");
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isPositiveAmount(double amount) {
        return amount > 0;
    }

    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && name.matches("^[A-Za-z\s]+$");
    }
}
