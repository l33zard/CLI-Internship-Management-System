package boundary;

import controller.AuthController;
import java.util.Scanner;

/**
 * Base class for console UI boundaries providing common input/output helpers
 * and a reference to the {@link controller.AuthController} when available.
 * <p>
 * This abstract class serves as the foundation for all user interface boundaries
 * in the system, providing:
 * <ul>
 *   <li>Common input validation and parsing utilities</li>
 *   <li>Standardized display formatting methods</li>
 *   <li>Navigation and confirmation helpers</li>
 *   <li>Password change functionality</li>
 *   <li>Cancel command recognition</li>
 * </ul>
 * 
 * All concrete boundary classes should extend this class to maintain consistent
 * user interaction patterns throughout the application.
 * 
 */
public abstract class BaseBoundary {
    /** Scanner instance for reading user input from standard input */
    protected final Scanner sc;
    
    /** Authentication controller for password management (may be null) */
    protected final AuthController auth;

    /**
     * Creates a boundary with an Authentication controller.
     * <p>
     * Use this constructor for boundaries that require password change functionality
     * or other authentication-related features.
     *
     * @param auth authentication controller for password changes and user verification
     */
    public BaseBoundary(AuthController auth) {
        this.sc = new Scanner(System.in);
        this.auth = auth;
    }

    /**
     * Creates a boundary without authentication support.
     * <p>
     * Use this constructor for boundaries that don't require authentication features,
     * such as registration or public information boundaries.
     */
    public BaseBoundary() {
        this.sc = new Scanner(System.in);
        this.auth = null;
    }

    /* ---------- Common Input Helpers ---------- */

    /**
     * Prompts for and returns a non-empty string input. Re-prompts until user provides a value.
     * <p>
     * This method ensures that required fields are not left empty by continuously
     * prompting the user until valid input is provided.
     *
     * @param prompt message to display to the user
     * @return trimmed non-empty user input
     * 
     * 
     * Usage example:
     * String name = getRequiredInput("Enter your name: ");
     */
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

    /**
     * Prompts for optional string input; returns the current value if user skips (empty response).
     * <p>
     * Useful for edit scenarios where existing values should be preserved if the user
     * doesn't want to make changes.
     *
     * @param prompt message to display to the user
     * @param currentValue value to use if user enters nothing
     * @return user input if provided, otherwise the current value
     * 
     * 
     * Usage example:
     * String newTitle = getOptionalInput("Enter new title [Current: " + currentTitle + "]: ", currentTitle);
     */
    protected String getOptionalInput(String prompt, String currentValue) {
        System.out.print(prompt);
        String input = sc.nextLine().trim();
        return input.isEmpty() ? currentValue : input;
    }

    /**
     * Safely parses a string to integer, returning a default value on parse failure.
     * <p>
     * This method handles NumberFormatException internally and provides graceful
     * degradation when parsing fails.
     *
     * @param s string to parse as integer
     * @param def default value to return if parsing fails
     * @return parsed integer value, or default value if parsing fails
     * 
     * Usage example:
     * int age = safeInt(userInput, 0); // Returns 0 if userInput is not a number
     */
    protected int safeInt(String s, int def) {
        try { 
            return Integer.parseInt(s.trim()); 
        } catch (Exception e) { 
            return def; 
        }
    }

    /**
     * Prompts for a yes/no response with comprehensive input recognition.
     * <p>
     * Accepts multiple input formats for affirmative and negative responses:
     * <ul>
     *   <li><strong>Yes:</strong> "1", "y", "yes", "true"</li>
     *   <li><strong>No:</strong> "0", "n", "no", "false"</li>
     * </ul>
     * Input is case-insensitive. Re-prompts on unrecognized input.
     *
     * @param message question to ask the user
     * @return true for yes responses, false for no responses
     * 
     * Usage example:
     * boolean proceed = promptBoolean("Do you want to continue? ");
     */
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

    /**
     * Checks whether input represents a cancel/back/exit command.
     * <p>
     * Recognizes the following cancel commands (case-insensitive):
     * <ul>
     *   <li>"0"</li>
     *   <li>"cancel"</li>
     *   <li>"back"</li>
     *   <li>"quit"</li>
     *   <li>"exit"</li>
     * </ul>
     *
     * @param input user input to check for cancel command
     * @return true if input matches any recognized cancel command, false otherwise
     */
    protected boolean isCancelCommand(String input) {
        if (input == null) return false;
        String normalized = input.trim().toLowerCase();
        return normalized.equals("0") || 
               normalized.equals("cancel") || 
               normalized.equals("back") || 
               normalized.equals("quit") ||
               normalized.equals("exit");
    }

    /**
     * Displays a standard operation cancelled message to the user.
     * <p>
     * Provides consistent feedback when users cancel operations throughout the application.
     */
    protected void displayCancelMessage() {
        System.out.println("Operation cancelled.");
    }

    /**
     * Prompts user to change their password via the authentication controller.
     * <p>
     * Guides the user through the password change process:
     * <ol>
     *   <li>Prompts for current password</li>
     *   <li>Prompts for new password</li>
     *   <li>Attempts password change via authentication controller</li>
     *   <li>Provides success/failure feedback</li>
     * </ol>
     * Returns early if authentication controller is not available.
     *
     * @param loginKey user's ID or email to identify them for password change
     * @return true if password change succeeded, false if failed or unavailable
     * 
     * @see AuthController#changePassword(String, String, String)
     */
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

    /**
     * Displays a main section header with title and equals underline.
     * <p>
     * Creates a visually distinct section header for major UI sections.
     * Example output:
     * <pre>
     * Student Dashboard
     * ================
     * </pre>
     *
     * @param title header title to display
     */
    protected void displaySectionHeader(String title) {
        System.out.println();
        System.out.println(title);
        System.out.println("=".repeat(title.length()));
    }

    /**
     * Displays a subsection header with title and dash underline.
     * <p>
     * Creates a visually distinct subsection header for nested UI sections.
     * Example output:
     * <pre>
     * Application History
     * ------------------
     * </pre>
     *
     * @param title subsection title to display
     */
    protected void displaySubSectionHeader(String title) {
        System.out.println(title);
        System.out.println("-".repeat(title.length()));
    }

    /**
     * Displays a list header (title with blank line before).
     * <p>
     * Provides consistent formatting for list displays throughout the application.
     *
     * @param title list header title to display
     */
    protected void displayItemListHeader(String title) {
        System.out.println();
        System.out.println(title);
    }

    // Common navigation

    /**
     * Prompts user to confirm an action (yes/no).
     * <p>
     * Uses the same input recognition as {@link #promptBoolean(String)} but
     * with a standardized confirmation prompt format.
     *
     * @param message action description to confirm
     * @return true if user confirms, false if they decline
     * 
     * Usage example:
     * if (confirmAction("Delete this application")) {
     *     // Perform deletion
     * }
     */
    protected boolean confirmAction(String message) {
        return promptBoolean(message + " (1 = Yes, 0 = No): ");
    }

    /**
     * Displays a prompt asking user to press Enter to continue; blocks until user does.
     * <p>
     * Useful for paging output and ensuring users have time to read information
     * before proceeding to the next screen.
     */
    protected void pressEnterToContinue() {
        System.out.println("Press Enter to continue...");
        sc.nextLine();
    }
}