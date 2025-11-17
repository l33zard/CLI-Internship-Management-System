package entity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a student's request to withdraw from an internship application.
 * 
 * <p>This domain model manages the complete lifecycle of a withdrawal request, from creation
 * through staff processing. It maintains an audit trail of all processing actions and ensures
 * proper coordination with the associated internship application.
 *
 * <p><b>Lifecycle States:</b>
 * <ul>
 *   <li><b>PENDING</b> - Request created, awaiting staff processing</li>
 *   <li><b>APPROVED</b> - Request approved by staff, application updated accordingly</li>
 *   <li><b>REJECTED</b> - Request rejected by staff, application remains unchanged</li>
 * </ul>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Maintains audit trail of staff processing actions</li>
 *   <li>Coordinates with internship application state changes</li>
 *   <li>Handles both accepted and pending application withdrawals</li>
 *   <li>Provides reason tracking for both students and staff</li>
 * </ul>
 *
 */
public class WithdrawalRequest {
    private static int idCounter = 1;

    private String requestId;                 // e.g., WRQ0001
    private InternshipApplication application;  // CHANGED: InternshipApplication instead of Application
    private Student requestedBy;              // must equal application.getStudent()
    private LocalDate requestedOn;

    private String reason;                          // optional free text, stored trimmed
    private WithdrawalRequestStatus status = WithdrawalRequestStatus.PENDING;

    // audit when processed by staff
    private CareerCenterStaff processedBy;          // null until processed
    private LocalDate processedOn;                  // null until processed
    private String staffNote;                       // optional

    /**
     * Creates a new withdrawal request for the specified application.
     * Validates that the requesting student owns the application being withdrawn.
     *
     * @param application the internship application being withdrawn (must belong to requester)
     * @param requestedBy the student requesting the withdrawal
     * @param reason optional free-text reason for withdrawal (will be trimmed)
     * @throws NullPointerException if application or requestedBy is null
     * @throws IllegalArgumentException if requester does not match application owner
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
     * @return the request ID in format "WRQ####"
     */
    public String getRequestId()      { return requestId; }
    
    /**
     * Returns the internship application being withdrawn.
     *
     * @return the associated internship application
     */
    public InternshipApplication getApplication() { return application; }  // CHANGED return type
    
    /**
     * Returns the student who requested the withdrawal.
     *
     * @return the requesting student
     */
    public Student getRequestedBy()   { return requestedBy; }
    
    /**
     * Returns the date when the withdrawal request was created.
     *
     * @return the request creation date
     */
    public LocalDate getRequestedOn() { return requestedOn; }
    
    /**
     * Returns the student's reason for withdrawal.
     *
     * @return the reason text (may be empty string if no reason provided)
     */
    public String getReason()         { return reason; }
    
    /**
     * Returns the current processing status of the request.
     *
     * @return the withdrawal request status
     */
    public WithdrawalRequestStatus getStatus() { return status; }
    
    /**
     * Returns the staff member who processed the request.
     *
     * @return the processing staff member, or null if still pending
     */
    public CareerCenterStaff getProcessedBy()  { return processedBy; }
    
    /**
     * Returns the date when the request was processed.
     *
     * @return the processing date, or null if still pending
     */
    public LocalDate getProcessedOn()          { return processedOn; }
    
    /**
     * Returns the optional note added by processing staff.
     *
     * @return the staff note, or null if no note provided
     */
    public String getStaffNote()               { return staffNote; }
    
    /**
     * Checks if the request is still awaiting staff action.
     *
     * @return true if status is PENDING, false otherwise
     */
    public boolean isPending() { return status == WithdrawalRequestStatus.PENDING; }

    // ---------- Commands ----------

    /**
     * Updates the student's reason text for the withdrawal request.
     * This operation is only allowed while the request is still PENDING.
     *
     * @param reason the new reason text
     * @throws IllegalStateException if the request has already been processed
     */
    public void setReason(String reason) {
        ensurePending();
        this.reason = sanitize(reason);
    }

    /**
     * Approves the withdrawal request and updates related aggregates atomically.
     * 
     * <p><b>Processing Logic:</b>
     * <ul>
     *   <li>If student had confirmed acceptance: frees up the internship slot and unsets acceptance</li>
     *   <li>If application was PENDING or SUCCESSFUL (unconfirmed): marks application as WITHDRAWN</li>
     * </ul>
     *
     * @param staff the career center staff member performing the approval
     * @param note optional note from staff explaining the decision
     * @throws NullPointerException if staff is null
     * @throws IllegalStateException if request is not in PENDING status
     */
    public void approve(CareerCenterStaff staff, String note) {
        Objects.requireNonNull(staff, "staff");
        ensurePending();

        if (application.isStudentAccepted()) {
            // frees up a slot and unsets acceptance, keeps status SUCCESSFUL (offer existed)
            application.revokeAcceptanceAfterApprovedWithdrawal();
        } else {
            // If still PENDING (or SUCCESSFUL but not confirmed), remove from the active pool
            if (application.getStatus() == ApplicationStatus.PENDING ||
                application.getStatus() == ApplicationStatus.SUCCESSFUL) {
                application.markWithdrawn();
            }
        }

        this.status = WithdrawalRequestStatus.APPROVED;
        stamp(staff, note);
    }

    /**
     * Rejects the withdrawal request.
     * The associated internship application remains unchanged.
     *
     * @param staff the career center staff member performing the rejection
     * @param note optional note from staff explaining the rejection
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
     * Ensures the request is still in PENDING status.
     *
     * @throws IllegalStateException if the request has already been processed
     */
    private void ensurePending() {
        if (!isPending()) throw new IllegalStateException("Request already processed: " + status);
    }

    /**
     * Records staff processing information including timestamp and notes.
     *
     * @param staff the staff member processing the request
     * @param note optional processing note
     */
    private void stamp(CareerCenterStaff staff, String note) {
        this.processedBy = staff;
        this.processedOn = LocalDate.now();
        this.staffNote   = sanitize(note);
    }

    /**
     * Sanitizes text input by trimming and limiting length.
     *
     * @param s the input string to sanitize
     * @return the sanitized string (empty string if input was null, trimmed and length-capped otherwise)
     */
    private static String sanitize(String s) {
        if (s == null) return "";
        String t = s.trim();
        // Optional: cap very long notes to avoid accidental huge payloads
        return t.length() > 2000 ? t.substring(0, 2000) : t;
    }

    // ---------- Identity & debug ----------

//    /**
//     * Compares this withdrawal request with another object for equality based on request ID.
//     *
//     * @param o the object to compare with
//     * @return true if the objects are equal, false otherwise
//     */
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof WithdrawalRequest)) return false;
//        WithdrawalRequest that = (WithdrawalRequest) o;
//        return requestId.equals(that.requestId);
//    }
//
//    /**
//     * Returns the hash code for this withdrawal request based on the request ID.
//     *
//     * @return the hash code
//     */
//    @Override
//    public int hashCode() { return requestId.hashCode(); }
//
//    /**
//     * Returns a string representation of this withdrawal request.
//     *
//     * @return formatted string with key request details
//     */
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