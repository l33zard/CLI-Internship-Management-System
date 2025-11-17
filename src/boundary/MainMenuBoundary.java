package boundary;

import java.util.Optional;

/**
 * Main menu boundary class that serves as the entry point for the Internship Placement Management System.
 * This class provides the top-level navigation menu that allows users to access various system functions
 * including login, company representative registration, help, and system exit.
 * 
 * <p>The main menu acts as a central router that delegates to role-specific boundaries after successful
 * authentication. It manages the overall application flow and user session routing.
 * 
 * <p>Key responsibilities include:
 * <ul>
 *   <li>Displaying the main navigation menu</li>
 *   <li>Handling user authentication and session management</li>
 *   <li>Routing authenticated users to appropriate role-specific interfaces</li>
 *   <li>Managing company representative registration</li>
 *   <li>Providing system help information</li>
 *   <li>Handling system exit with confirmation</li>
 * </ul>
 * 
 */
public class MainMenuBoundary extends BaseBoundary {
    private final LoginBoundary login;
    private final StudentBoundary studentUI;
    private final CompanyRepBoundary repUI;
    private final CareerCenterStaffBoundary staffUI;
    private final CompanyRepRegistrationBoundary registrationUI;

    /**
     * Constructs a new MainMenuBoundary with all required role-specific sub-boundaries.
     * This constructor initializes all UI boundaries needed for the complete application
     * functionality, allowing the main menu to delegate to appropriate interfaces based
     * on user role and selection.
     * 
     * @param login the login boundary responsible for user authentication
     * @param studentUI the student-specific UI boundary for student operations
     * @param repUI the company representative-specific UI boundary for company operations
     * @param staffUI the career center staff-specific UI boundary for administrative operations
     * @param registrationUI the company representative registration boundary for new company account creation
     * @throws IllegalArgumentException if any required boundary is null
     */
    public MainMenuBoundary(LoginBoundary login,
                            StudentBoundary studentUI,
                            CompanyRepBoundary repUI,
                            CareerCenterStaffBoundary staffUI,
                            CompanyRepRegistrationBoundary registrationUI) {
        super(); // No auth controller needed for main menu
        this.login = login;
        this.studentUI = studentUI;
        this.repUI = repUI;
        this.staffUI = staffUI;
        this.registrationUI = registrationUI;
    }

    /**
     * Runs the main menu loop that serves as the primary application entry point.
     * This method displays the main navigation menu and processes user selections
     * in a continuous loop until the user chooses to exit the system.
     * 
     * <p>The menu provides the following options:
     * <ul>
     *   <li><b>Login:</b> Authenticate existing users and route to role-specific interfaces</li>
     *   <li><b>Company Representative Registration:</b> Register new company representatives</li>
     *   <li><b>View Login Help:</b> Display authentication help information</li>
     *   <li><b>Exit System:</b> Terminate the application with confirmation</li>
     * </ul>
     * 
     * <p>This method only returns when the user explicitly chooses to exit the application,
     * ensuring the application remains active until user-initiated termination.
     * 
     * @throws SecurityException if there are issues with boundary delegation
     * @see #handleLogin()
     * @see #confirmExit()
     */
    public void run() {
        System.out.println("INTERNSHIP PLACEMENT MANAGEMENT SYSTEM");

        while (true) {
            displaySectionHeader("MAIN MENU");
            System.out.println("1. Login");
            System.out.println("2. Company Representative Registration");
            System.out.println("3. View Login Help");
            System.out.println("0. Exit System");
            System.out.print("Please enter your choice: ");
            
            String choice = sc.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> handleLogin();
                    case "2" -> {
                        displaySectionHeader("Company Representative Sign Up");
                        registrationUI.signupCompanyRep();
                    }
                    case "3" -> login.displayLoginHelp();
                    case "0" -> {
                        if (confirmExit()) {
                            System.out.println("Bye!");
                            return;
                        }
                    }
                    default -> System.out.println("Invalid choice. Please enter 1, 2, 3, or 0.");
                }
            } catch (Exception ex) {
                System.out.println("An error occurred: " + ex.getMessage());
                System.out.println("Please try again.");
            }
        }
    }

    /**
     * Handles the user login flow and delegates to appropriate role-specific user interfaces.
     * This method coordinates the authentication process and routes successfully authenticated
     * users to their respective role-specific boundaries:
     * 
     * <ul>
     *   <li><b>Students:</b> Directed to {@link StudentBoundary#menu(String)}</li>
     *   <li><b>Company Representatives:</b> Directed to {@link CompanyRepBoundary#menu(String)}</li>
     *   <li><b>Career Center Staff:</b> Directed to {@link CareerCenterStaffBoundary#menu(String)}</li>
     * </ul>
     * 
     * <p>If login fails or is cancelled by the user, control returns to the main menu.
     * After the user completes their session in a role-specific boundary, they are returned
     * to the main menu with a welcome message.
     * 
     * @throws IllegalStateException if there is an issue accessing the user portal after login
     * @see LoginBoundary#performLogin()
     * @see LoginBoundary.Session
     */
    private void handleLogin() {
        displaySectionHeader("User Login");

        Optional<LoginBoundary.Session> session = login.performLogin();
        
        if (session.isPresent()) {
            LoginBoundary.Session sess = session.get();
            
            try {
                switch (sess.role) {
                    case STUDENT -> {
                        studentUI.menu(sess.student.getUserId());
                    }
                    case COMPANY_REP -> {
                        repUI.menu(sess.rep.getEmail());
                    }
                    case STAFF -> {
                        staffUI.menu(sess.staff.getUserId());
                    }
                }
                System.out.println("Welcome back to the main menu");
            } catch (Exception ex) {
                System.out.println("Error accessing user portal: " + ex.getMessage());
            }
        } else {
            System.out.println("Returning to main menu...");
        }
    }

    /**
     * Prompts the user to confirm system exit to prevent accidental termination.
     * This method provides a safety mechanism by requiring explicit confirmation
     * before allowing the application to terminate, reducing the risk of data loss
     * or unintended closure.
     * 
     * @return {@code true} if the user confirms they want to exit the system,
     *         {@code false} if the user chooses to remain in the application
     * @see BaseBoundary#confirmAction(String)
     */
    private boolean confirmExit() {
        return confirmAction("Are you sure you want to exit the system?");
    }
}