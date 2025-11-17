package controller;

import database.ApplicationRepository;
import database.CompanyRepRepository;
import database.InternshipRepository;
import entity.CompanyRep;
import entity.Internship;
import entity.InternshipApplication;
import entity.InternshipLevel;
import entity.InternshipStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for company representative actions (manage internships and
 * applications for the rep's company).
 * <p>
 * This controller provides methods for company representatives to:
 * <ul>
 *   <li>Create, view, edit, and delete internship postings</li>
 *   <li>Manage application visibility</li>
 *   <li>Review and process internship applications</li>
 *   <li>Track application status (successful/unsuccessful)</li>
 * </ul>
 * 
 * The controller enforces business rules including:
 * <ul>
 *   <li>Maximum number of active postings per representative</li>
 *   <li>Ownership validation for all operations</li>
 *   <li>Status-based restrictions on editing and deletion</li>
 *   <li>Parameter validation for internship creation and updates</li>
 * </ul>
 * 
 */
public class CompanyRepController extends BaseController {
    /** Maximum number of active internship postings allowed per company representative */
    private static final int MAX_POSTINGS_PER_REP = 5;
    
    /** Maximum number of slots allowed per internship posting */
    private static final int MAX_SLOTS = 10;

    /**
     * Constructs a CompanyRepController wired to necessary repositories.
     *
     * @param reps the company representative repository
     * @param internships the internship repository
     * @param applications the internship application repository
     */
    public CompanyRepController(CompanyRepRepository reps,
                                InternshipRepository internships,
                                ApplicationRepository applications) {
        super(null, reps, null, internships, applications, null);
    }

    /**
     * Lists all internships owned by the company representative's company.
     *
     * @param repEmail the email address of the company representative
     * @return a list of internships belonging to the representative's company,
     *         or an empty list if no internships exist
     * @throws IllegalArgumentException if the company representative is not found
     */
    public List<Internship> listInternships(String repEmail) {
        CompanyRep rep = validateRepExists(repEmail);
        return internshipRepo.findByCompanyName(rep.getCompanyName());
    }
    
    /**
     * Retrieves details for a specific internship owned by the company representative.
     *
     * @param repEmail the email address of the company representative
     * @param internshipId the unique identifier of the internship to retrieve
     * @return the internship with the specified ID
     * @throws IllegalArgumentException if the company representative or internship is not found
     * @throws SecurityException if the internship does not belong to the representative's company
     */
    public Internship getInternship(String repEmail, String internshipId) {
        CompanyRep rep = validateRepExists(repEmail);
        Internship internship = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));
        
        validateOwnership(rep, internship);
        return internship;
    }

    /**
     * Creates a new internship posting for the company representative's company.
     *
     * @param repEmail the email address of the company representative
     * @param title the title of the internship
     * @param description the description of the internship
     * @param level the experience level required for the internship
     * @param preferredMajor the preferred major for applicants
     * @param openDate the date when applications open
     * @param closeDate the date when applications close
     * @param maxSlots the maximum number of available slots
     * @return the unique identifier of the newly created internship
     * @throws IllegalArgumentException if the company representative is not found,
     *         or if any parameter validation fails
     * @throws IllegalStateException if the representative has reached the maximum
     *         number of active postings
     */
    public String createInternship(String repEmail,
                                   String title, String description, InternshipLevel level,
                                   String preferredMajor, LocalDate openDate, LocalDate closeDate,
                                   int maxSlots) {
        CompanyRep rep = validateRepExists(repEmail);
        
        validateInternshipParams(title, description, level, preferredMajor, openDate, closeDate, maxSlots);

        long activeCount = internshipRepo.findByCompanyName(rep.getCompanyName()).stream()
                .filter(i -> i.getStatus() == InternshipStatus.PENDING
                          || i.getStatus() == InternshipStatus.APPROVED)
                .count();
        if (activeCount >= MAX_POSTINGS_PER_REP) {
            throw new IllegalStateException("You have reached the limit of "
                    + MAX_POSTINGS_PER_REP + " active internship postings.");
        }

        Internship i = new Internship(
                title, description, level, preferredMajor,
                openDate, closeDate, rep.getCompanyName(), maxSlots
        );
        internshipRepo.save(i);
        return i.getInternshipId();
    }
    
    /**
     * Edits an existing internship posting.
     * <p>
     * Only allowed when the internship is in an editable state (not approved or rejected).
     *
     * @param repEmail the email address of the company representative
     * @param internshipId the unique identifier of the internship to edit
     * @param title the new title of the internship
     * @param description the new description of the internship
     * @param level the new experience level required for the internship
     * @param preferredMajor the new preferred major for applicants
     * @param openDate the new date when applications open
     * @param closeDate the new date when applications close
     * @param maxSlots the new maximum number of available slots
     * @throws IllegalArgumentException if the company representative or internship is not found,
     *         or if any parameter validation fails
     * @throws SecurityException if the internship does not belong to the representative's company
     * @throws IllegalStateException if the internship cannot be edited due to its status
     */
    public void editInternship(String repEmail, String internshipId,
                              String title, String description, InternshipLevel level,
                              String preferredMajor, LocalDate openDate, LocalDate closeDate,
                              int maxSlots) {
        CompanyRep rep = validateRepExists(repEmail);
        Internship internship = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));
        
        validateOwnership(rep, internship);
        
        if (!internship.isEditable()) {
            throw new IllegalStateException("Cannot edit internship that has been approved or rejected.");
        }
        
        validateInternshipParams(title, description, level, preferredMajor, openDate, closeDate, maxSlots);
        
        internshipRepo.deleteById(internshipId);
        
        Internship updatedInternship = new Internship(
                title, description, level, preferredMajor,
                openDate, closeDate, rep.getCompanyName(), maxSlots
        );
        internshipRepo.save(updatedInternship);
    }
    
    /**
     * Deletes an internship posting if allowed by its status.
     *
     * @param repEmail the email address of the company representative
     * @param internshipId the unique identifier of the internship to delete
     * @throws IllegalArgumentException if the company representative or internship is not found
     * @throws SecurityException if the internship does not belong to the representative's company
     * @throws IllegalStateException if the internship cannot be deleted due to its status
     */
    public void deleteInternship(String repEmail, String internshipId) {
        CompanyRep rep = validateRepExists(repEmail);
        Internship internship = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));
        
        validateOwnership(rep, internship);
        
        if (!internship.canBeDeleted()) {
            throw new IllegalStateException("Cannot delete internship that has been approved.");
        }
        
        internshipRepo.deleteById(internshipId);
    }

    /**
     * Sets the visibility of an internship posting.
     *
     * @param repEmail the email address of the company representative
     * @param internshipId the unique identifier of the internship
     * @param visible true to make the internship visible, false to hide it
     * @throws IllegalArgumentException if the company representative or internship is not found
     * @throws SecurityException if the internship does not belong to the representative's company
     */
    public void setVisibility(String repEmail, String internshipId, boolean visible) {
        CompanyRep rep = validateRepExists(repEmail);
        Internship i = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));

        validateOwnership(rep, i);

        i.setVisible(visible);
        internshipRepo.save(i);
    }

    /**
     * Lists all applications for a specific internship owned by the company representative.
     *
     * @param repEmail the email address of the company representative
     * @param internshipId the unique identifier of the internship
     * @return a list of applications for the specified internship,
     *         or an empty list if no applications exist
     * @throws IllegalArgumentException if the company representative or internship is not found
     * @throws SecurityException if the internship does not belong to the representative's company
     */
    public List<InternshipApplication> listApplications(String repEmail, String internshipId) {
        CompanyRep rep = validateRepExists(repEmail);
        Internship i = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));

        validateOwnership(rep, i);
        return applicationRepo.findByInternship(internshipId);
    }

    /**
     * Marks an application as SUCCESSFUL (offer extended to applicant).
     *
     * @param repEmail the email address of the company representative
     * @param applicationId the unique identifier of the application
     * @throws IllegalArgumentException if the company representative or application is not found
     * @throws SecurityException if the application does not belong to the representative's company
     */
    public void markApplicationSuccessful(String repEmail, String applicationId) {
        CompanyRep rep = validateRepExists(repEmail);
        InternshipApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        validateOwnership(rep, app.getInternship());

        app.markSuccessful();
        applicationRepo.save(app);
    }

    /**
     * Marks an application as UNSUCCESSFUL (rejected).
     *
     * @param repEmail the email address of the company representative
     * @param applicationId the unique identifier of the application
     * @throws IllegalArgumentException if the company representative or application is not found
     * @throws SecurityException if the application does not belong to the representative's company
     */
    public void markApplicationUnsuccessful(String repEmail, String applicationId) {
        CompanyRep rep = validateRepExists(repEmail);
        InternshipApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        validateOwnership(rep, app.getInternship());

        app.markUnsuccessful();
        applicationRepo.save(app);
    }
    
    /**
     * Validates that a company representative exists in the system.
     *
     * @param repEmail the email address to validate
     * @return the CompanyRep entity if found
     * @throws IllegalArgumentException if no company representative is found with the given email
     */
    private CompanyRep validateRepExists(String repEmail) {
        return repRepo.findByEmail(repEmail)
                .orElseThrow(() -> new IllegalArgumentException("Company representative not found: " + repEmail));
    }
    
    /**
     * Validates that a company representative owns the specified internship.
     *
     * @param rep the company representative to validate
     * @param internship the internship to check ownership of
     * @throws SecurityException if the internship does not belong to the representative's company
     */
    private void validateOwnership(CompanyRep rep, Internship internship) {
        if (!rep.getCompanyName().equalsIgnoreCase(internship.getCompanyName())) {
            throw new SecurityException("Cannot manage another company's posting");
        }
    }
    
    /**
     * Validates internship parameters for creation and editing operations.
     *
     * @param title the internship title to validate
     * @param description the internship description to validate
     * @param level the internship level to validate
     * @param preferredMajor the preferred major to validate
     * @param openDate the open date to validate
     * @param closeDate the close date to validate
     * @param maxSlots the maximum slots to validate
     * @throws IllegalArgumentException if any parameter fails validation
     */
    private void validateInternshipParams(String title, String description, InternshipLevel level, String preferredMajor, LocalDate openDate, LocalDate closeDate,int maxSlots) {
		if (title == null || title.trim().isEmpty()) {
			throw new IllegalArgumentException("Title is required");
		}
		if (description == null || description.trim().isEmpty()) {
			throw new IllegalArgumentException("Description is required");
		}
		if (level == null) {
			throw new IllegalArgumentException("Internship level is required");
		}
		if (preferredMajor == null || preferredMajor.trim().isEmpty()) {
			throw new IllegalArgumentException("Preferred major is required");
		}
		if (openDate == null) {
			throw new IllegalArgumentException("Open date is required");
		}
		if (closeDate == null) {
			throw new IllegalArgumentException("Close date is required");
		}
		if (maxSlots < 1 || maxSlots > MAX_SLOTS) {
			throw new IllegalArgumentException("Max slots must be between 1 and " + MAX_SLOTS);
		}
		if (closeDate.isBefore(openDate)) {
			throw new IllegalArgumentException("Close date cannot be before open date");
		}
	}
}