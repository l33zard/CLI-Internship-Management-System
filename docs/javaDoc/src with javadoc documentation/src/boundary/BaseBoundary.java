package boundary;

import controller.AuthController;
import java.util.Scanner;

public abstract class BaseBoundary {
    protected final Scanner sc;
    protected final AuthController auth;

    public BaseBoundary(AuthController auth) {
        this.sc = new Scanner(System.in);
        this.auth = auth;
    }

    public BaseBoundary() {
        this.sc = new Scanner(System.in);
        this.auth = null;
    }

    /* ---------- Common Input Helpers ---------- */

    protected String getRequiredInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("This field is required. Please enter a value.");
        }
    }

    protected String getOptionalInput(String prompt, String currentValue) {
        System.out.print(prompt);
        String input = sc.nextLine().trim();
        return input.isEmpty() ? currentValue : input;
    }

    protected int safeInt(String s, int def) {
        try { 
            return Integer.parseInt(s.trim()); 
        } catch (Exception e) { 
            return def; 
        }
    }

    protected boolean promptBoolean(String message) {
        while (true) {
            System.out.print(message);
            String in = sc.nextLine().trim().toLowerCase();
            switch (in) {
                case "1", "y", "yes", "true":
                    return true;
                case "0", "n", "no", "false":
                    return false;
                default:
                    System.out.println("Please enter 1 for Yes or 0 for No.");
            }
        }
    }

    protected boolean isCancelCommand(String input) {
        if (input == null) return false;
        String normalized = input.trim().toLowerCase();
        return normalized.equals("0") || 
               normalized.equals("cancel") || 
               normalized.equals("back") || 
               normalized.equals("quit") ||
               normalized.equals("exit");
    }

    protected void displayCancelMessage() {
        System.out.println("Operation cancelled.");
    }

    protected boolean changePassword(String loginKey) {
        if (auth == null) {
            System.out.println("Password change not available.");
            return false;
        }

        System.out.print("Current password: ");
        String oldPw = sc.nextLine();
        System.out.print("New password: ");
        String newPw = sc.nextLine();
        try {
            auth.changePassword(loginKey, oldPw, newPw);
            System.out.println("Password changed successfully. Please log in again.");
            return true;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    // Common display helpers
    protected void displaySectionHeader(String title) {
        System.out.println();
        System.out.println(title);
        System.out.println("=".repeat(title.length()));
    }

    protected void displaySubSectionHeader(String title) {
        System.out.println();
        System.out.println(title);
        System.out.println("-".repeat(title.length()));
    }

    protected void displayItemListHeader(String title) {
        System.out.println();
        System.out.println(title);
    }

    // Common navigation
    protected boolean confirmAction(String message) {
        return promptBoolean(message + " (1 = Yes, 0 = No): ");
    }

    protected void pressEnterToContinue() {
        System.out.println("Press Enter to continue...");
        sc.nextLine();
    }
}