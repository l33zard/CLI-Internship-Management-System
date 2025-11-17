package entity;

/**
 * Enumeration of internship posting lifecycle states.
 * 
 * <p>This enum tracks an internship's progression through the approval workflow
 * and its current availability status. Status determines visibility to students
 * and what actions company representatives and staff can perform.</p>
 * 
 * <h2>Status Lifecycle</h2>
 * <pre>
 * PENDING → APPROVED → (slots fill) → FILLED
 *    ↓
 * REJECTED
 * </pre>
 * 
 * <h2>Status Permissions</h2>
 * <table border="1" cellpadding="5">
 *   <tr>
 *     <th>Status</th>
 *     <th>Can Edit?</th>
 *     <th>Can Delete?</th>
 *     <th>Can Make Visible?</th>
 *     <th>Students Can Apply?</th>
 *   </tr>
 *   <tr>
 *     <td>PENDING</td>
 *     <td>Yes</td>
 *     <td>Yes</td>
 *     <td>No</td>
 *     <td>No</td>
 *   </tr>
 *   <tr>
 *     <td>APPROVED</td>
 *     <td>No</td>
 *     <td>No</td>
 *     <td>Yes</td>
 *     <td>Yes (if visible)</td>
 *   </tr>
 *   <tr>
 *     <td>REJECTED</td>
 *     <td>No</td>
 *     <td>Yes</td>
 *     <td>No</td>
 *     <td>No</td>
 *   </tr>
 *   <tr>
 *     <td>FILLED</td>
 *     <td>No</td>
 *     <td>No</td>
 *     <td>N/A</td>
 *     <td>No</td>
 *   </tr>
 * </table>
 * 
 * @author SC2002 Assignment Team
 * @version 1.0
 * @since 1.0
 * @see Internship#getStatus()
 * @see Internship#approve()
 * @see Internship#reject()
 */
public enum InternshipStatus {
	/**
	 * Initial status after internship creation, awaiting staff review.
	 * 
	 * <p>Characteristics of PENDING internships:</p>
	 * <ul>
	 *   <li>Not visible to students</li>
	 *   <li>Can be edited by company representative</li>
	 *   <li>Can be deleted by company representative</li>
	 *   <li>Awaiting career center staff approval</li>
	 *   <li>Does not accept applications</li>
	 * </ul>
	 * 
	 * <p><b>Transitions from PENDING:</b></p>
	 * <ul>
	 *   <li>→ APPROVED (staff approves posting)</li>
	 *   <li>→ REJECTED (staff rejects posting)</li>
	 * </ul>
	 */
	PENDING,
	
	/**
	 * Approved by staff and ready for student applications.
	 * 
	 * <p>Characteristics of APPROVED internships:</p>
	 * <ul>
	 *   <li>Can be made visible to students by company rep</li>
	 *   <li>Cannot be edited (ensures integrity after approval)</li>
	 *   <li>Cannot be deleted (preserves application history)</li>
	 *   <li>Accepts applications when visible and within date range</li>
	 *   <li>Company rep can toggle visibility on/off</li>
	 * </ul>
	 * 
	 * <p><b>Transitions from APPROVED:</b></p>
	 * <ul>
	 *   <li>→ FILLED (all slots confirmed by student acceptances)</li>
	 *   <li>→ APPROVED (if slots freed by withdrawals)</li>
	 * </ul>
	 */
	APPROVED,
	
	/**
	 * Rejected by staff, not suitable for platform.
	 * 
	 * <p>Characteristics of REJECTED internships:</p>
	 * <ul>
	 *   <li>Not visible to students</li>
	 *   <li>Cannot be edited</li>
	 *   <li>Can be deleted by company representative</li>
	 *   <li>Company rep can create new posting addressing concerns</li>
	 *   <li>Rejection reason stored for feedback</li>
	 * </ul>
	 * 
	 * <p><b>No transitions from REJECTED</b> - terminal state</p>
	 */
	REJECTED,
	
	/**
	 * Closed by company representative, no longer accepting applications.
	 * 
	 * <p>Characteristics of CLOSED internships:</p>
	 * <ul>
	 *   <li>Posting ended by company representative</li>
	 *   <li>Not visible to students</li>
	 *   <li>Does not accept new applications</li>
	 *   <li>Existing applications remain valid</li>
	 *   <li>Can be used when position filled externally</li>
	 * </ul>
	 */
	CLOSED,
	
	/**
	 * All slots filled, capacity reached.
	 * 
	 * <p>Characteristics of FILLED internships:</p>
	 * <ul>
	 *   <li>Confirmed slots equals maximum slots</li>
	 *   <li>Automatically set when last slot confirmed</li>
	 *   <li>Not visible to students</li>
	 *   <li>Does not accept new applications</li>
	 *   <li>Can revert to APPROVED if slot freed (e.g., withdrawal)</li>
	 * </ul>
	 * 
	 * <p><b>Transitions from FILLED:</b></p>
	 * <ul>
	 *   <li>→ APPROVED (automatically when slot freed by approved withdrawal)</li>
	 * </ul>
	 */
	FILLED;
}
