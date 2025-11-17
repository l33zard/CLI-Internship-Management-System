package entity;

/**
 * Represents the processing status of a withdrawal request submitted by a student.
 * 
 * <p>This enum defines the possible states of a withdrawal request as it moves through
 * the approval workflow managed by career center staff. The status determines what actions
 * can be performed on the request and its associated internship application.
 *
 * <p><b>Status Transitions:</b>
 * <ul>
 *   <li><b>PENDING</b> → APPROVED (staff approves the withdrawal)</li>
 *   <li><b>PENDING</b> → REJECTED (staff rejects the withdrawal)</li>
 * </ul>
 *
 * <p><b>Implications for Associated Application:</b>
 * <ul>
 *   <li><b>APPROVED</b> - Application status is updated (withdrawn) and any reserved slots are released</li>
 *   <li><b>REJECTED</b> - Application remains unchanged in its current state</li>
 *   <li><b>PENDING</b> - Application remains active while awaiting staff decision</li>
 * </ul>
 *
 */
public enum WithdrawalRequestStatus {
    
    /**
     * Withdrawal request has been submitted and is awaiting review by career center staff.
     * In this state:
     * - The associated internship application remains active
     * - The student can still update the withdrawal reason
     * - Staff can approve or reject the request
     * - No changes have been made to the internship application
     */
    PENDING,
    
    /**
     * Withdrawal request has been approved by career center staff.
     * In this state:
     * - The internship application is marked as WITHDRAWN
     * - If the student had accepted the offer, the internship slot is released
     * - The application no longer counts toward the student's active application cap
     * - This is a terminal state for the withdrawal request
     */
    APPROVED,
    
    /**
     * Withdrawal request has been rejected by career center staff.
     * In this state:
     * - The internship application remains unchanged in its current status
     * - The student continues to be bound by the original application terms
     * - If the offer was accepted, the student remains committed to the internship
     * - This is a terminal state for the withdrawal request
     */
    REJECTED
}