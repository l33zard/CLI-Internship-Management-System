package boundary;

import java.util.Optional;

public class MainMenuBoundary extends BaseBoundary {
    private final LoginBoundary login;
    private final StudentBoundary studentUI;
    private final CompanyRepBoundary repUI;
    private final CareerCenterStaffBoundary staffUI;
    private final CompanyRepRegistrationBoundary registrationUI;

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

    private boolean confirmExit() {
        return confirmAction("Are you sure you want to exit the system?");
    }
}