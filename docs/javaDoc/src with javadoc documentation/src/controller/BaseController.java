package controller;

import database.*;

/**
 * Abstract base class providing shared functionality for all controllers.
 * 
 * <p>BaseController follows the Repository pattern by maintaining references to
 * all data repositories and providing common validation utilities. All controllers
 * in the system extend this class to access repositories and shared helper methods.</p>
 * 
 * <h2>Design Pattern</h2>
 * <p>This class implements aspects of several design patterns:</p>
 * <ul>
 *   <li><b>Repository Pattern:</b> Provides centralized repository access</li>
 *   <li><b>Template Method:</b> Subclasses extend for specific user type operations</li>
 *   <li><b>Dependency Injection:</b> Repositories injected via constructor</li>
 * </ul>
 * 
 * <h2>Controller Hierarchy</h2>
 * <pre>
 * BaseController (abstract)
 *  ├── {@link AuthController} - Authentication and session management
 *  ├── {@link StudentController} - Student operations (apply, view, accept)
 *  ├── {@link CompanyRepController} - Company posting and application review
 *  ├── {@link CareerCenterStaffController} - Approval and oversight operations
 *  ├── {@link CompanyRepRegistrationController} - Registration workflow
 *  └── {@link DataSaveController} - Data persistence to CSV
 * </pre>
 * 
 * <h2>Shared Responsibilities</h2>
 * <ul>
 *   <li>Repository access management</li>
 *   <li>Common validation utilities (email, null checks, range validation)</li>
 *   <li>Consistent error handling patterns</li>
 *   <li>Centralized dependency management</li>
 * </ul>
 * 
 * <h2>Repository Management</h2>
 * <p>All repositories are protected final fields, ensuring:</p>
 * <ul>
 *   <li>Immutability after construction</li>
 *   <li>Visibility to all subclasses</li>
 *   <li>Thread-safety for repository references</li>
 *   <li>Consistent access patterns</li>
 * </ul>
 * 
 * @author SC2002 Assignment Team
 * @version 1.0
 * @since 1.0
 * @see StudentController
 * @see CompanyRepController
 * @see CareerCenterStaffController
 * @see AuthController
 */
public abstract class BaseController {
    /**
     * Repository for student data access and persistence.
     * Used by controllers handling student-related operations.
     */
    protected final StudentRepository studentRepo;
    
    /**
     * Repository for company representative data access and persistence.
     * Used by controllers handling company rep operations and authentication.
     */
    protected final CompanyRepRepository repRepo;
    
    /**
     * Repository for career center staff data access and persistence.
     * Used by controllers handling staff operations and authentication.
     */
    protected final CareerCenterStaffRepository staffRepo;
    
    /**
     * Repository for internship posting data access and persistence.
     * Used by controllers managing internship creation, approval, and visibility.
     */
    protected final InternshipRepository internshipRepo;
    
    /**
     * Repository for internship application data access and persistence.
     * Used by controllers managing student applications and company reviews.
     */
    protected final ApplicationRepository applicationRepo;
    
    /**
     * Repository for withdrawal request data access and persistence.
     * Used by controllers handling student withdrawal requests and staff approvals.
     */
    protected final WithdrawalRequestRepository withdrawalRepo;
    
    /**
     * Constructs a BaseController with the specified repositories.
     * 
     * <p>Subclasses should call this constructor with the repositories they need,
     * passing null for repositories not used by that particular controller.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>
     * // StudentController only needs 4 repositories
     * super(students, null, null, internships, applications, withdrawals);
     * 
     * // CompanyRepController needs 3 repositories
     * super(null, reps, null, internships, applications, null);
     * </pre>
     * 
     * @param studentRepo student repository (can be null if not needed)
     * @param repRepo company representative repository (can be null if not needed)
     * @param staffRepo career center staff repository (can be null if not needed)
     * @param internshipRepo internship repository (can be null if not needed)
     * @param applicationRepo application repository (can be null if not needed)
     * @param withdrawalRepo withdrawal request repository (can be null if not needed)
     */
    public BaseController(StudentRepository studentRepo,
                         CompanyRepRepository repRepo,
                         CareerCenterStaffRepository staffRepo,
                         InternshipRepository internshipRepo,
                         ApplicationRepository applicationRepo,
                         WithdrawalRequestRepository withdrawalRepo) {
        this.studentRepo = studentRepo;
        this.repRepo = repRepo;
        this.staffRepo = staffRepo;
        this.internshipRepo = internshipRepo;
        this.applicationRepo = applicationRepo;
        this.withdrawalRepo = withdrawalRepo;
    }
    
    /**
     * Validates if an email address has proper format.
     * 
     * <p>Uses regex pattern matching to ensure email contains:</p>
     * <ul>
     *   <li>Local part (before @)</li>
     *   <li>@ symbol</li>
     *   <li>Domain name</li>
     *   <li>Top-level domain (e.g., .com, .edu)</li>
     * </ul>
     * 
     * <p><b>Valid examples:</b></p>
     * <ul>
     *   <li>user@example.com</li>
     *   <li>john.doe@company.co.uk</li>
     *   <li>student123@university.edu</li>
     * </ul>
     * 
     * <p><b>Invalid examples:</b></p>
     * <ul>
     *   <li>user@domain (no TLD)</li>
     *   <li>@example.com (no local part)</li>
     *   <li>user example@test.com (contains space)</li>
     * </ul>
     * 
     * @param email the email address to validate
     * @return {@code true} if email format is valid, {@code false} otherwise
     */
    protected boolean isEmailValid(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }
    
    /**
     * Checks if a string is null or empty after trimming whitespace.
     * 
     * <p>This is a convenience method for validating required text fields.
     * Returns true if the string is null or contains only whitespace.</p>
     * 
     * <p><b>Examples:</b></p>
     * <pre>
     * isNullOrEmpty(null)         → true
     * isNullOrEmpty("")           → true
     * isNullOrEmpty("   ")        → true
     * isNullOrEmpty("text")       → false
     * isNullOrEmpty("  text  ")   → false
     * </pre>
     * 
     * @param str the string to check
     * @return {@code true} if string is null or empty after trimming, {@code false} otherwise
     */
    protected boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Validates that an integer value is not negative.
     * 
     * <p>Used for validating counts, quantities, and other numeric fields that
     * should not be negative (e.g., maxSlots, yearOfStudy).</p>
     * 
     * @param value the integer value to validate
     * @param fieldName descriptive name of the field for error messages
     * @throws IllegalArgumentException if value is negative (less than 0)
     */
    protected void validateNotNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
    }
}