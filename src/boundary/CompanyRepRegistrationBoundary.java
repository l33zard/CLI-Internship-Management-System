package boundary;

import controller.CompanyRepRegistrationController;
import java.util.regex.Pattern;

/**
 * Boundary responsible for signing up new company representatives.
 * This runs without authentication and validates basic input before
 * forwarding registration requests to the controller.
 * <p>
 * This boundary provides a user-friendly registration interface for company
 * representatives to create new accounts. The registration process includes:
 * <ul>
 *   <li>Comprehensive input validation for all fields</li>
 *   <li>Email format verification and uniqueness checking</li>
 *   <li>User confirmation before submission</li>
 *   <li>Cancellation support at any point during registration</li>
 *   <li>Clear feedback on registration status and next steps</li>
 * </ul>
 * 
 * Registered accounts are created with PENDING status and require approval
 * from Career Center staff before they can be used to access the system.
 * 
 */
public class CompanyRepRegistrationBoundary extends BaseBoundary {
    /** Controller for company representative registration operations */
    private final CompanyRepRegistrationController ctl;
    
    /** Regular expression pattern for validating email format */
    private static final Pattern EMAIL_RX = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /**
     * Constructs the registration boundary.
     *
     * @param ctl controller that performs the registration operation
     */
    public CompanyRepRegistrationBoundary(CompanyRepRegistrationController ctl) {
        super(); // No auth controller needed for registration
        this.ctl = ctl;
    }

    /**
     * Runs the interactive sign-up flow for company representatives.
     * <p>
     * This method guides users through the complete registration process:
     * <ol>
     *   <li>Prompts for personal and company information</li>
     *   <li>Validates all input fields</li>
     *   <li>Checks email uniqueness</li>
     *   <li>Displays confirmation summary</li>
     *   <li>Submits registration to controller</li>
     *   <li>Provides success feedback and next steps</li>
     * </ol>
     * 
     * Users can cancel the registration at any point by entering "0" or "cancel".
     * Upon successful registration, accounts are created with PENDING status
     * and require Career Center staff approval before login is permitted.
     * 
     * @see #promptNonEmpty(String)
     * @see #promptEmail()
     * @see CompanyRepRegistrationController#registerCompanyRep(String, String, String, String, String)
     */
    public void signupCompanyRep() {
        System.out.println("(Type '0' or 'cancel' anytime to cancel sign up)");

        String name = promptNonEmpty("Full name: ");
        if (name == null) return;

        String company = promptNonEmpty("Company name: ");
        if (company == null) return;

        String dept = promptNonEmpty("Department: ");
        if (dept == null) return;

        String pos = promptNonEmpty("Position/Title: ");
        if (pos == null) return;

        String email = promptEmail();
        if (email == null) return;

        // Confirm all details
        displaySectionHeader("Please confirm your details");
        System.out.println("Full name: " + name);
        System.out.println("Company name: " + company);
        System.out.println("Department: " + dept);
        System.out.println("Position/Title: " + pos);
        System.out.println("Email: " + email);

        boolean confirm = confirmAction("Submit registration?");
        if (!confirm) {
            displayCancelMessage();
            return;
        }

        try {
            String repEmail = ctl.registerCompanyRep(name, company, dept, pos, email);
            System.out.println("Submitted successfully!");
            System.out.println("Use your EMAIL to login after staff approval: " + repEmail);
        } catch (Exception e) {
            System.out.println("Sign-up error: " + e.getMessage());
        }
    }

    /* ---------- Input Helpers ---------- */

    /**
     * Prompts for a non-empty string input with validation against numbers-only input.
     * <p>
     * This method ensures that:
     * <ul>
     *   <li>Input is not null or empty</li>
     *   <li>Input contains non-whitespace characters</li>
     *   <li>Input is not composed entirely of digits</li>
     *   <li>User can cancel the operation at any time</li>
     * </ul>
     * 
     * Continuously re-prompts until valid input is provided or the user cancels.
     *
     * @param label prompt label to display to the user
     * @return entered non-empty string, or null if user cancels the operation
     * 
     * @see #isCancelCommand(String)
     */
    private String promptNonEmpty(String label) {
        while (true) {
            System.out.print(label);
            String in = sc.nextLine();
            if (isCancelCommand(in)) return null;
            if (in == null || in.trim().isEmpty()) {
                System.out.println("This field cannot be empty.");
                continue;
            }
            String val = in.trim();
            if (val.chars().allMatch(Character::isDigit)) {
                System.out.println("Please enter a valid value (not numbers only).");
                continue;
            }
            return val;
        }
    }
    
    /**
     * Prompts for email input with format validation and uniqueness check via controller.
     * <p>
     * This method performs comprehensive email validation:
     * <ul>
     *   <li>Checks for basic email format using regular expression</li>
     *   <li>Validates email uniqueness through the controller</li>
     *   <li>Normalizes email to lowercase for consistency</li>
     *   <li>Provides clear error messages for invalid input</li>
     *   <li>Allows users to retry or cancel on duplicate emails</li>
     * </ul>
     * 
     * The email format validation uses the pattern: {@code ^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$}
     * which requires:
     * <ul>
     *   <li>Non-empty local part (before @)</li>
     *   <li>Non-empty domain part (after @)</li>
     *   <li>At least one dot in the domain</li>
     *   <li>No whitespace characters</li>
     * </ul>
     *
     * @return entered valid and unique email address, or null if user cancels
     * 
     * @see #EMAIL_RX
     * @see CompanyRepRegistrationController#emailExists(String)
     * @see #isCancelCommand(String)
     * @see #confirmAction(String)
     */
    private String promptEmail() {
        while (true) {
            System.out.print("Email: ");
            String in = sc.nextLine();
            if (isCancelCommand(in)) return null;
            if (in == null || in.trim().isEmpty()) {
                System.out.println("Email cannot be empty.");
                continue;
            }
            String email = in.trim().toLowerCase();
            if (!EMAIL_RX.matcher(email).matches()) {
                System.out.println("Invalid email format. Example: name@company.com");
                continue;
            }

            // Check uniqueness using controller
            if (ctl.emailExists(email)) {
                System.out.println("An account with this email already exists.");
                boolean retry = confirmAction("Enter a different email?");
                if (!retry) {
                    displayCancelMessage();
                    return null;
                }
                continue;
            }

            return email;
        }
    }
}