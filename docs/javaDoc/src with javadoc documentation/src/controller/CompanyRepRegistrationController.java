package controller;

import database.CompanyRepRepository;
import entity.CompanyRep;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Controller for managing company representative registration and account verification.
 * 
 * <p>This controller handles the business logic for new company representatives to:
 * <ul>
 *   <li>Register for an account in the internship system</li>
 *   <li>Verify email uniqueness before registration</li>
 *   <li>Validate registration information</li>
 *   <li>Track pending registration approvals</li>
 * </ul>
 * 
 * <p><strong>Registration Process:</strong>
 * <ol>
 *   <li>Company representative submits registration with company details</li>
 *   <li>System validates all required fields and email format</li>
 *   <li>System checks for duplicate email addresses</li>
 *   <li>Account is created with PENDING status</li>
 *   <li>Career center staff reviews and approves/rejects the registration</li>
 *   <li>Upon approval, representative can log in and post internships</li>
 * </ol>
 * 
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>Email format validation using regex pattern</li>
 *   <li>Duplicate email prevention</li>
 *   <li>All emails normalized to lowercase</li>
 *   <li>Required field validation</li>
 * </ul>
 * 
 * @see CompanyRep
 * @see BaseController
 */
public class CompanyRepRegistrationController extends BaseController {
    
    /**
     * Regular expression pattern for validating email addresses.
     * 
     * <p>Validates basic email format: localpart@domain
     * <ul>
     *   <li>Local part: Alphanumeric characters, plus (+), underscore (_), period (.), and hyphen (-)</li>
     *   <li>At symbol (@) required</li>
     *   <li>Domain: Any valid domain name</li>
     * </ul>
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    /**
     * Constructs a new CompanyRepRegistrationController with the specified repository.
     * 
     * @param reps the repository for managing company representatives
     */
    public CompanyRepRegistrationController(CompanyRepRepository reps) {
        super(null, reps, null, null, null, null);
    }

    /**
     * Registers a new company representative in the system.
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Validates all registration fields are complete and properly formatted</li>
     *   <li>Checks that the email address is not already registered</li>
     *   <li>Creates a new CompanyRep entity with PENDING approval status</li>
     *   <li>Normalizes email to lowercase</li>
     *   <li>Trims whitespace from all fields</li>
     *   <li>Saves the representative to the repository</li>
     * </ol>
     * 
     * <p><strong>Validation Rules:</strong>
     * <ul>
     *   <li><strong>Name:</strong> Required, cannot be empty or whitespace only</li>
     *   <li><strong>Company Name:</strong> Required, cannot be empty or whitespace only</li>
     *   <li><strong>Department:</strong> Required, cannot be empty or whitespace only</li>
     *   <li><strong>Position:</strong> Required, cannot be empty or whitespace only</li>
     *   <li><strong>Email:</strong> Required, must match valid email format, must be unique</li>
     * </ul>
     * 
     * <p><strong>Post-Registration:</strong>
     * The newly registered representative will have PENDING status and cannot access
     * the system until approved by career center staff.
     * 
     * @param name the full name of the company representative
     * @param companyName the name of the company the representative works for
     * @param department the department within the company
     * @param position the job title or position of the representative
     * @param email the email address for the account (will be used as login ID)
     * @return the normalized email address of the newly registered representative
     * @throws IllegalArgumentException if any required field is missing or invalid
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
     * <p>Used to provide real-time feedback during registration to prevent
     * duplicate email errors. Can be called before attempting registration
     * to inform users if their email is available.
     * 
     * @param email the email address to check
     * @return true if the email is already registered, false otherwise
     */
    public boolean emailExists(String email) {
        return repRepo.findByEmail(email).isPresent();
    }
    
    /**
     * Retrieves all company representatives with pending approval status.
     * 
     * <p>Returns representatives who have completed registration but are
     * awaiting career center staff review. These accounts cannot post
     * internships or log in until approved.
     * 
     * <p><strong>Filtering Logic:</strong>
     * <ul>
     *   <li>Excludes approved representatives</li>
     *   <li>Excludes rejected representatives</li>
     *   <li>Includes only those in pending state</li>
     * </ul>
     * 
     * @return a list of company representatives awaiting approval
     */
    public List<CompanyRep> getPendingReps() {
        return repRepo.findAll().stream()
                .filter(rep -> !rep.isApproved() && !rep.isRejected())
                .collect(Collectors.toList());
    }
    
    /**
     * Retrieves a company representative by their email address.
     * 
     * <p>Used to look up representative details, typically after registration
     * or when verifying account status.
     * 
     * @param email the email address of the representative
     * @return the company representative with the specified email
     * @throws IllegalArgumentException if no representative is found with that email
     */
    public CompanyRep getRepByEmail(String email) {
        return repRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Company representative not found: " + email));
    }
    
    /**
     * Validates all required fields for company representative registration.
     * 
     * <p>Ensures that all registration data is complete and properly formatted
     * before creating a new account.
     * 
     * <p><strong>Validation Performed:</strong>
     * <ul>
     *   <li>Null checking for all parameters</li>
     *   <li>Empty string checking (after trimming whitespace)</li>
     *   <li>Email format validation using regex pattern</li>
     * </ul>
     * 
     * @param name the representative's name to validate
     * @param companyName the company name to validate
     * @param department the department to validate
     * @param position the position to validate
     * @param email the email address to validate
     * @throws IllegalArgumentException if any field is null, empty, or improperly formatted
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
     * Validates email format using the EMAIL_PATTERN regex.
     * 
     * <p>Checks that the email contains:
     * <ul>
     *   <li>A local part with valid characters</li>
     *   <li>An @ symbol</li>
     *   <li>A domain part</li>
     * </ul>
     * 
     * <p><strong>Note:</strong> This is a basic format check and does not verify
     * that the email address actually exists or can receive messages.
     * 
     * @param email the email address to validate
     * @return true if the email matches the expected format, false otherwise
     */
    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}