package entity;

/**
 * Represents the possible statuses of an application throughout its lifecycle.
 * <p>
 * This enum defines the various states an application can be in, from initial submission
 * to final disposition. The status transitions typically follow this pattern:
 * 
 * <ul>
 *   <li><b>PENDING</b> - Initial state when application is submitted but not yet processed</li>
 *   <li><b>SUCCESSFUL</b> - Application has been approved/accepted</li>
 *   <li><b>UNSUCCESSFUL</b> - Application has been rejected/denied</li>
 *   <li><b>WITHDRAWN</b> - Applicant has voluntarily withdrawn the application</li>
 * </ul>
 * 
 */
public enum ApplicationStatus {
    
    /**
     * Application has been submitted and is awaiting review or processing.
     * This is the initial state for newly submitted applications.
     */
    PENDING,
    
    /**
     * Application has been reviewed and approved/accepted.
     * This represents a successful outcome for the applicant.
     */
    SUCCESSFUL,
    
    /**
     * Application has been reviewed and rejected/denied.
     * This represents an unsuccessful outcome where the application did not meet
     * the required criteria or was not selected.
     */
    UNSUCCESSFUL,
    
    /**
     * Application has been voluntarily withdrawn by the applicant before
     * a final decision was made. This status indicates the applicant chose
     * to remove their application from consideration.
     */
    WITHDRAWN
}