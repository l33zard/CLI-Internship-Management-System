package boundary;

import database.CareerCenterStaffRepository;
import database.CompanyRepRepository;
import database.StudentRepository;
import entity.CareerCenterStaff;
import entity.CompanyRep;
import entity.Student;
import controller.AuthController;
import java.util.Optional;

public class LoginBoundary extends BaseBoundary {
    public enum Role { STUDENT, COMPANY_REP, STAFF }

    private final StudentRepository students;
    private final CompanyRepRepository reps;
    private final CareerCenterStaffRepository staff;

    public LoginBoundary(StudentRepository students,
                         CompanyRepRepository reps,
                         CareerCenterStaffRepository staff,
                         AuthController auth) {
        super(auth);
        this.students = students; 
        this.reps = reps; 
        this.staff = staff; 
    }

    /**
     * Main login method that handles the entire login process.
     * Returns a Session if login successful, empty if user cancels or fails.
     */
    public Optional<Session> performLogin() {
        System.out.println("Please login with your credentials");
        System.out.println("Type 'quit' or enter '0' to return");
        System.out.println("-".repeat(35));

        while (true) {
            System.out.print("Enter your ID/Email: ");
            String userInput = sc.nextLine().trim();
            
            // Check for exit command
            if (isCancelCommand(userInput)) {
                System.out.println("Exiting Login...");
                return Optional.empty();
            }
            
            // Validate input
            if (userInput.isEmpty()) {
                System.out.println("Error: ID/Email cannot be empty. Please try again.");
                continue;
            }

            try {
                // Use AuthController for authentication
                String authToken = auth.login(userInput, getPassword());
                if (authToken != null) {
                    return createSessionFromAuthToken(authToken, userInput);
                }
            } catch (Exception ex) {
                System.out.println("Login failed: " + ex.getMessage());
                System.out.println("Please try again.");
                
                // Offer to retry or quit
                if (!promptRetry()) {
                    return Optional.empty();
                }
            }
        }
    }

    /**
     * Get password from user with basic masking.
     */
    private String getPassword() {
        System.out.print("Password: ");
        return sc.nextLine().trim();
    }

    /**
     * Create session from AuthController's token.
     */
    private Optional<Session> createSessionFromAuthToken(String authToken, String userInput) {
        try {
            String[] parts = authToken.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalStateException("Invalid authentication token");
            }

            Role role = Role.valueOf(parts[0]);
            String userId = parts[1];

            switch (role) {
                case STUDENT:
                    Student student = students.findById(userId)
                            .orElseThrow(() -> new IllegalStateException("Student not found: " + userId));
                    System.out.println("Welcome, " + student.getName());
                    return Optional.of(new Session(role, student, null, null));

                case COMPANY_REP:
                    CompanyRep rep = reps.findByEmail(userId)
                            .orElseThrow(() -> new IllegalStateException("Company rep not found: " + userId));
                    if (rep.isApproved()) {
                        // Approved - allow login
                        System.out.println("Welcome, " + rep.getName());
                        return Optional.of(new Session(role, null, rep, null));
                    } else if (rep.isRejected()) {
                        // Explicitly rejected
                        System.out.println("Account rejected by Career Center staff.");
                        System.out.println("Reason: " + 
                            (rep.getRejectionReason() != null && !rep.getRejectionReason().isEmpty() 
                             ? rep.getRejectionReason() 
                             : "No reason provided"));
                        System.out.println("Please contact Career Center for more information.");
                        return Optional.empty();
                    } else {
                        // Neither approved nor rejected = pending
                        System.out.println("Account pending approval by Career Center staff. Please try again later.");
                        return Optional.empty();
                    }

                case STAFF:
                    CareerCenterStaff staffMember = staff.findById(userId)
                            .orElseThrow(() -> new IllegalStateException("Staff not found: " + userId));
                    System.out.println("Welcome, " + staffMember.getName());
                    return Optional.of(new Session(role, null, null, staffMember));

                default:
                    throw new IllegalStateException("Unknown role: " + role);
            }
        } catch (Exception ex) {
            System.out.println("Error creating session: " + ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Alternative login method for backward compatibility.
     */
    public Optional<Session> login(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            System.out.println("Error: ID/Email cannot be empty.");
            return Optional.empty();
        }

        userInput = userInput.trim();
        
        try {
            String authToken = auth.login(userInput, getPassword());
            if (authToken != null) {
                return createSessionFromAuthToken(authToken, userInput);
            }
        } catch (Exception ex) {
            System.out.println("Login failed: " + ex.getMessage());
        }
        
        return Optional.empty();
    }

    /**
     * Prompt user to retry login or quit.
     */
    private boolean promptRetry() {
        return confirmAction("Would you like to try again?");
    }

    /**
     * Display login instructions.
     */
    public void displayLoginHelp() {
        displaySectionHeader("Login Help");
        System.out.println("Student Login:");
        System.out.println("  Use your Student ID (format: U1234567X)");
        System.out.println("  Default password: 'password'");
        System.out.println();
        System.out.println("Company Representative Login:");
        System.out.println("  Use your email address");
        System.out.println("  Must be approved by Career Center staff first");
        System.out.println("  Default password: 'password'");
        System.out.println();
        System.out.println("Career Center Staff Login:");
        System.out.println("  Use your Staff ID");
        System.out.println("  Default password: 'password'");
    }

    /** 
     * Represents an active session (only one actor type non-null).
     */
    public static class Session {
        public final Role role;
        public final Student student;
        public final CompanyRep rep;
        public final CareerCenterStaff staff;

        public Session(Role role, Student student, CompanyRep rep, CareerCenterStaff staff) {
            this.role = role;
            this.student = student;
            this.rep = rep;
            this.staff = staff;
        }

        /**
         * Get the user's display name.
         */
        public String getUserDisplayName() {
            switch (role) {
                case STUDENT:
                    return student != null ? student.getName() : "Unknown Student";
                case COMPANY_REP:
                    return rep != null ? rep.getName() : "Unknown Representative";
                case STAFF:
                    return staff != null ? staff.getName() : "Unknown Staff";
                default:
                    return "Unknown User";
            }
        }

        /**
         * Get the user's ID for the session.
         */
        public String getUserId() {
            switch (role) {
                case STUDENT:
                    return student != null ? student.getUserId() : null;
                case COMPANY_REP:
                    return rep != null ? rep.getEmail() : null;
                case STAFF:
                    return staff != null ? staff.getUserId() : null;
                default:
                    return null;
            }
        }

        @Override
        public String toString() {
            return String.format("Session{role=%s, user=%s}", role, getUserDisplayName());
        }
    }
}