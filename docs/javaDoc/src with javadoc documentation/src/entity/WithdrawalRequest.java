package entity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a student's request to withdraw from an accepted internship placement.
 * 
 * <p>Withdrawal requests provide a formal process for students to exit accepted
 * placements with staff oversight. This ensures proper slot management and
 * maintains system integrity by requiring approval before releasing commitments.</p>
 * 
 * <h2>When Withdrawals Are Needed</h2>
 * <p>Students may request withdrawal in scenarios such as:</p>
 * <ul>
 *   <li>Accepting a better opportunity elsewhere</li>
 *   <li>Personal or family circumstances</li>
 *   <li>Academic scheduling conflicts</li>
 *   <li>Health or medical reasons</li>
 *   <li>Company or position no longer suitable</li>
 * </ul>
 * 
 * <h2>Withdrawal Process</h2>
 * <ol>
 *   <li><b>Student submits request</b> with reason (status: PENDING)</li>
 *   <li><b>Career center staff reviews</b> the request and circumstances</li>
 *   <li><b>Staff approves or rejects</b> with optional note</li>
 *   <li><b>If approved:</b> Placement freed, internship slot released</li>
 *   <li><b>If rejected:</b> Student remains in accepted placement</li>
 * </ol>
 * 
 * <h2>Impact of Approved Withdrawals</h2>
 * <p>When a withdrawal is approved, the system automatically:</p>
 * <ul>
 *   <li>Releases the student from their placement commitment</li>
 *   <li>Decrements the internship's confirmed slot count</li>
 *   <li>Updates application status to WITHDRAWN</li>
 *   <li>Allows student to apply for other internships again</li>
 *   <li>Makes the freed slot available to other students</li>
 * </ul>
 * 
 * <h2>Business Rules</h2>
 * <ul>
 *   <li>Only students with accepted placements can request withdrawal</li>
 *   <li>Withdrawal requests can only be created by the application owner</li>
 *   <li>Requests in PENDING status can have reason updated by student</li>
 *   <li>Once processed (APPROVED/REJECTED), requests become immutable</li>
 *   <li>Staff approval is mandatory - no automatic withdrawals</li>
 * </ul>
 * 
 * <h2>Audit Trail</h2>
 * <p>Withdrawal requests maintain a complete audit trail including:</p>
 * <ul>
 *   <li>Who requested (student)</li>
 *   <li>When requested (date)</li>
 *   <li>Why requested (reason)</li>
 *   <li>Who processed (staff member)</li>
 *   <li>When processed (date)</li>
 *   <li>Staff notes on decision</li>
 * </ul>
 * 
 * @author SC2002 Assignment Team
 * @version 1.0
 * @since 1.0
 * @see WithdrawalRequestStatus
 * @see InternshipApplication
 * @see CareerCenterStaff
 * @see Student
 */
public class WithdrawalRequest {
    /**
     * Counter for generating unique withdrawal request IDs.
     * Increments with each new request created.
     */
    private static int idCounter = 1;

    /**
     * Unique identifier for this withdrawal request.
     * Format: "WRQXXXX" where XXXX is a zero-padded 4-digit number.
     */
    private String requestId;
    
    /**
     * The internship application the student wants to withdraw from.
     * Must be an application in ACCEPTED status (student confirmed placement).
     */
    private InternshipApplication application;
    
    /**
     * The student who submitted this withdrawal request.
     * Must be the same student who owns the application.
     */
    private Student requestedBy;
    
    /**
     * Date when the withdrawal request was submitted.
     */
    private LocalDate requestedOn;

    /**
     * Student's explanation for requesting withdrawal.
     * Optional free text, stored trimmed and length-limited.
     */
    private String reason;
    
    /**
     * Current status of this withdrawal request.
     * 
     * @see WithdrawalRequestStatus
     */
    private WithdrawalRequestStatus status = WithdrawalRequestStatus.PENDING;

    /**
     * Career center staff member who processed this request.
     * Null until request is processed (approved or rejected).
     */
    private CareerCenterStaff processedBy;
    
