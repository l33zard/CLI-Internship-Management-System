package boundary;

import controller.AuthController;
import controller.CompanyRepController;
import entity.Internship;
import entity.InternshipApplication;
import entity.InternshipLevel;
import entity.InternshipStatus;
import entity.Student;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Boundary handling console interactions for company representatives.
 * Provides menus to create and manage internships and view applications.
 * <p>
 * This boundary provides the user interface for company representatives to:
 * <ul>
 *   <li>View and manage their existing internship postings</li>
 *   <li>Create new internship postings (pending Career Center approval)</li>
 *   <li>Review and process student applications</li>
 *   <li>Manage internship visibility and details</li>
 *   <li>Delete draft internship postings</li>
 * </ul>
 * 
 * The interface follows a hierarchical menu structure with comprehensive
 * error handling and input validation for all user interactions.
 * 
 */
public class CompanyRepBoundary extends BaseBoundary {
    /** Controller for company representative business operations */
    private final CompanyRepController ctl;

    /**
     * Constructs a company representative boundary.
     *
     * @param ctl  controller implementing company representative operations
     * @param auth authentication controller used for password changes
     */
    public CompanyRepBoundary(CompanyRepController ctl, AuthController auth) {
        super(auth);
        this.ctl = ctl;
    }

    /**
     * Main menu loop for a company representative.
     * <p>
     * Displays the primary navigation menu and handles user input to direct
     * to appropriate functionality. The menu includes:
     * <ul>
     *   <li>View and manage existing internships</li>
     *   <li>Create new internship postings</li>
     *   <li>Change password</li>
     *   <li>Logout</li>
     * </ul>
     * 
     * The method runs in a continuous loop until the user chooses to logout.
     * All exceptions are caught and handled gracefully with user-friendly messages.
     *
     * @param repEmail login key (email) of the currently authenticated representative
     * 
     * @see #handleInternships(String)
     * @see #handleCreateInternship(String)
     */
    public void menu(String repEmail) {
        while (true) {
            displaySectionHeader("Company Representative Dashboard");
            System.out.println("1. View My Internships");
            System.out.println("2. Create New Internship");
            System.out.println("9. Change my password");
            System.out.println("0. Logout");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> handleInternships(repEmail);
                    case "2" -> handleCreateInternship(repEmail);
                    case "9" -> {
                        boolean changed = changePassword(repEmail);
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

    /* ---------- My Internships ---------- */
    
    /**
     * Displays list of company representative's internships and allows selection for management.
     * <p>
     * This method:
     * <ol>
     *   <li>Retrieves all internships belonging to the representative's company</li>
     *   <li>Displays them in a numbered list with key details (title, level, status, visibility, slots)</li>
     *   <li>Allows the representative to select an internship for detailed management</li>
     *   <li>Handles navigation back to the main menu</li>
     * </ol>
     *
     * @param repEmail email of the representative
     * 
     * @see CompanyRepController#listInternships(String)
     * @see #handleInternshipActions(String, Internship)
     */
    private void handleInternships(String repEmail) {
        List<Internship> myPosts = ctl.listInternships(repEmail);
        if (myPosts.isEmpty()) {
            System.out.println("You have no internships yet. Create your first internship.");
            return;
        }

        displaySectionHeader("My Internships");
        for (int i = 0; i < myPosts.size(); i++) {
            Internship it = myPosts.get(i);
            System.out.printf("%d) %s [%s] | Status: %s | Visible: %s | Slots: %d/%d\n",
                    i + 1, 
                    it.getTitle(), 
                    it.getLevel(),
                    it.getStatus(),
                    it.isVisible() ? "Yes" : "No",
                    it.getConfirmedSlots(),
                    it.getMaxSlots());
        }

        System.out.print("Select internship (0 to back): ");
        int sel = safeInt(sc.nextLine(), 0);
        if (sel <= 0 || sel > myPosts.size()) return;

        Internship chosen = myPosts.get(sel - 1);
        handleInternshipActions(repEmail, chosen);
    }

    /**
     * Displays action menu for managing a specific internship (visibility, applications, edit, delete).
     * <p>
     * This method provides a comprehensive management interface for individual internships:
     * <ul>
     *   <li>Set visibility (for approved internships only)</li>
     *   <li>View and process applications</li>
     *   <li>Edit internship details (for editable internships only)</li>
     *   <li>Delete internship (for deletable internships only)</li>
     * </ul>
     * 
     * The internship object is refreshed after operations that modify its state.
     *
     * @param repEmail email of the representative
     * @param internship internship to manage
     * 
     * @see #handleApplications(String, Internship)
     * @see #handleEditInternship(String, Internship)
     * @see #handleDeleteInternship(String, Internship)
     * @see CompanyRepController#setVisibility(String, String, boolean)
     * @see CompanyRepController#getInternship(String, String)
     */
    private void handleInternshipActions(String repEmail, Internship internship) {
        while (true) {
            displayInternshipDetails(internship);
            System.out.println("\n--- Action Menu ---");
            System.out.println("1. Set Visibility");
            System.out.println("2. View Applications");
            System.out.println("3. Edit Internship");
            System.out.println("4. Delete Internship");
            System.out.println("0. Back to List");
            System.out.print("Choice: ");
            String act = sc.nextLine().trim();
            try {
                switch (act) {
                    case "1" -> {
                        if (internship.getStatus() != InternshipStatus.APPROVED) {
                            System.out.println("Only approved internships can have visibility changed.");
                            continue;
                        }
                        boolean vis = promptBoolean("Make visible to students? (1 = Yes, 0 = No): ");
                        ctl.setVisibility(repEmail, internship.getInternshipId(), vis);
                        System.out.println(vis ? "Set to visible." : "Set to hidden.");
                        // Refresh the internship object
                        internship = ctl.getInternship(repEmail, internship.getInternshipId());
                    }
                    case "2" -> {
                        handleApplications(repEmail, internship);
                        return; // back to list after viewing apps
                    }
                    case "3" -> {
                        handleEditInternship(repEmail, internship);
                        // Refresh after edit
                        internship = ctl.getInternship(repEmail, internship.getInternshipId());
                    }
                    case "4" -> {
                        handleDeleteInternship(repEmail, internship);
                        return; // back to list after deletion
                    }
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    /* ---------- View Applications for One Internship ---------- */
    
    /**
     * Displays applications for a specific internship grouped by status.
     * <p>
     * This method:
     * <ol>
     *   <li>Retrieves all applications for the specified internship</li>
     *   <li>Groups applications by status (PENDING, SUCCESSFUL, UNSUCCESSFUL)</li>
     *   <li>Displays each group with formatted application information</li>
     *   <li>Allows selection of individual applications for detailed review</li>
     * </ol>
     *
     * @param repEmail email of the representative
     * @param internship internship to view applications for
     * 
     * @see CompanyRepController#listApplications(String, String)
     * @see #displayApplications(List)
     * @see #handleApplicationActions(String, InternshipApplication)
     */
    private void handleApplications(String repEmail, Internship internship) {
        List<InternshipApplication> apps = ctl.listApplications(repEmail, internship.getInternshipId());
        if (apps.isEmpty()) {
            System.out.println("No applications yet for this internship.");
            return;
        }

        displaySectionHeader("Applications for: " + internship.getTitle());
        
        // Filter applications by status
        List<InternshipApplication> pendingApps = apps.stream()
                .filter(a -> a.getStatus() == entity.ApplicationStatus.PENDING)
                .toList();
        List<InternshipApplication> successfulApps = apps.stream()
                .filter(a -> a.getStatus() == entity.ApplicationStatus.SUCCESSFUL)
                .toList();
        List<InternshipApplication> unsuccessfulApps = apps.stream()
                .filter(a -> a.getStatus() == entity.ApplicationStatus.UNSUCCESSFUL)
                .toList();

        if (!pendingApps.isEmpty()) {
            displaySubSectionHeader("PENDING APPLICATIONS");
            displayApplications(pendingApps);
        }
        if (!successfulApps.isEmpty()) {
            displaySubSectionHeader("SUCCESSFUL APPLICATIONS");
            displayApplications(successfulApps);
        }
        if (!unsuccessfulApps.isEmpty()) {
            displaySubSectionHeader("UNSUCCESSFUL APPLICATIONS");
            displayApplications(unsuccessfulApps);
        }

        System.out.print("Select application to review (0 to back): ");
        int sel = safeInt(sc.nextLine(), 0);
        if (sel <= 0 || sel > apps.size()) return;

        InternshipApplication chosen = apps.get(sel - 1);
        handleApplicationActions(repEmail, chosen);
    }

    /**
     * Displays list of applications in formatted output.
     * <p>
     * Shows each application with:
     * <ul>
     *   <li>Application ID</li>
     *   <li>Student name, ID, and major</li>
     *   <li>Application status</li>
     *   <li>Acceptance indicator</li>
     * </ul>
     *
     * @param apps applications to display
     */
    private void displayApplications(List<InternshipApplication> apps) {
        for (int i = 0; i < apps.size(); i++) {
            InternshipApplication a = apps.get(i);
            Student s = a.getStudent();
            String acceptedIndicator = a.isStudentAccepted() ? "ACCEPTED" : "NOT ACCEPTED";
            System.out.printf("%d) %s | %s (%s, %s) | Status: %s | %s\n",
                    i + 1,
                    a.getApplicationId(),
                    s.getName(),
                    s.getUserId(),
                    s.getMajor(),
                    a.getStatus(),
                    acceptedIndicator);
        }
    }

    /**
     * Displays application details and allows representative to accept or reject (for pending applications).
     * <p>
     * This method:
     * <ol>
     *   <li>Displays comprehensive application and student details</li>
     *   <li>Shows application status and acceptance status</li>
     *   <li>Provides action menu for pending applications only</li>
     *   <li>Allows marking applications as SUCCESSFUL (offer) or UNSUCCESSFUL (reject)</li>
     * </ol>
     *
     * @param repEmail email of the representative
     * @param application application to review
     * 
     * @see CompanyRepController#markApplicationSuccessful(String, String)
     * @see CompanyRepController#markApplicationUnsuccessful(String, String)
     */
    private void handleApplicationActions(String repEmail, InternshipApplication application) {
        Student student = application.getStudent();
        
        displaySectionHeader("Application Details");
        System.out.println("Application ID: " + application.getApplicationId());
        System.out.println("Student: " + student.getName() + " (" + student.getUserId() + ")");
        System.out.println("Major: " + student.getMajor() + " | Year: " + student.getYearOfStudy());
        System.out.println("Email: " + student.getEmail());
        System.out.println("Applied On: " + application.getAppliedOn());
        System.out.println("Status: " + application.getStatus());
        System.out.println("Accepted: " + (application.isStudentAccepted() ? "Yes" : "No"));

        // Only allow actions on pending applications
        if (application.getStatus() != entity.ApplicationStatus.PENDING) {
            System.out.println("This application has already been processed.");
            return;
        }

        while (true) {
            displaySubSectionHeader("Action Menu");
            System.out.println("1. Mark as SUCCESSFUL (Offer)");
            System.out.println("2. Mark as UNSUCCESSFUL (Reject)");
            System.out.println("0. Back to Applications");
            System.out.print("Choice: ");
            String act = sc.nextLine().trim();
            try {
                switch (act) {
                    case "1" -> {
                        ctl.markApplicationSuccessful(repEmail, application.getApplicationId());
                        System.out.println("Application marked SUCCESSFUL. Offer sent to student.");
                        return;
                    }
                    case "2" -> {
                        ctl.markApplicationUnsuccessful(repEmail, application.getApplicationId());
                        System.out.println("Application marked UNSUCCESSFUL. Student notified.");
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

    /* ---------- Create Internship ---------- */
    
    /**
     * Prompts for and creates a new internship posting.
     * <p>
     * Guides the user through entering all required internship details:
     * <ul>
     *   <li>Title and description</li>
     *   <li>Internship level and preferred major</li>
     *   <li>Open and close dates (with validation)</li>
     *   <li>Maximum number of slots (1-10)</li>
     * </ul>
     * 
     * The created internship will have PENDING status until approved by Career Center staff.
     *
     * @param repEmail email of the representative creating the posting
     * 
     * @see CompanyRepController#createInternship(String, String, String, InternshipLevel, String, LocalDate, LocalDate, int)
     * @see #getInternshipLevel()
     * @see #getDateInput(String, boolean)
     * @see #getSlotInput(String)
     */
    private void handleCreateInternship(String repEmail) {
        displaySectionHeader("Create New Internship");
        
        String title = getRequiredInput("Title: ");
        String description = getRequiredInput("Description: ");
        
        InternshipLevel level = getInternshipLevel();
        String preferredMajor = getRequiredInput("Preferred Major (e.g., CSC, EEE, MAE): ");
        
        LocalDate openDate = getDateInput("Open date (YYYY-MM-DD): ", false);
        LocalDate closeDate = getDateInput("Close date (YYYY-MM-DD): ", true);
        
        // Validate date range
        if (closeDate.isBefore(openDate)) {
            System.out.println("Error: Close date cannot be before open date.");
            return;
        }
        
        int maxSlots = getSlotInput("Max slots (1-10): ");

        try {
            String id = ctl.createInternship(repEmail, title, description, level, preferredMajor, openDate, closeDate, maxSlots);
            System.out.println("Internship created successfully!");
            System.out.println("Internship ID: " + id);
            System.out.println("Status: PENDING (Waiting for Career Center approval)");
        } catch (Exception ex) {
            System.out.println("Error creating internship: " + ex.getMessage());
        }
    }

    /* ---------- Edit Internship ---------- */
    
    /**
     * Allows representative to edit internship details (title, description, level, major, dates, slots).
     * <p>
     * This method:
     * <ol>
     *   <li>Validates that the internship is editable (not approved or rejected)</li>
     *   <li>Prompts for new values with current values as defaults</li>
     *   <li>Validates all input including date ranges and slot limits</li>
     *   <li>Updates the internship through the controller</li>
     * </ol>
     * 
     * Users can leave fields blank to keep current values.
     *
     * @param repEmail email of the representative
     * @param internship internship to edit
     * 
     * @see CompanyRepController#editInternship(String, String, String, String, InternshipLevel, String, LocalDate, LocalDate, int)
     * @see #getOptionalInternshipLevel(InternshipLevel)
     * @see #getOptionalDateInput(String, LocalDate, boolean)
     * @see #getOptionalSlotInput(String, int)
     */
    private void handleEditInternship(String repEmail, Internship internship) {
        if (!internship.isEditable()) {
            System.out.println("Cannot edit internship that has been approved or rejected.");
            return;
        }
        
        System.out.println("\n=== Editing: " + internship.getTitle() + " ===");
        System.out.println("Leave field blank to keep current value.");
        
        String currentTitle = internship.getTitle();
        String currentDesc = internship.getDescription();
        InternshipLevel currentLevel = internship.getLevel();
        String currentMajor = internship.getPreferredMajor();
        LocalDate currentOpen = internship.getOpenDate();
        LocalDate currentClose = internship.getCloseDate();
        int currentSlots = internship.getMaxSlots();

        String title = getOptionalInput("Title [" + currentTitle + "]: ", currentTitle);
        String description = getOptionalInput("Description [" + currentDesc + "]: ", currentDesc);
        
        InternshipLevel level = getOptionalInternshipLevel(currentLevel);
        String preferredMajor = getOptionalInput("Preferred Major [" + currentMajor + "]: ", currentMajor);
        
        LocalDate openDate = getOptionalDateInput("Open date [" + currentOpen + "]: ", currentOpen, false);
        LocalDate closeDate = getOptionalDateInput("Close date [" + currentClose + "]: ", currentClose, true);
        
        int maxSlots = getOptionalSlotInput("Max slots [" + currentSlots + "]: ", currentSlots);

        ctl.editInternship(repEmail, internship.getInternshipId(), title, description, level, preferredMajor, openDate, closeDate, maxSlots);
        System.out.println("Internship updated successfully.");
    }

    /* ---------- Delete Internship ---------- */
    
    /**
     * Deletes an internship posting after confirmation.
     * <p>
     * This method:
     * <ol>
     *   <li>Validates that the internship can be deleted (not approved)</li>
     *   <li>Prompts for confirmation before deletion</li>
     *   <li>Deletes the internship through the controller</li>
     *   <li>Provides success/error feedback</li>
     * </ol>
     *
     * @param repEmail email of the representative
     * @param internship internship to delete
     * 
     * @see CompanyRepController#deleteInternship(String, String)
     */
    private void handleDeleteInternship(String repEmail, Internship internship) {
        if (!internship.canBeDeleted()) {
            System.out.println("Cannot delete internship that has been approved.");
            return;
        }
        
        boolean confirm = promptBoolean("Are you sure you want to delete '" + internship.getTitle() + "'? (1 = Yes, 0 = No): ");
        if (confirm) {
            try {
                ctl.deleteInternship(repEmail, internship.getInternshipId());
                System.out.println("Internship deleted successfully!");
            } catch (Exception ex) {
                System.out.println("Error deleting internship: " + ex.getMessage());
            }
        }
    }

    /* ---------- Input Helpers ---------- */
    
    /**
     * Prompts user to select an internship level (BASIC, INTERMEDIATE, ADVANCED).
     * <p>
     * Continuously prompts until a valid internship level is provided.
     * Input is case-insensitive.
     *
     * @return selected {@link InternshipLevel}
     */
    private InternshipLevel getInternshipLevel() {
        while (true) {
            System.out.print("Level (BASIC/INTERMEDIATE/ADVANCED): ");
            String input = sc.nextLine().trim().toUpperCase();
            try {
                return InternshipLevel.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid level. Please enter BASIC, INTERMEDIATE, or ADVANCED.");
            }
        }
    }

    /**
     * Prompts user to select an internship level with current value as default.
     * <p>
     * If the user enters nothing, returns the current level.
     * If the user enters an invalid level, returns the current level with a warning.
     *
     * @param currentLevel current internship level
     * @return selected {@link InternshipLevel} or current level if input is empty or invalid
     */
    private InternshipLevel getOptionalInternshipLevel(InternshipLevel currentLevel) {
        System.out.print("Level [" + currentLevel + "]: ");
        String input = sc.nextLine().trim().toUpperCase();
        if (input.isEmpty()) {
            return currentLevel;
        }
        try {
            return InternshipLevel.valueOf(input);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid level. Keeping current value.");
            return currentLevel;
        }
    }

    /**
     * Prompts user to enter a date in YYYY-MM-DD format with optional past date validation.
     * <p>
     * Continuously prompts until a valid date is provided.
     *
     * @param prompt prompt message to display
     * @param allowPast whether to allow dates in the past
     * @return parsed {@link LocalDate}
     */
    private LocalDate getDateInput(String prompt, boolean allowPast) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                LocalDate date = LocalDate.parse(input);
                if (!allowPast && date.isBefore(LocalDate.now())) {
                    System.out.println("Date cannot be in the past. Please enter a future date.");
                    continue;
                }
                return date;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }
    }

    /**
     * Prompts user to enter an optional date in YYYY-MM-DD format (empty input returns current date).
     * <p>
     * If the user enters nothing, returns the current date.
     * Validates date format and past date restrictions if input is provided.
     *
     * @param prompt prompt message to display
     * @param currentDate current date to use as default
     * @param allowPast whether to allow dates in the past
     * @return parsed {@link LocalDate} or current date if input is empty
     */
    private LocalDate getOptionalDateInput(String prompt, LocalDate currentDate, boolean allowPast) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.isEmpty()) {
                return currentDate;
            }
            try {
                LocalDate date = LocalDate.parse(input);
                if (!allowPast && date.isBefore(LocalDate.now())) {
                    System.out.println("Date cannot be in the past. Please enter a future date.");
                    continue;
                }
                return date;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD or leave blank.");
            }
        }
    }

    /**
     * Prompts user to enter number of internship slots (1-10).
     * <p>
     * Continuously prompts until a valid number between 1 and 10 is provided.
     *
     * @param prompt prompt message to display
     * @return number of slots (1-10)
     */
    private int getSlotInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                int slots = Integer.parseInt(input);
                if (slots >= 1 && slots <= 10) {
                    return slots;
                }
                System.out.println("Slots must be between 1 and 10.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter a number between 1 and 10.");
            }
        }
    }

    /**
     * Prompts user to enter optional number of internship slots with current value as default.
     * <p>
     * If the user enters nothing, returns the current slot count.
     * If the user enters an invalid number, returns the current slot count with a warning.
     *
     * @param prompt prompt message to display
     * @param currentSlots current number of slots
     * @return number of slots (1-10) or current slots if input is empty or invalid
     */
    private int getOptionalSlotInput(String prompt, int currentSlots) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.isEmpty()) {
                return currentSlots;
            }
            try {
                int slots = Integer.parseInt(input);
                if (slots >= 1 && slots <= 10) {
                    return slots;
                }
                System.out.println("Slots must be between 1 and 10. Keeping current value.");
                return currentSlots;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Keeping current value.");
                return currentSlots;
            }
        }
    }

    /* ---------- Display Helpers ---------- */
    
    /**
     * Displays complete internship details including status, visibility, and slots information.
     * <p>
     * Shows all stored attributes of the internship for comprehensive review:
     * <ul>
     *   <li>Basic information (ID, title, description)</li>
     *   <li>Requirements (level, preferred major)</li>
     *   <li>Timing (open/close dates)</li>
     *   <li>Status and visibility</li>
     *   <li>Slot usage and editability</li>
     * </ul>
     *
     * @param internship internship to display
     */
    private void displayInternshipDetails(Internship internship) {
        displaySectionHeader("Internship Details");
        System.out.println("ID: " + internship.getInternshipId());
        System.out.println("Title: " + internship.getTitle());
        System.out.println("Description: " + internship.getDescription());
        System.out.println("Level: " + internship.getLevel());
        System.out.println("Preferred Major: " + internship.getPreferredMajor());
        System.out.println("Open Date: " + internship.getOpenDate());
        System.out.println("Close Date: " + internship.getCloseDate());
        System.out.println("Status: " + internship.getStatus());
        System.out.println("Visible: " + (internship.isVisible() ? "Yes" : "No"));
        System.out.println("Slots: " + internship.getConfirmedSlots() + "/" + internship.getMaxSlots());
        System.out.println("Editable: " + (internship.isEditable() ? "Yes" : "No"));
    }
}