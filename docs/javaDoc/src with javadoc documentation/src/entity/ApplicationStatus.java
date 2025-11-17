package entity;

/**
 * Enumeration of application states throughout the review and acceptance process.
 * 
 * <p>Application status tracks the progression of a student's application from
 * submission through company review, offer, and final student decision. Status
 * determines what actions are available to students and company representatives.</p>
 * 
 * <h2>Status Workflow</h2>
 * <pre>
 * PENDING → SUCCESSFUL → ACCEPTED (placement confirmed)
 *    ↓         ↓
 * UNSUCCESSFUL  REJECTED (student declines offer)
 *    
 * Any status → WITHDRAWN (via approved withdrawal request)
 * </pre>
 * 
 * <h2>Active vs Inactive Status</h2>
 * <p><b>Active</b> (counts toward 3-application cap):</p>
 * <ul>
 *   <li>PENDING - Under company review</li>
 *   <li>SUCCESSFUL - Offered but not yet accepted by student</li>
 * </ul>
 * 
 * <p><b>Inactive</b> (does not count toward cap):</p>
 * <ul>
 *   <li>UNSUCCESSFUL - Rejected by company</li>
 *   <li>ACCEPTED - Student accepted (has placement)</li>
 *   <li>REJECTED - Student declined offer</li>
 *   <li>WITHDRAWN - Withdrawn via approved request</li>
 * </ul>
 * 
 * @author SC2002 Assignment Team
 * @version 1.0
 * @since 1.0
 * @see InternshipApplication#getStatus()
 * @see Student#activeApplicationsCount(Student.AppReadPort)
 */
public enum ApplicationStatus {
	/**
	 * Application submitted and awaiting company review.
	 * 
	 * <p>Characteristics of PENDING applications:</p>
	 * <ul>
	 *   <li>Counts toward student's 3-application cap</li>
	 *   <li>Under review by company representative</li>
	 *   <li>Company can mark SUCCESSFUL or UNSUCCESSFUL</li>
	 *   <li>Student cannot take action yet</li>
	 *   <li>Can be withdrawn via withdrawal request</li>
	 * </ul>
	 * 
	 * <p><b>Transitions from PENDING:</b></p>
	 * <ul>
	 *   <li>→ SUCCESSFUL (company offers position)</li>
	 *   <li>→ UNSUCCESSFUL (company rejects application)</li>
	 *   <li>→ WITHDRAWN (via approved withdrawal request)</li>
	 * </ul>
	 */
	PENDING,
	
	/**
	 * Company offered the position to the student.
	 * 
	 * <p>Characteristics of SUCCESSFUL applications:</p>
	 * <ul>
	 *   <li>Counts toward student's cap until accepted/rejected</li>
	 *   <li>Student has received an offer</li>
	 *   <li>Student can accept (→ ACCEPTED) or reject (→ REJECTED)</li>
	 *   <li>Company cannot revoke offer after extending it</li>
	 *   <li>Student decision is final</li>
	 * </ul>
	 * 
	 * <p><b>Transitions from SUCCESSFUL:</b></p>
	 * <ul>
	 *   <li>→ ACCEPTED (student accepts offer)</li>
	 *   <li>→ REJECTED (student declines offer)</li>
	 *   <li>→ WITHDRAWN (via approved withdrawal request)</li>
	 * </ul>
	 */
	SUCCESSFUL,
	
	/**
	 * Company rejected the application.
	 * 
	 * <p>Characteristics of UNSUCCESSFUL applications:</p>
	 * <ul>
	 *   <li>Does not count toward student's application cap</li>
	 *   <li>No further action possible</li>
	 *   <li>Student can apply for other internships</li>
	 *   <li>Terminal state (no transitions out)</li>
	 * </ul>
	 * 
	 * <p><b>No transitions from UNSUCCESSFUL</b> - terminal state</p>
	 */
	UNSUCCESSFUL,
	
	/**
	 * Application withdrawn via approved withdrawal request.
	 * 
	 * <p>Characteristics of WITHDRAWN applications:</p>
	 * <ul>
	 *   <li>Does not count toward student's application cap</li>
	 *   <li>Student requested and staff approved withdrawal</li>
	 *   <li>If was accepted, internship slot is freed</li>
	 *   <li>Student can now apply for other internships</li>
	 *   <li>Withdrawal request provides audit trail</li>
	 * </ul>
	 * 
	 * <p><b>No transitions from WITHDRAWN</b> - terminal state</p>
	 */
	WITHDRAWN
}
