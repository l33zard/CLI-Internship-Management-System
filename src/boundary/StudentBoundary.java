package boundary;

import controller.AuthController;
import controller.StudentController;
import entity.ApplicationStatus;
import entity.Internship;
import entity.InternshipApplication;
import entity.WithdrawalRequest;
import entity.WithdrawalRequestStatus;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console boundary class for student-facing interactions in the internship management system.
 * This class provides the student dashboard interface and delegates business operations
 * to the {@link StudentController}. It handles all student-related menu navigation,
 * application management, and withdrawal request processing through a console-based UI.
 * 
 * <p>Key responsibilities include:
 * <ul>
 *   <li>Displaying eligible internships for application</li>
 *   <li>Managing student internship applications (view, accept offers, withdraw)</li>
 *   <li>Handling withdrawal request creation and tracking</li>
 *   <li>Providing password change functionality</li>
 * </ul>
 * 
 */
public class StudentBoundary extends BaseBoundary {
    private final StudentController ctl;

    /**
     * Creates a new StudentBoundary instance bound to the specified controllers.
     * 
     * @param ctl  the student controller that implements core student operations
     * @param auth the authentication controller used for password management
     * @throws IllegalArgumentException if either controller is null
     */
    public StudentBoundary(StudentController ctl, AuthController auth) {
        super(auth);
        this.ctl = ctl;
    }

