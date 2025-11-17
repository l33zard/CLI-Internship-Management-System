package boundary;

import controller.CompanyRepController;
import controller.AuthController;
import entity.Internship;
import entity.InternshipApplication;
import entity.InternshipLevel;
import entity.InternshipStatus;
import entity.Student;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class CompanyRepBoundary extends BaseBoundary {
    private final CompanyRepController ctl;

    public CompanyRepBoundary(CompanyRepController ctl, AuthController auth) {
        super(auth);
        this.ctl = ctl;
    }

    public void menu(String repEmail) {
        while (true) {
            displaySectionHeader("Company Representative Dashboard");
            System.out.println("1. View My Internships");
            System.out.println("2. Create New Internship");
            System.out.println("3. Edit Internship");
            System.out.println("4. Delete Internship");
            System.out.println("9. Change my password");
            System.out.println("0. Logout");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> handleInternships(repEmail);
                    case "2" -> handleCreateInternship(repEmail);
                    case "3" -> handleEditInternship(repEmail);
                    case "4" -> handleDeleteInternship(repEmail);
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

    private void handleInternshipActions(String repEmail, Internship internship) {
        while (true) {
            displayInternshipDetails(internship);
            displaySubSectionHeader("Action Menu");
            System.out.println("1. Set Visibility");
            System.out.println("2. View Applications");
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
                        boolean vis = confirmAction("Make visible to students?");
                        ctl.setVisibility(repEmail, internship.getInternshipId(), vis);
                        System.out.println(vis ? "Set to visible." : "Set to hidden.");
                        // Refresh the internship object
                        internship = ctl.getInternship(repEmail, internship.getInternshipId());
                    }
                    case "2" -> {
                        handleApplications(repEmail, internship);
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

    /* ---------- View Applications for One Internship ---------- */
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
    private void handleEditInternship(String repEmail) {
        List<Internship> editableInternships = ctl.listInternships(repEmail).stream()
                .filter(Internship::isEditable)
                .toList();
                
        if (editableInternships.isEmpty()) {
            System.out.println("No editable internships found. Only PENDING internships can be edited.");
            return;
        }

        displaySectionHeader("Edit Internship");
        for (int i = 0; i < editableInternships.size(); i++) {
            Internship it = editableInternships.get(i);
            System.out.printf("%d) %s [%s] | Created: %s\n",
                    i + 1, it.getTitle(), it.getLevel(), it.getOpenDate());
        }

        System.out.print("Select internship to edit (0 to back): ");
        int sel = safeInt(sc.nextLine(), 0);
        if (sel <= 0 || sel > editableInternships.size()) return;

        Internship chosen = editableInternships.get(sel - 1);
        editInternshipDetails(repEmail, chosen);
    }

    private void editInternshipDetails(String repEmail, Internship internship) {
        displaySectionHeader("Editing: " + internship.getTitle());
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

        try {
            ctl.editInternship(repEmail, internship.getInternshipId(), title, description, level, 
                              preferredMajor, openDate, closeDate, maxSlots);
            System.out.println("Internship updated successfully!");
        } catch (Exception ex) {
            System.out.println("Error updating internship: " + ex.getMessage());
        }
    }

    /* ---------- Delete Internship ---------- */
    private void handleDeleteInternship(String repEmail) {
        List<Internship> deletableInternships = ctl.listInternships(repEmail).stream()
                .filter(Internship::canBeDeleted)
                .toList();
                
        if (deletableInternships.isEmpty()) {
            System.out.println("No deletable internships found. Only PENDING or REJECTED internships can be deleted.");
            return;
        }

        displaySectionHeader("Delete Internship");
        for (int i = 0; i < deletableInternships.size(); i++) {
            Internship it = deletableInternships.get(i);
            System.out.printf("%d) %s [%s] | Status: %s\n",
                    i + 1, it.getTitle(), it.getLevel(), it.getStatus());
        }

        System.out.print("Select internship to delete (0 to back): ");
        int sel = safeInt(sc.nextLine(), 0);
        if (sel <= 0 || sel > deletableInternships.size()) return;

        Internship chosen = deletableInternships.get(sel - 1);
        
        boolean confirm = confirmAction("Are you sure you want to delete '" + chosen.getTitle() + "'?");
        if (confirm) {
            try {
                ctl.deleteInternship(repEmail, chosen.getInternshipId());
                System.out.println("Internship deleted successfully!");
            } catch (Exception ex) {
                System.out.println("Error deleting internship: " + ex.getMessage());
            }
        }
    }

    /* ---------- Input Helpers ---------- */
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