    /**
     * Date when the request was processed by staff.
     * Null until request is processed (approved or rejected).
     */
    private LocalDate processedOn;
    
    /**
     * Optional note from staff explaining the approval/rejection decision.
     * Can be used to provide guidance or explain policy application.
     */
    private String staffNote;

    /**
     * Constructs a new WithdrawalRequest for an accepted internship placement.
     * 
     * <p>The request is created in PENDING status awaiting staff review.
     * A unique request ID is automatically generated. The current date is
     * recorded as the request submission date.</p>
     * 
     * <p><b>Validation:</b></p>
     * <ul>
     *   <li>Application must not be null</li>
     *   <li>Requesting student must not be null</li>
     *   <li>Requesting student must be the owner of the application</li>
     * </ul>
     * 
     * @param application the internship application to withdraw from
     * @param requestedBy the student submitting the withdrawal request
     * @param reason the student's explanation for withdrawal (can be null)
     * @throws NullPointerException if application or requestedBy is null
     * @throws IllegalArgumentException if requestedBy is not the application owner
     */
    public WithdrawalRequest(InternshipApplication application, Student requestedBy, String reason) {
        this.requestId   = String.format("WRQ%04d", idCounter++);
        this.application = Objects.requireNonNull(application, "application");
        this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy");
        if (application.getStudent() == null || !application.getStudent().equals(requestedBy)) {
            throw new IllegalArgumentException("Requester must be the owner of the application");
        }
        this.requestedOn = LocalDate.now();
        this.reason      = sanitize(reason);
    }

    // ---------- Queries ----------

    /**
     * Returns the unique withdrawal request identifier.
     * 
     * @return the request ID (format: "WRQXXXX")
     */
    public String getRequestId()      { return requestId; }
    
    /**
     * Returns the internship application this withdrawal request pertains to.
     * 
     * @return the application
     */
    public InternshipApplication getApplication() { return application; }
    
    /**
     * Returns the student who submitted this withdrawal request.
     * 
     * @return the requesting student
     */
    public Student getRequestedBy()   { return requestedBy; }
    
    /**
     * Returns the date when this request was submitted.
     * 
     * @return the submission date
     */
    public LocalDate getRequestedOn() { return requestedOn; }
    
    /**
     * Returns the student's reason for requesting withdrawal.
     * 
     * @return the reason text, or empty string if not provided
     */
    public String getReason()         { return reason; }
    
    /**
     * Returns the current status of this withdrawal request.
     * 
     * @return the status (PENDING, APPROVED, or REJECTED)
     */
    public WithdrawalRequestStatus getStatus() { return status; }
    
    /**
     * Returns the staff member who processed this request.
     * 
     * @return the processing staff member, or null if not yet processed
     */
    public CareerCenterStaff getProcessedBy()  { return processedBy; }
    
    /**
     * Returns the date when this request was processed.
     * 
     * @return the processing date, or null if not yet processed
     */
    public LocalDate getProcessedOn()          { return processedOn; }
    
    /**
     * Returns the staff's note explaining the approval/rejection decision.
     * 
     * @return the staff note, or empty string if none provided
     */
    public String getStaffNote()               { return staffNote; }
    
    /**
     * Checks if this withdrawal request is still pending staff review.
     * 
     * @return {@code true} if status is PENDING, {@code false} otherwise
     */
    public boolean isPending() { return status == WithdrawalRequestStatus.PENDING; }

    // ---------- Commands ----------

    /**
     * Updates the student's reason text before staff processes the request.
     * 
     * <p>Allows students to edit or clarify their withdrawal reason while
     * the request is still pending. Cannot be changed after processing.</p>
     * 
     * @param reason the new reason text (can be null)
     * @throws IllegalStateException if request is not in PENDING status
     */
    public void setReason(String reason) {
        ensurePending();
        this.reason = sanitize(reason);
    }