    /**
     * Main student menu loop that presents the student dashboard and handles user interaction.
     * This method displays the primary navigation menu and processes user selections
     * for various student operations. The loop continues until the user chooses to logout.
     * 
     * <p>Menu options include:
     * <ul>
     *   <li>View Available Internships</li>
     *   <li>View My Applications</li>
     *   <li>View My Withdrawal Requests</li>
     *   <li>Change Password</li>
     *   <li>Logout</li>
     * </ul>
     * 
     * @param studentId the unique identifier (login key) of the currently authenticated student
     * @throws SecurityException if the studentId is invalid or access is denied
     */
    public void menu(String studentId) {
        while (true) {
            displaySectionHeader("Student Dashboard");
            System.out.println("1. View Available Internships");
            System.out.println("2. View My Applications");
            System.out.println("3. View My Withdrawal Requests");
            System.out.println("9. Change my password");
            System.out.println("0. Logout");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> handleAvailableInternships(studentId);
                    case "2" -> handleMyApplications(studentId);
                    case "3" -> handleMyWithdrawalRequests(studentId);
                    case "9" -> {
                        boolean changed = changePassword(studentId);
                        if (changed) return;
                    }
                    case "0" -> {
                        System.out.println("Logging out...");
                        return;
                    }
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
                System.out.println("Please try again.");
            }
        }
    }

    /* ---------- Available Internships ---------- */
    
    /**
     * Displays eligible internships and allows students to apply for specific opportunities.
     * This method retrieves all internships that match the student's eligibility criteria,
     * displays them in a formatted list, and provides the option to view details and apply.
     * 
     * <p>The method checks application limits and placement status before allowing applications:
     * <ul>
     *   <li>Maximum of 3 active applications allowed</li>
     *   <li>Cannot apply if student has already accepted a placement</li>
     * </ul>
     * 
     * @param studentId the unique identifier of the student
     * @throws IllegalStateException if the student cannot apply (limit reached or placement accepted)
     * @see StudentController#viewEligibleInternships(String)
     * @see StudentController#canApplyMore(String)
     * @see StudentController#hasConfirmedPlacement(String)
     * @see StudentController#applyForInternship(String, String)
     */
    private void handleAvailableInternships(String studentId) {
        List<Internship> internships = ctl.viewEligibleInternships(studentId);
        
        displaySectionHeader("All Eligible Internships");
        System.out.println("Found " + internships.size() + " internship(s)");
        
        if (internships.isEmpty()) {
            System.out.println("No internships available that match your profile.");
            System.out.println("Check back later for new opportunities.");
            return;
        }

        for (int i = 0; i < internships.size(); i++) {
            Internship it = internships.get(i);
            System.out.printf("%d) %s | %s | %s | Closes: %s\n",
                    i + 1,
                    it.getTitle(),
                    it.getLevel(),
                    it.getCompanyName(),
                    it.getCloseDate());
        }

        System.out.print("Select internship to apply (0 to back): ");
        int sel = safeInt(sc.nextLine(), 0);
        if (sel <= 0 || sel > internships.size()) return;

        Internship chosen = internships.get(sel - 1);
        displayInternshipDetails(chosen);
        
        boolean canApply = ctl.canApplyMore(studentId);
        
        if (!canApply) {
            System.out.println("You have reached the maximum of 3 active applications.");
            System.out.println("You cannot apply for more internships at this time.");
            return;
        }

        if (ctl.hasConfirmedPlacement(studentId)) {
            System.out.println("You have already accepted an internship placement.");
            System.out.println("You cannot apply for more internships.");
            return;
        }

        boolean apply = confirmAction("Apply for this internship?");
        if (apply) {
            try {
                String appId = ctl.applyForInternship(studentId, chosen.getInternshipId());
                System.out.println("Application submitted successfully!");
                System.out.println("Application ID: " + appId);
            } catch (Exception ex) {
                System.out.println("Error applying: " + ex.getMessage());
            }
        }
    }

    /* ---------- My Applications ---------- */
    
    /**
     * Displays the applications submenu for viewing applications filtered by status or all applications.
     * This method serves as the entry point for application management, allowing students
     * to navigate between different views of their applications based on status.
     * 
     * @param studentId the unique identifier of the student
     * @see StudentController#viewMyApplications(String)
     * @see ApplicationStatus
     */
    private void handleMyApplications(String studentId) {
        while (true) {
            List<InternshipApplication> myApps = ctl.viewMyApplications(studentId);
            
            if (myApps.isEmpty()) {
                System.out.println("You have no applications yet.");
                return;
            }

            displaySectionHeader("VIEW APPLICATIONS");
            System.out.println("1. View Pending Applications");
            System.out.println("2. View Successful Applications"); 
            System.out.println("3. View Unsuccessful Applications");
            System.out.println("4. View Withdrawn Applications");
            System.out.println("5. View All Applications");
            System.out.println("0. Back to Main Menu");
            System.out.print("Choice: ");
            
            String choice = sc.nextLine().trim();
            
            switch (choice) {
                case "1" -> handleApplicationStatusView(studentId, myApps, ApplicationStatus.PENDING, "PENDING APPLICATIONS");
                case "2" -> handleApplicationStatusView(studentId, myApps, ApplicationStatus.SUCCESSFUL, "SUCCESSFUL APPLICATIONS");
                case "3" -> handleApplicationStatusView(studentId, myApps, ApplicationStatus.UNSUCCESSFUL, "UNSUCCESSFUL APPLICATIONS");
                case "4" -> handleApplicationStatusView(studentId, myApps, ApplicationStatus.WITHDRAWN, "WITHDRAWN APPLICATIONS");
                case "5" -> handleAllApplicationsView(studentId, myApps);
                case "0" -> { return; }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Displays applications filtered by a specific status and allows management of selected applications.
     * 
     * @param studentId the unique identifier of the student
     * @param allApps all student applications (unfiltered)
     * @param status the application status to filter by
     * @param title the display title for the section header
     * @see ApplicationStatus
     */
    private void handleApplicationStatusView(String studentId, List<InternshipApplication> allApps, 
                                           ApplicationStatus status, String title) {
        List<InternshipApplication> filteredApps = allApps.stream()
                .filter(app -> app.getStatus() == status)
                .collect(Collectors.toList());
        
        if (filteredApps.isEmpty()) {
            System.out.println("No " + title.toLowerCase() + " found.");
            return;
        }
        
        displaySectionHeader(title);
        for (int i = 0; i < filteredApps.size(); i++) {
            InternshipApplication app = filteredApps.get(i);
            String acceptedIndicator = app.isStudentAccepted() ? "ACCEPTED" : "";
            System.out.printf("%d) %s | %s | %s | %s %s\n",
                    i + 1,
                    app.getApplicationId(),
                    app.getInternship().getTitle(),
                    app.getInternship().getCompanyName(),
                    app.getStatus(),
                    acceptedIndicator);
        }
        
        System.out.print("Select application to manage (0 to go back): ");
        int selection = safeInt(sc.nextLine(), 0);
        
        if (selection > 0 && selection <= filteredApps.size()) {
            InternshipApplication selectedApp = filteredApps.get(selection - 1);
            handleApplicationManagement(studentId, selectedApp);
        }
    }

    /**
     * Displays all applications across all statuses and allows management of selected applications.
     * 
     * @param studentId the unique identifier of the student
     * @param allApps all student applications across all statuses
     */
    private void handleAllApplicationsView(String studentId, List<InternshipApplication> allApps) {
        if (allApps.isEmpty()) {
            System.out.println("No applications found.");
            return;
        }
        
        displaySectionHeader("ALL APPLICATIONS");
        for (int i = 0; i < allApps.size(); i++) {
            InternshipApplication app = allApps.get(i);
            String acceptedIndicator = app.isStudentAccepted() ? "ACCEPTED" : "";
            System.out.printf("%d) %s | %s | %s | %s | %s\n",
                    i + 1,
                    app.getApplicationId(),
                    app.getInternship().getTitle(),
                    app.getInternship().getCompanyName(),
                    app.getStatus(),
                    acceptedIndicator);
        }
        
        System.out.print("Select application to manage (0 to go back): ");
        int selection = safeInt(sc.nextLine(), 0);
        
        if (selection > 0 && selection <= allApps.size()) {
            InternshipApplication selectedApp = allApps.get(selection - 1);
            handleApplicationManagement(studentId, selectedApp);
        }
    }

    /**
     * Displays detailed application information and provides context-appropriate actions.
     * Available actions depend on the application status and acceptance state:
     * <ul>
     *   <li>PENDING: Can request withdrawal</li>
     *   <li>SUCCESSFUL (not accepted): Can accept offer or request withdrawal</li>
     *   <li>SUCCESSFUL (accepted): Can request withdrawal with special handling</li>
     * </ul>
     * 
     * @param studentId the unique identifier of the student
     * @param app the application to manage
     * @throws IllegalStateException if invalid actions are attempted for the current state
     * @see StudentController#getApplication(String, String)
     * @see StudentController#confirmAcceptance(String, String)
     */
    private void handleApplicationManagement(String studentId, InternshipApplication app) {
        while (true) {
            app = ctl.getApplication(studentId, app.getApplicationId());
            displaySectionHeader("Application Details");
            System.out.println("Application ID: " + app.getApplicationId());
            System.out.println("Internship: " + app.getInternship().getTitle());
            System.out.println("Company: " + app.getInternship().getCompanyName());
            System.out.println("Status: " + app.getStatus());
            System.out.println("Applied On: " + app.getAppliedOn());
            System.out.println("Accepted: " + (app.isStudentAccepted() ? "Yes" : "No"));
            
            displaySubSectionHeader("Available Actions");
            
            // Show available actions based on application status
            if (app.getStatus() == ApplicationStatus.PENDING) {
                System.out.println("1. Request Withdrawal");
                System.out.println("0. Back to Applications");
            } else if (app.getStatus() == ApplicationStatus.SUCCESSFUL && !app.isStudentAccepted()) {
                System.out.println("1. Accept Offer");
                System.out.println("2. Request Withdrawal");
                System.out.println("0. Back to Applications");
            } else if (app.getStatus() == ApplicationStatus.SUCCESSFUL && app.isStudentAccepted()) {
                System.out.println("1. Request Withdrawal (After Acceptance)");
                System.out.println("0. Back to Applications");
            } else {
                System.out.println("0. Back to Applications");
            }
            
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();
            
            try {
                switch (choice) {
                    case "1" -> {
                        if (app.getStatus() == ApplicationStatus.PENDING) {
                            handleWithdrawalRequest(studentId, app);
                            return;
                        } else if (app.getStatus() == ApplicationStatus.SUCCESSFUL && !app.isStudentAccepted()) {
                            ctl.confirmAcceptance(studentId, app.getApplicationId());
                            System.out.println("Offer accepted successfully!");
                            return;
                        } else if (app.getStatus() == ApplicationStatus.SUCCESSFUL && app.isStudentAccepted()) {
                            handleWithdrawalRequest(studentId, app);
                            return;
                        }
                    }
                    case "2" -> {
                        if (app.getStatus() == ApplicationStatus.SUCCESSFUL && !app.isStudentAccepted()) {
                            handleWithdrawalRequest(studentId, app);
                            return;
                        }
                    }
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    /* ---------- Withdrawal Requests ---------- */
    
    /**
     * Displays the withdrawal requests submenu for viewing requests filtered by status or all requests.
     * This method provides navigation between different views of withdrawal requests
     * based on their processing status.
     * 
     * @param studentId the unique identifier of the student
     * @see StudentController#viewMyWithdrawalRequests(String)
     * @see WithdrawalRequestStatus
     */
    private void handleMyWithdrawalRequests(String studentId) {
        while (true) {
            List<WithdrawalRequest> allWithdrawals = ctl.viewMyWithdrawalRequests(studentId);
            
            displaySectionHeader("VIEW WITHDRAWAL REQUESTS");
            System.out.println("1. View Pending Withdrawal Requests");
            System.out.println("2. View Approved Withdrawal Requests");
            System.out.println("3. View Rejected Withdrawal Requests");
            System.out.println("4. View All Withdrawal Requests");
            System.out.println("0. Back to Main Menu");
            System.out.print("Choice: ");
            
            String choice = sc.nextLine().trim();
            
            // Check if there are any withdrawals before proceeding
            if (allWithdrawals.isEmpty()) {
                System.out.println("You have no withdrawal requests.");
                if (!choice.equals("0")) {
                    pressEnterToContinue();
                }
                if (choice.equals("0")) {
                    return;
                }
                continue;
            }
            
            switch (choice) {
                case "1" -> handleWithdrawalStatusView(studentId, allWithdrawals, WithdrawalRequestStatus.PENDING, "PENDING WITHDRAWAL REQUESTS");
                case "2" -> handleWithdrawalStatusView(studentId, allWithdrawals, WithdrawalRequestStatus.APPROVED, "APPROVED WITHDRAWAL REQUESTS");
                case "3" -> handleWithdrawalStatusView(studentId, allWithdrawals, WithdrawalRequestStatus.REJECTED, "REJECTED WITHDRAWAL REQUESTS");
                case "4" -> handleAllWithdrawalsView(studentId, allWithdrawals);
                case "0" -> { return; }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Displays withdrawal requests filtered by a specific status.
     * 
     * @param studentId the unique identifier of the student
     * @param allWithdrawals all student withdrawal requests (unfiltered)
     * @param status the withdrawal request status to filter by
     * @param title the display title for the section header
     * @see WithdrawalRequestStatus
     */
    private void handleWithdrawalStatusView(String studentId, List<WithdrawalRequest> allWithdrawals, 
                                          WithdrawalRequestStatus status, String title) {
        List<WithdrawalRequest> filteredWithdrawals = allWithdrawals.stream()
                .filter(wr -> wr.getStatus() == status)
                .collect(Collectors.toList());
        
        if (filteredWithdrawals.isEmpty()) {
            System.out.println("No " + title.toLowerCase() + " found.");
            return;
        }
        
        displaySectionHeader(title);
        for (int i = 0; i < filteredWithdrawals.size(); i++) {
            WithdrawalRequest wr = filteredWithdrawals.get(i);
            var app = wr.getApplication();
            System.out.printf("%d) %s | %s @ %s | Status: %s | Requested: %s\n",
                    i + 1,
                    wr.getRequestId(),
                    app.getInternship().getTitle(),
                    app.getInternship().getCompanyName(),
                    wr.getStatus(),
                    wr.getRequestedOn());
        }
        
        System.out.print("Select request to view details (0 to go back): ");
        int selection = safeInt(sc.nextLine(), 0);
        
        if (selection > 0 && selection <= filteredWithdrawals.size()) {
            WithdrawalRequest selectedWithdrawal = filteredWithdrawals.get(selection - 1);
            displayWithdrawalDetails(selectedWithdrawal);
        }
    }

    /**
     * Displays all withdrawal requests across all statuses.
     * 
     * @param studentId the unique identifier of the student
     * @param allWithdrawals all student withdrawal requests across all statuses
     */
    private void handleAllWithdrawalsView(String studentId, List<WithdrawalRequest> allWithdrawals) {
        displaySectionHeader("ALL WITHDRAWAL REQUESTS");
        for (int i = 0; i < allWithdrawals.size(); i++) {
            WithdrawalRequest wr = allWithdrawals.get(i);
            var app = wr.getApplication();
            System.out.printf("%d) %s | %s @ %s | Status: %s | Requested: %s\n",
                    i + 1,
                    wr.getRequestId(),
                    app.getInternship().getTitle(),
                    app.getInternship().getCompanyName(),
                    wr.getStatus(),
                    wr.getRequestedOn());
        }
        
        System.out.print("Select request to view details (0 to go back): ");
        int selection = safeInt(sc.nextLine(), 0);
        
        if (selection > 0 && selection <= allWithdrawals.size()) {
            WithdrawalRequest selectedWithdrawal = allWithdrawals.get(selection - 1);
            displayWithdrawalDetails(selectedWithdrawal);
        }
    }

    /**
     * Prompts the user for a withdrawal reason and submits a withdrawal request for the specified application.
     * This method validates that the reason is not empty before submitting the request to the controller.
     * 
     * @param studentId the unique identifier of the student
     * @param app the application to request withdrawal for
     * @throws IllegalArgumentException if the withdrawal reason is empty
     * @see StudentController#requestWithdrawal(String, String, String)
     */
    private void handleWithdrawalRequest(String studentId, InternshipApplication app) {
        System.out.print("Enter withdrawal reason: ");
        String reason = sc.nextLine().trim();
        
        if (reason.isEmpty()) {
            System.out.println("Withdrawal reason cannot be empty.");
            return;
        }
        
        try {
            String withdrawalId = ctl.requestWithdrawal(studentId, app.getApplicationId(), reason);
            System.out.println("Withdrawal request submitted successfully!");
            System.out.println("Request ID: " + withdrawalId);
        } catch (Exception e) {
            System.out.println("Error submitting withdrawal request: " + e.getMessage());
        }
    }

    /**
     * Displays detailed information about a withdrawal request including processing status and staff notes.
     * Shows comprehensive information about the withdrawal request, including the associated application,
     * reason for withdrawal, processing status, and any staff comments if processed.
     * 
     * @param wr the withdrawal request to display
     */
    private void displayWithdrawalDetails(WithdrawalRequest wr) {
        var app = wr.getApplication();
        
        displaySectionHeader("Withdrawal Request Details");
        System.out.println("Request ID: " + wr.getRequestId());
        System.out.println("Application: " + app.getInternship().getTitle() + " @ " + app.getInternship().getCompanyName());
        System.out.println("Application Status: " + app.getStatus());
        System.out.println("Accepted: " + (app.isStudentAccepted() ? "Yes" : "No"));
        System.out.println("Withdrawal Reason: " + wr.getReason());
        System.out.println("Requested On: " + wr.getRequestedOn());
        System.out.println("Status: " + wr.getStatus());
        
        if (wr.getProcessedBy() != null) {
            System.out.println("Processed By: " + wr.getProcessedBy().getName());
            System.out.println("Processed On: " + wr.getProcessedOn());
            System.out.println("Staff Note: " + wr.getStaffNote());
        }        
        pressEnterToContinue();
    }

    /* ---------- Display Helpers ---------- */
    
    /**
     * Displays complete internship details including company information, requirements, and availability.
     * Presents a comprehensive view of internship information to help students make informed
     * application decisions.
     * 
     * @param internship the internship to display details for
     */
    private void displayInternshipDetails(Internship internship) {
        displaySectionHeader("Internship Details");
        System.out.println("Title: " + internship.getTitle());
        System.out.println("Company: " + internship.getCompanyName());
        System.out.println("Level: " + internship.getLevel());
        System.out.println("Preferred Major: " + internship.getPreferredMajor());
        System.out.println("Description: " + internship.getDescription());
        System.out.println("Open Date: " + internship.getOpenDate());
        System.out.println("Close Date: " + internship.getCloseDate());
        System.out.println("Available Slots: " + (internship.getMaxSlots() - internship.getConfirmedSlots()) + "/" + internship.getMaxSlots());
    }
}