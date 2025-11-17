package entity;

/**
 * Enumeration of withdrawal request states in the approval workflow.
 * 
 * <p>Withdrawal request status tracks the progression of a student's request
 * to exit an accepted internship placement. All withdrawal requests require
 * career center staff approval to ensure proper oversight and slot management.</p>
 * 
 * <h2>Status Workflow</h2>
 * <pre>
 * PENDING → APPROVED (placement freed)
 *    ↓
 * PENDING → REJECTED (placement maintained)
 * </pre>
 * 
 * <h2>Status Characteristics</h2>
 * <ul>
 *   <li><b>PENDING:</b> Awaiting staff review, student can update reason</li>
 *   <li><b>APPROVED:</b> Staff approved, placement released, slot freed</li>
 *   <li><b>REJECTED:</b> Staff denied, student remains in placement</li>
 * </ul>
 * 
 * @author SC2002 Assignment Team
 * @version 1.0
 * @since 1.0
 * @see WithdrawalRequest#getStatus()
 * @see WithdrawalRequest#approve(CareerCenterStaff, String)
 * @see WithdrawalRequest#reject(CareerCenterStaff, String)
 */
public enum WithdrawalRequestStatus {
	/**
	 * Withdrawal request submitted and awaiting staff review.
	 * 
	 * <p>Characteristics of PENDING withdrawal requests:</p>
	 * <ul>
	 *   <li>Student has submitted request with reason</li>
	 *   <li>Student can update the reason text</li>
	 *   <li>Career center staff reviewing circumstances</li>
	 *   <li>Student remains in accepted placement</li>
	 *   <li>No changes to internship or application yet</li>
	 * </ul>
	 * 
	 * <p><b>Available Actions:</b></p>
	 * <ul>
	 *   <li>Student: Update reason for withdrawal</li>
	 *   <li>Staff: Approve or reject with optional note</li>
	 * </ul>
	 * 
	 * <p><b>Transitions from PENDING:</b></p>
	 * <ul>
	 *   <li>→ APPROVED (staff approves withdrawal)</li>
	 *   <li>→ REJECTED (staff denies withdrawal)</li>
	 * </ul>
	 */
	PENDING,
	
	/**
	 * Staff approved the withdrawal request.
	 * 
	 * <p>When a withdrawal is APPROVED:</p>
	 * <ul>
	 *   <li>Student is released from placement commitment</li>
	 *   <li>If placement was confirmed, internship slot is freed</li>
	 *   <li>Application status updated to WITHDRAWN</li>
	 *   <li>Student can now apply for other internships</li>
	 *   <li>Freed slot becomes available to other students</li>
	 * </ul>
	 * 
	 * <p><b>Audit Information Captured:</b></p>
	 * <ul>
	 *   <li>Which staff member approved</li>
	 *   <li>When approval occurred</li>
	 *   <li>Optional staff note explaining decision</li>
	 * </ul>
	 * 
	 * <p><b>No transitions from APPROVED</b> - terminal state</p>
	 */
	APPROVED,
	
	/**
	 * Staff rejected the withdrawal request.
	 * 
	 * <p>When a withdrawal is REJECTED:</p>
	 * <ul>
	 *   <li>Student remains committed to accepted placement</li>
	 *   <li>Application status unchanged (still ACCEPTED)</li>
	 *   <li>Internship slot remains confirmed</li>
	 *   <li>Student cannot apply for other internships</li>
	 *   <li>Staff note may explain policy or reasoning</li>
	 * </ul>
	 * 
	 * <p><b>Common Rejection Reasons:</b></p>
	 * <ul>
	 *   <li>Too close to internship start date</li>
	 *   <li>Insufficient justification</li>
	 *   <li>Policy violations</li>
	 *   <li>Better resolution path available</li>
	 * </ul>
	 * 
	 * <p><b>Audit Information Captured:</b></p>
	 * <ul>
	 *   <li>Which staff member rejected</li>
	 *   <li>When rejection occurred</li>
	 *   <li>Optional staff note explaining decision</li>
	 * </ul>
	 * 
	 * <p><b>No transitions from REJECTED</b> - terminal state</p>
	 */
	REJECTED
}
