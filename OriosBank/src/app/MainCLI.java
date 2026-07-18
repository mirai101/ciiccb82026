package app;

import cli.LoginMenu;

public class MainCLI {
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     WELCOME TO ORIOS BANK SYSTEM         ║");
        System.out.println("║         Terminal Interface               ║");
        System.out.println("╚══════════════════════════════════════════╝");
        new LoginMenu().show();
    }
}
