package boundary;

import controller.CareerCenterStaffController;
import controller.AuthController;
import entity.CompanyRep;
import entity.Internship;
import entity.InternshipLevel;
import entity.InternshipStatus;
import entity.WithdrawalRequest;
import java.util.List;

/**
 * Console boundary for Career Center staff operations. Allows staff to
 * review and approve internships, company representative registrations and
 * withdrawal requests as well as generate simple reports.
 * <p>
 * This boundary provides the user interface for Career Center staff members to:
 * <ul>
 *   <li>Review and approve/reject pending internship postings</li>
 *   <li>Approve or reject company representative registrations</li>
 *   <li>Process student withdrawal requests for applications</li>
 *   <li>Generate filtered reports on internships</li>
 *   <li>Change their own password</li>
 * </ul>
 * 
 * The interface follows a hierarchical menu structure with comprehensive
 * error handling and user feedback.
 * 
 */
public class CareerCenterStaffBoundary extends BaseBoundary {
    /** Controller for career center staff business operations */
    private final CareerCenterStaffController ctl;

    /**
     * Creates a staff boundary bound to the supplied controller and auth.
     *
     * @param ctl  controller for career center staff operations
     * @param auth authentication controller used for password changes
     */
    public CareerCenterStaffBoundary(CareerCenterStaffController ctl, AuthController auth) {
        super(auth);
        this.ctl = ctl;
    }

