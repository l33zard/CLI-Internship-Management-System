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
 * Controller for managing company representative operations related to internship postings and applications.
 * 
 * <p>This controller handles the business logic for company representatives to:
 * <ul>
 *   <li>Create, edit, and delete internship postings</li>
 *   <li>Manage posting visibility</li>
 *   <li>Review and process student applications</li>
 *   <li>Monitor their active postings</li>
 * </ul>
 * 
 * <p><strong>Business Rules:</strong>
 * <ul>
 *   <li>Each company representative can have a maximum of {@value #MAX_POSTINGS_PER_REP} active postings</li>
 *   <li>Each internship posting can have a maximum of {@value #MAX_SLOTS} slots</li>
 *   <li>Company representatives can only manage their own company's postings</li>
 *   <li>Approved or rejected postings cannot be edited</li>
 *   <li>Only approved postings cannot be deleted</li>
 * </ul>
 * 
 * @see CompanyRep
 * @see Internship
 * @see InternshipApplication
 * @see BaseController
 */
public class CompanyRepController extends BaseController {
    
    /**
     * Maximum number of active internship postings allowed per company representative.
     */
    private static final int MAX_POSTINGS_PER_REP = 5;
    
    /**
     * Maximum number of slots allowed per internship posting.
     */
    private static final int MAX_SLOTS = 10;

    /**
     * Constructs a new CompanyRepController with the specified repositories.
     * 
     * @param reps the repository for managing company representatives
     * @param internships the repository for managing internship postings
     * @param applications the repository for managing internship applications
     */
    public CompanyRepController(CompanyRepRepository reps,
                                InternshipRepository internships,
                                ApplicationRepository applications) {
        super(null, reps, null, internships, applications, null);
    }

    /**
     * Retrieves all internship postings for a specific company representative.
     * 
     * <p>Returns all internships associated with the representative's company,
     * regardless of their status (pending, approved, rejected, or filled).
     * 
     * @param repEmail the email address of the company representative
     * @return a list of all internships posted by the representative's company
     * @throws IllegalArgumentException if the company representative is not found
     */
    public List<Internship> listInternships(String repEmail) {
        CompanyRep rep = validateRepExists(repEmail);
        return internshipRepo.findByCompanyName(rep.getCompanyName());
    }
    
    /**
     * Retrieves a specific internship posting owned by the company representative.
     * 
     * <p>Validates that the internship exists and that the representative's company
     * owns the posting before returning it.
     * 
     * @param repEmail the email address of the company representative
     * @param internshipId the unique identifier of the internship
     * @return the internship with the specified ID
     * @throws IllegalArgumentException if the internship is not found
     * @throws SecurityException if the internship belongs to a different company
     */
    public Internship getInternship(String repEmail, String internshipId) {
        CompanyRep rep = validateRepExists(repEmail);
        Internship internship = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));
        
        validateOwnership(rep, internship);
        return internship;
    }

    /**
     * Creates a new internship posting for the company representative.
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Validates the company representative exists</li>
     *   <li>Validates all internship parameters</li>
     *   <li>Checks if the representative has reached the posting limit ({@value #MAX_POSTINGS_PER_REP})</li>
     *   <li>Creates and saves the new internship posting with status PENDING</li>
     * </ol>
     * 
     * <p><strong>Validation Rules:</strong>
     * <ul>
     *   <li>Title must not be empty</li>
     *   <li>Description must not be empty</li>
     *   <li>Internship level is required</li>
     *   <li>Preferred major must not be empty</li>
     *   <li>Open and close dates are required</li>
     *   <li>Close date must not be before open date</li>
     *   <li>Max slots must be between 1 and {@value #MAX_SLOTS}</li>
     * </ul>
     * 
     * @param repEmail the email address of the company representative
     * @param title the title of the internship position
     * @param description detailed description of the internship
     * @param level the level of the internship (FRESHMAN, SOPHOMORE, JUNIOR, SENIOR)
     * @param preferredMajor the preferred academic major for applicants
     * @param openDate the date when applications open
     * @param closeDate the date when applications close
     * @param maxSlots the maximum number of available positions
     * @return the unique identifier of the newly created internship
     * @throws IllegalArgumentException if any parameter is invalid
     * @throws IllegalStateException if the posting limit has been reached
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
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Validates the company representative exists</li>
     *   <li>Validates the internship exists and is owned by the representative's company</li>
     *   <li>Checks if the internship can be edited (must be PENDING or REJECTED)</li>
     *   <li>Validates all new parameters</li>
     *   <li>Deletes the old internship and creates a new one with updated details</li>
     * </ol>
     * 
     * <p><strong>Important:</strong> Editing replaces the internship entirely, generating a new ID.
     * This is done to maintain data integrity and simplify the update process.
     * 
     * @param repEmail the email address of the company representative
     * @param internshipId the unique identifier of the internship to edit
     * @param title the new title of the internship position
     * @param description the new detailed description
     * @param level the new internship level
     * @param preferredMajor the new preferred academic major
     * @param openDate the new application open date
     * @param closeDate the new application close date
     * @param maxSlots the new maximum number of positions
     * @throws IllegalArgumentException if the internship is not found or parameters are invalid
     * @throws SecurityException if the internship belongs to a different company
     * @throws IllegalStateException if the internship has been approved or rejected
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
     * Deletes an internship posting.
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Validates the company representative exists</li>
     *   <li>Validates the internship exists and is owned by the representative's company</li>
     *   <li>Checks if the internship can be deleted (must not be APPROVED)</li>
     *   <li>Removes the internship from the repository</li>
     * </ol>
     * 
     * <p><strong>Deletion Rules:</strong>
     * <ul>
     *   <li>PENDING internships can be deleted</li>
     *   <li>REJECTED internships can be deleted</li>
     *   <li>APPROVED internships cannot be deleted (to preserve application history)</li>
     * </ul>
     * 
     * @param repEmail the email address of the company representative
     * @param internshipId the unique identifier of the internship to delete
     * @throws IllegalArgumentException if the internship is not found
     * @throws SecurityException if the internship belongs to a different company
     * @throws IllegalStateException if the internship has been approved
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
     * <p>Controls whether students can see and apply to the internship posting.
     * Typically used to temporarily hide postings without deleting them, or to
     * make approved postings visible to students.
     * 
     * @param repEmail the email address of the company representative
     * @param internshipId the unique identifier of the internship
     * @param visible true to make the internship visible to students, false to hide it
     * @throws IllegalArgumentException if the internship is not found
     * @throws SecurityException if the internship belongs to a different company
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
     * Retrieves all applications for a specific internship posting.
     * 
     * <p>Returns all applications submitted to the specified internship,
     * allowing the company representative to review and process them.
     * 
     * @param repEmail the email address of the company representative
     * @param internshipId the unique identifier of the internship
     * @return a list of all applications for the internship
     * @throws IllegalArgumentException if the internship is not found
     * @throws SecurityException if the internship belongs to a different company
     */
    public List<InternshipApplication> listApplications(String repEmail, String internshipId) {
        CompanyRep rep = validateRepExists(repEmail);
        Internship i = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));

        validateOwnership(rep, i);
        return applicationRepo.findByInternship(internshipId);
    }

    /**
     * Marks an application as successful (accepted).
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Validates the company representative and application exist</li>
     *   <li>Verifies the representative owns the internship</li>
     *   <li>Updates the application status to SUCCESSFUL</li>
     *   <li>Saves the updated application</li>
     * </ol>
     * 
     * <p>This indicates the student has been selected for the internship position.
     * 
     * @param repEmail the email address of the company representative
     * @param applicationId the unique identifier of the application
     * @throws IllegalArgumentException if the application is not found
     * @throws SecurityException if the internship belongs to a different company
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
     * Marks an application as unsuccessful (rejected).
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Validates the company representative and application exist</li>
     *   <li>Verifies the representative owns the internship</li>
     *   <li>Updates the application status to UNSUCCESSFUL</li>
     *   <li>Saves the updated application</li>
     * </ol>
     * 
     * <p>This indicates the student has not been selected for the internship position.
     * 
     * @param repEmail the email address of the company representative
     * @param applicationId the unique identifier of the application
     * @throws IllegalArgumentException if the application is not found
     * @throws SecurityException if the internship belongs to a different company
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
     * Validates that a company representative exists in the repository.
     * 
     * @param repEmail the email address to validate
     * @return the company representative if found
     * @throws IllegalArgumentException if the representative is not found
     */
    private CompanyRep validateRepExists(String repEmail) {
        return repRepo.findByEmail(repEmail)
                .orElseThrow(() -> new IllegalArgumentException("Company representative not found: " + repEmail));
    }
    
    /**
     * Validates that a company representative owns a specific internship posting.
     * 
     * <p>Ensures that representatives can only manage internships posted by their own company,
     * preventing unauthorized access to other companies' postings.
     * 
     * @param rep the company representative
     * @param internship the internship to validate ownership for
     * @throws SecurityException if the internship does not belong to the representative's company
     */
    private void validateOwnership(CompanyRep rep, Internship internship) {
        if (!rep.getCompanyName().equalsIgnoreCase(internship.getCompanyName())) {
            throw new SecurityException("Cannot manage another company's posting");
        }
    }
    
    /**
     * Validates all parameters required to create or edit an internship posting.
     * 
     * <p><strong>Validation Rules:</strong>
     * <ul>
     *   <li>Title: Required, cannot be empty or whitespace only</li>
     *   <li>Description: Required, cannot be empty or whitespace only</li>
     *   <li>Level: Required, must be a valid InternshipLevel enum value</li>
     *   <li>Preferred Major: Required, cannot be empty or whitespace only</li>
     *   <li>Open Date: Required, cannot be null</li>
     *   <li>Close Date: Required, cannot be null, must be on or after open date</li>
     *   <li>Max Slots: Must be between 1 and {@value #MAX_SLOTS}</li>
     * </ul>
     * 
     * @param title the internship title to validate
     * @param description the internship description to validate
     * @param level the internship level to validate
     * @param preferredMajor the preferred major to validate
     * @param openDate the application open date to validate
     * @param closeDate the application close date to validate
     * @param maxSlots the maximum number of slots to validate
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