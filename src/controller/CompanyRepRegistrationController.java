package controller;

import database.CompanyRepRepository;
import entity.CompanyRep;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Controller handling registration and management of company representative accounts.
 * <p>
 * This controller provides functionality for:
 * <ul>
 *   <li>Registering new company representative accounts (pending approval)</li>
 *   <li>Checking email availability</li>
 *   <li>Retrieving pending representatives for administrative review</li>
 *   <li>Validating registration data and email formats</li>
 * </ul>
 * 
 * New company representatives are registered with a pending status and must be
 * approved by an administrator before they can access the system.
 * 
 */
public class CompanyRepRegistrationController extends BaseController {
    /** Regular expression pattern for validating email format */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    /**
     * Constructs a CompanyRepRegistrationController with the company representative repository.
     *
     * @param reps the company representative repository for data access operations
     */
    public CompanyRepRegistrationController(CompanyRepRepository reps) {
        super(null, reps, null, null, null, null);
    }

    /**
     * Registers a new company representative account with pending approval status.
     * <p>
     * Validates all registration fields and email format before creating the account.
     * The email address is normalized to lowercase and serves as the unique identifier.
     * 
     * @param name the full name of the company representative
     * @param companyName the name of the company the representative works for
     * @param department the department within the company
     * @param position the job position/title of the representative
     * @param email the email address (will be used as login identifier)
     * @return the registered representative's email address (identifier)
     * @throws IllegalArgumentException if any required field is null, empty, 
     *         or if the email format is invalid
     * @throws IllegalStateException if the email address is already registered
     */
    public String registerCompanyRep(String name, String companyName, String department,
                                     String position, String email) {
        validateRegistrationFields(name, companyName, department, position, email);
        
        if (repRepo.existsById(email)) {
            throw new IllegalStateException("Email already registered. Please use a different email address.");
        }

        CompanyRep rep = new CompanyRep(
            email.trim().toLowerCase(), 
            name.trim(), 
            companyName.trim(), 
            department.trim(), 
            position.trim(), 
            email.trim().toLowerCase()
        );
        repRepo.save(rep);
        return rep.getEmail();
    }
    
    /**
     * Checks if an email address is already registered in the system.
     *
     * @param email the email address to check for existence
     * @return true if the email is already registered, false otherwise
     */
    public boolean emailExists(String email) {
        return repRepo.findByEmail(email).isPresent();
    }
    
    /**
     * Retrieves all company representatives with pending approval status.
     * <p>
     * Pending representatives are those who have registered but have not yet been
     * approved or rejected by an administrator.
     *
     * @return a list of company representatives with pending status,
     *         or an empty list if no pending representatives exist
     */
    public List<CompanyRep> getPendingReps() {
        return repRepo.findAll().stream()
                .filter(rep -> !rep.isApproved() && !rep.isRejected())
                .collect(Collectors.toList());
    }
    
    /**
     * Retrieves a company representative by their email address.
     *
     * @param email the email address of the representative to retrieve
     * @return the CompanyRep entity with the specified email
     * @throws IllegalArgumentException if no company representative is found 
     *         with the given email address
     */
    public CompanyRep getRepByEmail(String email) {
        return repRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Company representative not found: " + email));
    }
    
    /**
     * Validates all registration fields for completeness and basic formatting.
     *
     * @param name the name to validate
     * @param companyName the company name to validate
     * @param department the department to validate
     * @param position the position to validate
     * @param email the email to validate
     * @throws IllegalArgumentException if any field is null, empty, or contains
     *         only whitespace, or if the email format is invalid
     */
    private void validateRegistrationFields(String name, String companyName, String department,
                                          String position, String email) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Company name is required");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department is required");
        }
        if (position == null || position.trim().isEmpty()) {
            throw new IllegalArgumentException("Position is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
    
    /**
     * Validates an email address against the standard email format pattern.
     *
     * @param email the email address to validate
     * @return true if the email matches the valid format pattern, false otherwise
     */
    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}