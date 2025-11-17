package boundary;

import database.CareerCenterStaffRepository;
import database.CompanyRepRepository;
import database.StudentRepository;
import entity.CareerCenterStaff;
import entity.CompanyRep;
import entity.Student;
import controller.AuthController;
import java.util.Optional;

/**
 * Console boundary class handling interactive login flow for all user roles in the Internship Placement Management System.
 * This class manages user authentication through the {@link AuthController} and creates session objects for authenticated users.
 * Supports login flows for Students, Company Representatives, and Career Center Staff with role-specific validation and approval checks.
 * 
 * <p>The login boundary provides:
 * <ul>
 *   <li>Interactive credential prompting with validation</li>
 *   <li>Multiple login attempt handling</li>
 *   <li>Role-based session creation</li>
 *   <li>Company representative approval status checking</li>
 *   <li>Login help information</li>
 *   <li>Graceful exit and retry mechanisms</li>
 * </ul>
 * 
 */
public class LoginBoundary extends BaseBoundary {
    
    /**
     * Enumeration representing the available user roles in the system.
     * Determines which user interface and permissions are available after login.
     */
    public enum Role { STUDENT, COMPANY_REP, STAFF }

    private final StudentRepository students;
    private final CompanyRepRepository reps;
    private final CareerCenterStaffRepository staff;

    /**
     * Constructs a LoginBoundary with access to all user repositories and authentication services.
     * Initializes the boundary with repositories for all user types to enable user lookup and session creation.
     * 
     * @param students repository of student users for student session creation
     * @param reps repository of company representatives for company rep session creation
     * @param staff repository of career center staff for staff session creation
     * @param auth authentication controller for credential validation and token generation
     * @throws IllegalArgumentException if any repository or auth controller is null
     */
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
     * Performs the interactive login flow with credential prompts and validation.
     * This method guides users through the authentication process with multiple attempts
     * and returns a Session object on successful authentication.
     * 
     * <p>Login Flow:
     * <ol>
     *   <li>Prompts for user ID/email with exit option</li>
     *   <li>Validates input format and emptiness</li>
     *   <li>Prompts for password</li>
     *   <li>Delegates authentication to {@link AuthController}</li>
     *   <li>Creates session on successful authentication</li>
     *   <li>Offers retry option on failure</li>
     * </ol>
     * 
     * @return {@code Optional} containing a {@link Session} on successful authentication,
     *         or {@code Optional.empty()} if login failed, was cancelled, or user lacks approval
     * @throws SecurityException if authentication service is unavailable
     * @see #createSessionFromAuthToken(String, String)
     * @see #promptRetry()
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
     * Prompts the user for password input without validation.
     * This method provides a simple password prompt and returns the entered value.
     * No masking or validation is performed at this level.
     * 
     * @return the password string entered by the user
     */
    private String getPassword() {
        System.out.print("Password: ");
        return sc.nextLine().trim();
    }

    /**
     * Maps an authentication token from {@link AuthController} to a Session object.
     * This method validates the token format, extracts role and user ID, retrieves
     * user details from the appropriate repository, and performs role-specific validation.
     * 
     * <p>Role-specific processing:
     * <ul>
     *   <li><b>Students:</b> Direct session creation if student exists</li>
     *   <li><b>Company Representatives:</b> Checks approval status (approved, pending, or rejected)</li>
     *   <li><b>Staff:</b> Direct session creation if staff exists</li>
     * </ul>
     * 
     * @param authToken authentication token in format "ROLE:userId" returned by AuthController
     * @param userInput original user input (ID or email) for context and error reporting
     * @return {@code Optional} containing a {@link Session} on success, or empty if token is invalid,
     *         user not found, or company representative lacks approval
     * @throws IllegalStateException if token format is invalid, user not found, or role is unknown
     * @see Role
     * @see Session
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
     * Alternative login entry point for programmatic or backward compatibility use.
     * Accepts pre-provided user input instead of prompting, useful for testing
     * or integration with other systems.
     * 
     * @param userInput student ID, company representative email, or staff ID
     * @return {@code Optional} containing a {@link Session} on successful authentication,
     *         or {@code Optional.empty()} if login failed
     * @throws IllegalArgumentException if userInput is null or empty
     * @see #performLogin()
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
     * Prompts the user whether to retry login after a failed attempt.
     * Provides a yes/no confirmation to continue with additional login attempts
     * or return to the previous menu.
     * 
     * @return {@code true} if the user wants to retry login, {@code false} if they want to quit
     * @see BaseBoundary#confirmAction(String)
     */
    private boolean promptRetry() {
        return confirmAction("Would you like to try again?");
    }

    /**
     * Displays comprehensive login help information for all user roles.
     * Provides format specifications for login credentials and default passwords
     * to assist users with authentication.
     * 
     * <p>Help information includes:
     * <ul>
     *   <li>Student login format and default password</li>
     *   <li>Company representative requirements and approval process</li>
     *   <li>Staff login format and default password</li>
     * </ul>
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
     * Represents an active authenticated session for a single user in the system.
     * This immutable class holds the authenticated user's information and provides
     * convenient access to user details. Exactly one actor type (student, rep, or staff)
     * is non-null, determined by the {@link Role}.
     * 
     * <p>Session objects are used to:
     * <ul>
     *   <li>Route users to appropriate role-specific interfaces</li>
     *   <li>Provide user context for authorization checks</li>
     *   <li>Display user information in the UI</li>
     *   <li>Track active user sessions</li>
     * </ul>
     * 
     * @author Your Name
     * @version 1.0
     * @see Role
     */
    public static class Session {
        public final Role role;
        public final Student student;
        public final CompanyRep rep;
        public final CareerCenterStaff staff;

        /**
         * Constructs a session with the authenticated user details.
         * Exactly one of the user type parameters should be non-null, corresponding to the role.
         * 
         * @param role user role determining which actor field should be non-null
         * @param student student user (non-null only if role is STUDENT)
         * @param rep company representative (non-null only if role is COMPANY_REP)
         * @param staff career center staff member (non-null only if role is STAFF)
         * @throws IllegalArgumentException if the role and user type parameters are inconsistent
         */
        public Session(Role role, Student student, CompanyRep rep, CareerCenterStaff staff) {
            this.role = role;
            this.student = student;
            this.rep = rep;
            this.staff = staff;
        }

        /**
         * Gets the user's display name for UI presentation.
         * Returns the full name of the authenticated user or a fallback message
         * if the user entity is unexpectedly null.
         * 
         * @return the user's display name, or "Unknown [Role]" if user not found
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
         * Gets the user's unique identifier for system operations.
         * Returns the appropriate ID based on user role:
         * <ul>
         *   <li>Students: student ID</li>
         *   <li>Company Representatives: email address</li>
         *   <li>Staff: staff ID</li>
         * </ul>
         * 
         * @return the user's ID/email, or null if role is unknown or user entity is null
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

        /**
         * Returns a string representation of the session for debugging and logging.
         * 
         * @return session details including role and user display name
         */
        @Override
        public String toString() {
            return String.format("Session{role=%s, user=%s}", role, getUserDisplayName());
        }
    }
}