    /**
     * Approves the withdrawal and updates linked aggregates atomically.
     * 
     * <p>This method handles the complex state transitions required when
     * a withdrawal is approved:</p>
     * 
     * <h3>If Student Had Confirmed Placement:</h3>
     * <ul>
     *   <li>Revokes the student's acceptance</li>
     *   <li>Decrements internship's confirmed slot count</li>
     *   <li>Frees the slot for other students</li>
     *   <li>Application remains SUCCESSFUL (offer existed)</li>
     * </ul>
     * 
     * <h3>If Application Still PENDING or Unconfirmed SUCCESSFUL:</h3>
     * <ul>
     *   <li>Marks application as WITHDRAWN</li>
     *   <li>Removes from active application count</li>
     *   <li>No slot impact (wasn't confirmed)</li>
     * </ul>
     * 
     * <p>The request status changes to APPROVED and staff/date are recorded.</p>
     * 
     * @param staff the career center staff member approving the request
     * @param note optional explanation for approval decision
     * @throws NullPointerException if staff is null
     * @throws IllegalStateException if request is not in PENDING status
     */
    public void approve(CareerCenterStaff staff, String note) {
        Objects.requireNonNull(staff, "staff");
        ensurePending();

        if (application.isStudentAccepted()) {
            // Student had confirmed placement - free up the slot
            application.revokeAcceptanceAfterApprovedWithdrawal();
        } else {
            // Application not yet confirmed - just mark withdrawn
            if (application.getStatus() == ApplicationStatus.PENDING ||
                application.getStatus() == ApplicationStatus.SUCCESSFUL) {
                application.markWithdrawn();
            }
        }

        this.status = WithdrawalRequestStatus.APPROVED;
        stamp(staff, note);
    }

    /**
     * Rejects the withdrawal request; application remains unchanged.
     * 
     * <p>When staff rejects a withdrawal:</p>
     * <ul>
     *   <li>Student remains committed to their accepted placement</li>
     *   <li>Application status unchanged</li>
     *   <li>Internship slot remains confirmed</li>
     *   <li>Request status changes to REJECTED</li>
     *   <li>Staff note can explain policy or reasoning</li>
     * </ul>
     * 
     * @param staff the career center staff member rejecting the request
     * @param note optional explanation for rejection decision
     * @throws NullPointerException if staff is null
     * @throws IllegalStateException if request is not in PENDING status
     */
    public void reject(CareerCenterStaff staff, String note) {
        Objects.requireNonNull(staff, "staff");
        ensurePending();
        this.status = WithdrawalRequestStatus.REJECTED;
        stamp(staff, note);
    }

    // ---------- Helpers ----------

    /**
     * Ensures this request is in PENDING status before allowing modifications.
     * 
     * <p>Internal guard method to prevent changes to processed requests.</p>
     * 
     * @throws IllegalStateException if status is not PENDING
     */
    private void ensurePending() {
        if (!isPending()) throw new IllegalStateException("Request already processed: " + status);
    }

    /**
     * Records staff member, processing date, and note when request is processed.
     * 
     * <p>Internal helper method called by both approve() and reject().</p>
     * 
     * @param staff the staff member processing the request
     * @param note the staff's note (can be null)
     */
    private void stamp(CareerCenterStaff staff, String note) {
        this.processedBy = staff;
        this.processedOn = LocalDate.now();
        this.staffNote   = sanitize(note);
    }

    /**
     * Sanitizes text input by trimming and enforcing length limits.
     * 
     * <p>Prevents accidental storage of huge text payloads by capping
     * at 2000 characters. Returns empty string for null input.</p>
     * 
     * @param s the text to sanitize
     * @return sanitized text (trimmed and length-limited)
     */
    private static String sanitize(String s) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() > 2000 ? t.substring(0, 2000) : t;
    }


    // ---------- Identity & debug ----------

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof WithdrawalRequest)) return false;
//        WithdrawalRequest that = (WithdrawalRequest) o;
//        return requestId.equals(that.requestId);
//    }
//
//    @Override
//    public int hashCode() { return requestId.hashCode(); }
//
//    @Override
//    public String toString() {
//        return "WithdrawalRequest{" +
//                "id='" + requestId + '\'' +
//                ", app=" + application.getApplicationId() +
//                ", by=" + requestedBy.getUserId() +
//                ", status=" + status +
//                '}';
//    }
}