    /**
     * Main menu loop for career center staff.
     * <p>
     * Displays the primary navigation menu and handles user input to direct
     * to appropriate functionality. The menu includes:
     * <ul>
     *   <li>Pending internship management</li>
     *   <li>Company representative approval</li>
     *   <li>Withdrawal request processing</li>
     *   <li>Report generation</li>
     *   <li>Password change</li>
     *   <li>Logout</li>
     * </ul>
     * 
     * The method runs in a continuous loop until the user chooses to logout.
     * All exceptions are caught and handled gracefully with user-friendly messages.
     *
     * @param staffId login key of the currently authenticated staff user
     * 
     * @see #handlePendingInternships(String)
     * @see #handlePendingCompanyReps(String)
     * @see #handlePendingWithdrawals(String)
     * @see #handleReports(String)
     */
    public void menu(String staffId) {
        while (true) {
            displaySectionHeader("Career Center Staff Dashboard");
            System.out.println("1. View Pending Internships");
            System.out.println("2. View Pending Company Reps");
            System.out.println("3. View Pending Withdrawal Requests");
            System.out.println("4. Generate Reports");
            System.out.println("9. Change my password");
            System.out.println("0. Logout");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> handlePendingInternships(staffId);
                    case "2" -> handlePendingCompanyReps(staffId);
                    case "3" -> handlePendingWithdrawals(staffId);
                    case "4" -> handleReports(staffId);
                    case "9" -> {
                        boolean changed = changePassword(staffId);
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

    /* ---------- 1) Pending Internships ---------- */
    
    /**
     * Displays pending internship approvals and allows staff to approve or reject.
     * <p>
     * This method:
     * <ol>
     *   <li>Retrieves all internships with PENDING status</li>
     *   <li>Displays them in a numbered list with key details</li>
     *   <li>Allows staff to select an internship for detailed review</li>
     *   <li>Provides options to approve (with visibility choice) or reject</li>
     *   <li>Handles all operations with proper error handling</li>
     * </ol>
     *
     * @param staffId id of the staff member reviewing
     * 
     * @see CareerCenterStaffController#listPendingInternships()
     * @see CareerCenterStaffController#approveInternship(String, String, boolean)
     * @see CareerCenterStaffController#rejectInternship(String, String)
     * @see #displayInternshipDetails(Internship)
     */
    private void handlePendingInternships(String staffId) {
        List<Internship> list = ctl.listPendingInternships();
        if (list.isEmpty()) {
            System.out.println("No pending internships for approval.");
            return;
        }

        displaySectionHeader("Pending Internships");
        for (int i = 0; i < list.size(); i++) {
            Internship it = list.get(i);
            System.out.printf("%d) %s [%s] | %s | Open: %s Close: %s | Slots: %d/%d\n",
                    i + 1,
                    it.getTitle(),
                    it.getLevel(),
                    it.getCompanyName(),
                    it.getOpenDate(),
                    it.getCloseDate(),
                    it.getConfirmedSlots(),
                    it.getMaxSlots());
        }
        
        System.out.print("Select internship to review (0 to back): ");
        int sel = safeInt(sc.nextLine(), 0);
        if (sel <= 0 || sel > list.size()) return;

        Internship chosen = list.get(sel - 1);
        displayInternshipDetails(chosen);
        
        while (true) {
            displaySubSectionHeader("Action Menu");
            System.out.println("1. Approve Internship");
            System.out.println("2. Reject Internship");
            System.out.println("0. Back to List");
            System.out.print("Choice: ");
            String action = sc.nextLine().trim();
            try {
                switch (action) {
                    case "1" -> {
                        boolean vis = confirmAction("Make visible to students immediately?");
                        ctl.approveInternship(staffId, chosen.getInternshipId(), vis);
                        System.out.println(vis
                                ? "Internship approved and made visible to students."
                                : "Internship approved but kept hidden (not visible to students).");
                        return;
                    }
                    case "2" -> {
                        ctl.rejectInternship(staffId, chosen.getInternshipId());
                        System.out.println("Internship rejected.");
                        return;
                    }
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    /* ---------- 2) Pending Company Reps ---------- */
    
    /**
     * Displays pending company representative registrations and allows staff to approve or reject.
     * <p>
     * This method:
     * <ol>
     *   <li>Retrieves all unapproved company representatives</li>
     *   <li>Displays them in a numbered list with contact details</li>
     *   <li>Allows staff to select a representative for detailed review</li>
     *   <li>Provides options to approve or reject (with reason)</li>
     *   <li>Handles all operations with proper error handling</li>
     * </ol>
     *
     * @param staffId id of the staff member reviewing
     * 
     * @see CareerCenterStaffController#listUnapprovedReps()
     * @see CareerCenterStaffController#approveCompanyRep(String, String)
     * @see CareerCenterStaffController#rejectCompanyRep(String, String, String)
     * @see #displayRepDetails(CompanyRep)
     */
    private void handlePendingCompanyReps(String staffId) {
        List<CompanyRep> reps = ctl.listUnapprovedReps();
        if (reps.isEmpty()) {
            System.out.println("No pending company representative registrations.");
            return;
        }

        displaySectionHeader("Pending Company Representatives");
        for (int i = 0; i < reps.size(); i++) {
            CompanyRep r = reps.get(i);
            System.out.printf("%d) %s | %s | %s, %s | Email: %s\n",
                    i + 1, 
                    r.getName(), 
                    r.getCompanyName(), 
                    r.getDepartment(), 
                    r.getPosition(),
                    r.getEmail());
        }
        
        System.out.print("Select representative to review (0 to back): ");
        int sel = safeInt(sc.nextLine(), 0);
        if (sel <= 0 || sel > reps.size()) return;

        CompanyRep chosen = reps.get(sel - 1);
        displayRepDetails(chosen);
        
        while (true) {
            displaySubSectionHeader("Action Menu");
            System.out.println("1. Approve Representative");
            System.out.println("2. Reject Representative");
            System.out.println("0. Back to List");
            System.out.print("Choice: ");
            String action = sc.nextLine().trim();
            try {
                switch (action) {
                    case "1" -> { 
                        ctl.approveCompanyRep(staffId, chosen.getEmail()); 
                        System.out.println("Company Representative approved. They can now login."); 
                        return; 
                    }
                    case "2" -> {
                        System.out.print("Enter rejection reason: ");
                        String reason = sc.nextLine().trim();
                        if (reason.isEmpty()) reason = "Registration rejected by Career Center staff";
                        ctl.rejectCompanyRep(staffId, chosen.getEmail(), reason);
                        System.out.println("Company Representative rejected.");
                        return;
                    }
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    /* ---------- 3) Pending Withdrawal Requests ---------- */
    
    /**
     * Displays pending withdrawal requests and allows staff to approve or reject.
     * <p>
     * This method:
     * <ol>
     *   <li>Retrieves all pending withdrawal requests</li>
     *   <li>Displays them in a numbered list with student and internship details</li>
     *   <li>Allows staff to select a request for detailed review</li>
     *   <li>Provides options to approve or reject (with optional notes)</li>
     *   <li>Handles all operations with proper error handling</li>
     * </ol>
     *
     * @param staffId id of the staff member reviewing
     * 
     * @see CareerCenterStaffController#listPendingWithdrawals()
     * @see CareerCenterStaffController#approveWithdrawal(String, String, String)
     * @see CareerCenterStaffController#rejectWithdrawal(String, String, String)
     * @see #displayWithdrawalDetails(WithdrawalRequest)
     */
    private void handlePendingWithdrawals(String staffId) {
        List<WithdrawalRequest> list = ctl.listPendingWithdrawals();
        if (list.isEmpty()) {
            System.out.println("No pending withdrawal requests.");
            return;
        }

        displaySectionHeader("Pending Withdrawal Requests");
        for (int i = 0; i < list.size(); i++) {
            WithdrawalRequest wr = list.get(i);
            var app = wr.getApplication();
            var s = app.getStudent();
            var it = app.getInternship();
            System.out.printf("%d) %s | Student: %s (%s) | Internship: %s @ %s\n",
                    i + 1,
                    wr.getRequestId(),
                    s.getName(),
                    s.getUserId(),
                    it.getTitle(),
                    it.getCompanyName());
        }

        System.out.print("Select withdrawal request to review (0 to back): ");
        int sel = safeInt(sc.nextLine(), 0);
        if (sel <= 0 || sel > list.size()) return;

        WithdrawalRequest chosen = list.get(sel - 1);
        displayWithdrawalDetails(chosen);

        while (true) {
            displaySubSectionHeader("Action Menu");
            System.out.println("1. Approve Withdrawal");
            System.out.println("2. Reject Withdrawal");
            System.out.println("0. Back to List");
            System.out.print("Choice: ");
            String action = sc.nextLine().trim();
            try {
                switch (action) {
                    case "1" -> {
                        System.out.print("Enter approval note (optional): ");
                        String note = sc.nextLine().trim();
                        ctl.approveWithdrawal(staffId, chosen.getRequestId(), note);
                        System.out.println("Withdrawal request approved.");
                        return;
                    }
                    case "2" -> {
                        System.out.print("Enter rejection note (optional): ");
                        String note = sc.nextLine().trim();
                        ctl.rejectWithdrawal(staffId, chosen.getRequestId(), note);
                        System.out.println("Withdrawal request rejected.");
                        return;
                    }
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    /* ---------- 4) Reports ---------- */
    
    /**
     * Displays report generation menu allowing filtering by various criteria.
     * <p>
     * Provides a sub-menu for generating different types of internship reports:
     * <ul>
     *   <li>By status (PENDING, APPROVED, REJECTED, etc.)</li>
     *   <li>By preferred major</li>
     *   <li>By company name</li>
     *   <li>By internship level</li>
     *   <li>All internships</li>
     * </ul>
     *
     * @param staffId id of the staff member generating reports
     * 
     * @see #generateInternshipsByStatus()
     * @see #generateInternshipsByMajor()
     * @see #generateInternshipsByCompany()
     * @see #generateInternshipsByLevel()
     * @see #generateAllInternships()
     */
    private void handleReports(String staffId) {
        while (true) {
            displaySectionHeader("Generate Reports");
            System.out.println("1. Internships by Status");
            System.out.println("2. Internships by Major");
            System.out.println("3. Internships by Company");
            System.out.println("4. Internships by Level");
            System.out.println("5. All Internships");
            System.out.println("0. Back to Main Menu");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();
            
            try {
                switch (choice) {
                    case "1" -> generateInternshipsByStatus();
                    case "2" -> generateInternshipsByMajor();
                    case "3" -> generateInternshipsByCompany();
                    case "4" -> generateInternshipsByLevel();
                    case "5" -> generateAllInternships();
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception ex) {
                System.out.println("Error generating report: " + ex.getMessage());
            }
        }
    }

    /**
     * Generates and displays internship report filtered by status.
     * <p>
     * Prompts the user for a status filter and displays all internships
     * matching that status. If no status is provided, shows all internships.
     *
     * @see CareerCenterStaffController#filterInternships(InternshipStatus, String, String, String)
     * @see #displayInternshipReport(List, String)
     */
    private void generateInternshipsByStatus() {
        displaySectionHeader("Internships by Status");
        System.out.print("Enter status (PENDING/APPROVED/REJECTED/FILLED/CLOSED) or leave blank for all: ");
        String statusStr = sc.nextLine().trim().toUpperCase();
        
        InternshipStatus status = null;
        if (!statusStr.isEmpty()) {
            try {
                status = InternshipStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid status. Showing all internships.");
            }
        }
        
        List<Internship> internships = ctl.filterInternships(status, null, null, null);
        displayInternshipReport(internships, "Status: " + (status != null ? status : "ALL"));
    }

    /**
     * Generates and displays internship report filtered by preferred major.
     * <p>
     * Prompts the user for a major filter and displays all internships
     * matching that major. If no major is provided, shows all internships.
     *
     * @see CareerCenterStaffController#filterInternships(InternshipStatus, String, String, String)
     * @see #displayInternshipReport(List, String)
     */
    private void generateInternshipsByMajor() {
        displaySectionHeader("Internships by Major");
        System.out.print("Enter major (CSC/EEE/MAE/etc) or leave blank for all: ");
        String major = sc.nextLine().trim();
        if (major.isEmpty()) major = null;
        
        List<Internship> internships = ctl.filterInternships(null, major, null, null);
        displayInternshipReport(internships, "Major: " + (major != null ? major : "ALL"));
    }

    /**
     * Generates and displays internship report filtered by company name.
     * <p>
     * Prompts the user for a company name filter and displays all internships
     * from that company. If no company is provided, shows all internships.
     *
     * @see CareerCenterStaffController#filterInternships(InternshipStatus, String, String, String)
     * @see #displayInternshipReport(List, String)
     */
    private void generateInternshipsByCompany() {
        displaySectionHeader("Internships by Company");
        System.out.print("Enter company name or leave blank for all: ");
        String company = sc.nextLine().trim();
        if (company.isEmpty()) company = null;
        
        List<Internship> internships = ctl.filterInternships(null, null, company, null);
        displayInternshipReport(internships, "Company: " + (company != null ? company : "ALL"));
    }

    /**
     * Generates and displays internship report filtered by internship level.
     * <p>
     * Prompts the user for a level filter and displays all internships
     * matching that level. If no level is provided, shows all internships.
     *
     * @see CareerCenterStaffController#filterInternships(InternshipStatus, String, String, String)
     * @see #displayInternshipReport(List, String)
     */
    private void generateInternshipsByLevel() {
        displaySectionHeader("Internships by Level");
        System.out.print("Enter level (BASIC/INTERMEDIATE/ADVANCED) or leave blank for all: ");
        String levelStr = sc.nextLine().trim().toUpperCase();
        
        String level = null;
        if (!levelStr.isEmpty()) {
            try {
                InternshipLevel.valueOf(levelStr);
                level = levelStr;
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid level. Showing all internships.");
            }
        }
        
        List<Internship> internships = ctl.filterInternships(null, null, null, level);
        displayInternshipReport(internships, "Level: " + (level != null ? level : "ALL"));
    }

    /**
     * Generates and displays a report of all internships in the system.
     * <p>
     * Shows comprehensive information about every internship regardless of
     * status, visibility, or other attributes.
     *
     * @see CareerCenterStaffController#listAllInternships()
     * @see #displayInternshipReport(List, String)
     */
    private void generateAllInternships() {
        List<Internship> internships = ctl.listAllInternships();
        displayInternshipReport(internships, "ALL INTERNSHIPS");
    }

    /**
     * Displays internship report with given filter description.
     * <p>
     * Formats and displays internship data in a consistent tabular format
     * including key attributes like title, company, level, status, and slot usage.
     *
     * @param internships list of internships to display
     * @param filter filter description for header (e.g., "Status: APPROVED")
     */
    private void displayInternshipReport(List<Internship> internships, String filter) {
        displaySectionHeader("Internship Report - " + filter);
        System.out.println("Total: " + internships.size() + " internship(s)");
        
        if (internships.isEmpty()) {
            System.out.println("No internships match the criteria.");
            return;
        }

        for (int i = 0; i < internships.size(); i++) {
            Internship it = internships.get(i);
            System.out.printf("%d) %s | %s | %s | Status: %s | Visible: %s | Slots: %d/%d\n",
                    i + 1,
                    it.getTitle(),
                    it.getCompanyName(),
                    it.getLevel(),
                    it.getStatus(),
                    it.isVisible() ? "Yes" : "No",
                    it.getConfirmedSlots(),
                    it.getMaxSlots());
        }
    }

    /* ---------- Display Helpers ---------- */
    
    /**
     * Displays detailed information about an internship.
     * <p>
     * Shows comprehensive details including all stored attributes of the internship
     * for review purposes during approval processes.
     *
     * @param internship internship to display
     */
    private void displayInternshipDetails(Internship internship) {
        displaySectionHeader("Internship Details");
        System.out.println("ID: " + internship.getInternshipId());
        System.out.println("Title: " + internship.getTitle());
        System.out.println("Company: " + internship.getCompanyName());
        System.out.println("Level: " + internship.getLevel());
        System.out.println("Preferred Major: " + internship.getPreferredMajor());
        System.out.println("Description: " + internship.getDescription());
        System.out.println("Open Date: " + internship.getOpenDate());
        System.out.println("Close Date: " + internship.getCloseDate());
        System.out.println("Status: " + internship.getStatus());
        System.out.println("Visible: " + (internship.isVisible() ? "Yes" : "No"));
        System.out.println("Slots: " + internship.getConfirmedSlots() + "/" + internship.getMaxSlots());
    }

    /**
     * Displays detailed information about a company representative.
     * <p>
     * Shows comprehensive details including contact information, company details,
     * and approval status for review during registration approval.
     *
     * @param rep company representative to display
     */
    private void displayRepDetails(CompanyRep rep) {
        displaySectionHeader("Company Representative Details");
        System.out.println("Name: " + rep.getName());
        System.out.println("Email: " + rep.getEmail());
        System.out.println("Company: " + rep.getCompanyName());
        System.out.println("Department: " + rep.getDepartment());
        System.out.println("Position: " + rep.getPosition());
        System.out.println("Status: " + (rep.isApproved() ? "APPROVED" : rep.isRejected() ? "REJECTED" : "PENDING"));
        if (rep.isRejected()) {
            System.out.println("Rejection Reason: " + rep.getRejectionReason());
        }
    }

    /**
     * Displays detailed information about a withdrawal request.
     * <p>
     * Shows comprehensive details including student information, internship details,
     * application status, and withdrawal reason for informed decision making.
     *
     * @param wr withdrawal request to display
     */
    private void displayWithdrawalDetails(WithdrawalRequest wr) {
        var app = wr.getApplication();
        var s = app.getStudent();
        var it = app.getInternship();
        
        displaySectionHeader("Withdrawal Request Details");
        System.out.println("Request ID: " + wr.getRequestId());
        System.out.println("Student: " + s.getName() + " (" + s.getUserId() + ")");
        System.out.println("Student Major: " + s.getMajor() + " | Year: " + s.getYearOfStudy());
        System.out.println("Internship: " + it.getTitle() + " @ " + it.getCompanyName());
        System.out.println("Application Status: " + app.getStatus());
        System.out.println("Accepted: " + (app.isStudentAccepted() ? "Yes" : "No"));
        System.out.println("Withdrawal Reason: " + wr.getReason());
        System.out.println("Requested On: " + wr.getRequestedOn());
    }
}