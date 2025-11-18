package controller;

import database.*;

/**
 * Base class for controllers providing shared repository access and common
 * validation helpers.
 * <p>
 * This abstract class serves as the foundation for all specialized controllers
 * in the system, providing:
 * <ul>
 *   <li>Protected access to all repository interfaces</li>
 *   <li>Common validation methods for input checking</li>
 *   <li>Standardized constructor for dependency injection</li>
 *   <li>Shared utility methods for common operations</li>
 * </ul>
 * 
 * Controllers extending this class inherit consistent access to data repositories
 * and common validation logic, promoting code reuse and maintainability.
*
 */
public class BaseController {
    /** 
     * Repository providing access to student records. May be null for some controllers. 
     */
    protected final StudentRepository studentRepo;

    /** 
     * Repository providing access to company representative records. 
     */
    protected final CompanyRepRepository repRepo;

    /** 
     * Repository providing access to career center staff records. 
     */
    protected final CareerCenterStaffRepository staffRepo;

    /** 
     * Repository providing access to internships. 
     */
    protected final InternshipRepository internshipRepo;

    /** 
     * Repository providing access to internship applications. 
     */
    protected final ApplicationRepository applicationRepo;

    /** 
     * Repository providing access to withdrawal requests. 
     */
    protected final WithdrawalRequestRepository withdrawalRepo;
    
    /**
     * Constructs a controller with the repositories it needs.
     * <p>
     * All repositories are marked as final and must be provided during construction.
     * Some repositories may be null for controllers that don't require access to
     * specific entity types.
     *
     * @param studentRepo student repository (may be null for controllers that don't need student data)
     * @param repRepo company representative repository
     * @param staffRepo career center staff repository
     * @param internshipRepo internship repository
     * @param applicationRepo internship application repository
     * @param withdrawalRepo withdrawal request repository
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
     * Performs basic email format validation (lightweight check).
     * <p>
     * This method provides a simple validation that checks for basic email format:
     * <ul>
     *   <li>Non-null value</li>
     *   <li>Contains exactly one @ symbol</li>
     *   <li>Has non-empty local part (before @)</li>
     *   <li>Has non-empty domain part (after @)</li>
     *   <li>Domain contains at least one dot</li>
     *   <li>No whitespace characters</li>
     * </ul>
     * Note: This is not a comprehensive email validation and should be supplemented
     * with additional checks if stricter validation is required.
     *
     * @param email the email address to validate
     * @return true if the email superficially looks like a valid email address,
     *         false if null or clearly invalid
     */
    protected boolean isEmailValid(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }
    
    /**
     * Checks whether a string is null or empty after trimming whitespace.
     * <p>
     * This is useful for validating required form fields and input parameters
     * where whitespace-only values should be considered empty.
     *
     * @param str the string to check
     * @return true if the string is null, empty, or contains only whitespace characters;
     *         false if the string contains non-whitespace characters
     */
    protected boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Validates that a numeric value is not negative.
     * <p>
     * Commonly used for validating counts, ages, quantities, and other numeric
     * fields that should not have negative values.
     *
     * @param value the integer value to validate
     * @param fieldName the name of the field being validated (used in exception message)
     * @throws IllegalArgumentException if the value is negative, with a message
     *         indicating the field name
     */
    protected void validateNotNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
    }
}