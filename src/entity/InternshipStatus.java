package entity;

/**
 * Represents the approval and availability status of an internship posting.
 * 
 * <p>This enum defines the lifecycle states of an internship posting as it moves through
 * the approval process and eventual filling or closure. The status determines what actions
 * can be performed on the internship and its visibility to students.
 *
 * <p><b>Lifecycle Flow:</b>
 * <ul>
 *   <li><b>PENDING</b> → APPROVED or REJECTED (staff decision)</li>
 *   <li><b>APPROVED</b> → FILLED (when slots fill) or CLOSED (manual closure)</li>
 *   <li><b>REJECTED</b> - End state (cannot be made visible)</li>
 *   <li><b>FILLED</b> - End state (all slots occupied)</li>
 *   <li><b>CLOSED</b> - End state (manually closed before filling)</li>
 * </ul>
 *
 */
public enum InternshipStatus {
    
    /**
     * Internship posting has been submitted and is awaiting staff approval.
     * In this state:
     * - Not visible to students
     * - Editable by company representative
     * - Can be approved or rejected by career center staff
     */
    PENDING,
    
    /**
     * Internship posting has been approved by career center staff.
     * In this state:
     * - Can be made visible to students (if visibility is enabled)
     * - Accepting applications (if within date range and has available slots)
     * - Can be closed by company representative or automatically filled
     */
    APPROVED,
    
    /**
     * Internship posting has been rejected by career center staff.
     * In this state:
     * - Not visible to students
     * - Cannot be made visible
     * - Can be deleted by company representative
     * - End state for rejected postings
     */
    REJECTED,
    
    /**
     * Internship posting has been manually closed by the company representative.
     * In this state:
     * - Not visible to students
     * - Not accepting applications
     * - May have some confirmed slots but not filled to capacity
     * - End state for manually closed postings
     */
    CLOSED,
    
    /**
     * Internship posting has reached its maximum capacity of confirmed slots.
     * In this state:
     * - Typically not visible to students (automatically hidden)
     * - Not accepting applications
     * - All available slots have been filled by student acceptances
     * - End state for filled postings
     */
    FILLED;
}