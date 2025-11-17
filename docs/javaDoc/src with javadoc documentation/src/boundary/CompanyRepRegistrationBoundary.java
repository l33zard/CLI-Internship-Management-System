package boundary;

import controller.CompanyRepRegistrationController;
import java.util.regex.Pattern;

public class CompanyRepRegistrationBoundary extends BaseBoundary {
    private final CompanyRepRegistrationController ctl;
    private static final Pattern EMAIL_RX = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public CompanyRepRegistrationBoundary(CompanyRepRegistrationController ctl) {
        super(); // No auth controller needed for registration
        this.ctl = ctl;
    }

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