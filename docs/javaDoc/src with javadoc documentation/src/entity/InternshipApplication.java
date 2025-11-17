package entity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a student's application to an internship opportunity.
 * 
 * <p>This aggregate root manages the complete lifecycle of an application from
 * submission through review, offer, and acceptance/rejection. It enforces business
 * rules and maintains consistency between student applications and internship slots.</p>
 * 
 * <h2>Application Lifecycle</h2>
 * <p>Applications progress through the following states:</p>
 * <ol>
 *   <li><b>PENDING:</b> Initial state after student submission, awaiting company review</li>
 *   <li><b>SUCCESSFUL:</b> Company offers the position to the student</li>
 *   <li><b>UNSUCCESSFUL:</b> Company rejects the application</li>
 *   <li><b>ACCEPTED:</b> Student accepts the successful offer (placement confirmed)</li>
 *   <li><b>REJECTED:</b> Student rejects the successful offer</li>
 *   <li><b>WITHDRAWN:</b> Application withdrawn (typically after staff-approved withdrawal request)</li>
 * </ol>
 * 
 * <h2>Business Rules Enforced</h2>
 * <ul>
 *   <li>Applications can only be created if internship is open and visible</li>
 *   <li>Student must be eligible for the internship level</li>
 *   <li>Student cannot have more than {@value Student#MAX_ACTIVE_APPLICATIONS} active applications</li>
 *   <li>Student cannot have a confirmed placement when applying</li>
 *   <li>Only PENDING applications can be marked SUCCESSFUL or UNSUCCESSFUL</li>
 *   <li>Students can only accept/reject SUCCESSFUL offers</li>
 *   <li>Accepting an offer increments internship's confirmed slots</li>
 * </ul>
 * 
 * <h2>Persistence and Re-attachment</h2>
 * <p>This class supports CSV persistence through:</p>
 * <ul>
 *   <li>Storing internship ID as a durable reference</li>
 *   <li>Allowing internship object to be re-attached after loading</li>
 *   <li>Gracefully handling null internship references during load</li>
 * </ul>
 * 
 * @author SC2002 Assignment Team
 * @version 1.0
 * @since 1.0
 * @see Student
 * @see Internship
 * @see ApplicationStatus
 */
public class InternshipApplication {
    /**
     * Counter for generating unique application IDs.
     * Increments with each new application created.
     */
    private static int idCounter = 1;

    /**
     * Unique identifier for this application.
     * Format: "APPXXXX" where XXXX is a zero-padded 4-digit number.
     * Mutable to support repository loading.
     */
    private String applicationId;
    
    /**
     * Date when the application was submitted.
     * Immutable after creation.
     */
    private final LocalDate appliedOn;
    
    /**
     * The student who submitted this application.
     * Immutable reference to the student entity.
     */
    private final Student student;

    /**
     * Durable reference to the internship by ID.
     * Stored separately to support persistence and re-attachment.
     * May be set independently of the internship object reference.
     */
    private String internshipId;

    /**
     * The internship this application is for.
     * May be null immediately after CSV load, requiring re-attachment.
     * Controllers are responsible for re-attaching internship objects after load.
     */
    private Internship internship;

    /**
     * Flag indicating if the student has accepted a SUCCESSFUL offer.
     * When true, the student has a confirmed placement at this internship.
     */
    private boolean studentAccepted;
    
    /**
     * Current status of the application in the review and acceptance workflow.
     * 
     * @see ApplicationStatus
     */
    private ApplicationStatus status;

    // ---------- Construction ----------

    /**
     * Constructs a new InternshipApplication with full validation.
     * 
     * <p>This constructor performs comprehensive business rule validation including:</p>
     * <ul>
     *   <li>Internship is open for applications on the specified date</li>
     *   <li>Student is eligible for the internship level</li>
     *   <li>Student does not have a confirmed placement</li>
     *   <li>Student has not reached the application cap</li>
     * </ul>
     * 
     * <p>The application is created in PENDING status awaiting company review.
     * A unique application ID is automatically generated.</p>
     * 
     * @param appliedOn the date of application submission
     * @param student the student submitting the application
     * @param internship the internship being applied for
     * @param appReadPort port for checking application constraints
     * @throws NullPointerException if any required parameter is null
     * @throws IllegalStateException if any business rule validation fails
     */
    public InternshipApplication(LocalDate appliedOn,
                                 Student student,
                                 Internship internship,
                                 Student.AppReadPort appReadPort) {
        this.applicationId = String.format("APP%04d", idCounter++);
        this.appliedOn = Objects.requireNonNull(appliedOn, "appliedOn");
        this.student = Objects.requireNonNull(student, "student");
        this.internship = Objects.requireNonNull(internship, "internship");
        this.internshipId = internship.getInternshipId();
        this.status = ApplicationStatus.PENDING;
        this.studentAccepted = false;

        // Creation-time domain checks
        if (!internship.isOpenForApplications(appliedOn)) {
            throw new IllegalStateException("Internship is not open/visible for applications");
        }
        if (!student.isEligibleFor(internship.getLevel())) {
            throw new IllegalStateException("Student not eligible for level " + internship.getLevel());
        }
        if (student.hasConfirmedPlacement(appReadPort)) {
            throw new IllegalStateException("Student already has a confirmed placement");
        }
        if (!student.canStartAnotherApplication(appReadPort)) {
            throw new IllegalStateException("Student reached application cap (" + Student.MAX_ACTIVE_APPLICATIONS + ")");
        }
    }

    /**
     * Constructs a new InternshipApplication with current date as application date.
     * 
     * <p>Convenience constructor that uses the current date as the application date.
     * Delegates to the full constructor for validation.</p>
     * 
     * @param student the student submitting the application
     * @param internship the internship being applied for
     * @param appReadPort port for checking application constraints
     * @throws NullPointerException if any required parameter is null
     * @throws IllegalStateException if any business rule validation fails
     */
    public InternshipApplication(Student student,
                                 Internship internship,
                                 Student.AppReadPort appReadPort) {
        this(LocalDate.now(), student, internship, appReadPort);
    }

    // ---------- Queries ----------

    /**
     * Returns the unique application identifier.
     * 
     * @return the application ID (format: "APPXXXX")
     */
    public String getApplicationId() { return applicationId; }
    
    /**
     * Returns the date when this application was submitted.
     * 
     * @return the application date
     */
    public LocalDate getAppliedOn() { return appliedOn; }
    
    /**
     * Returns the student who submitted this application.
     * 
     * @return the student
     */
    public Student getStudent() { return student; }

    /**
     * Returns the internship ID this application is for.
     * 
     * <p>This durable reference is available even if the internship object
     * has not been re-attached after loading from persistence.</p>
     * 
     * @return the internship ID, or null if not set
     */
    public String getInternshipId() {
        if (internshipId != null) return internshipId;
        return (internship != null ? internship.getInternshipId() : null);
    }

    /**
     * Returns the internship object this application is for.
     * 
     * <p><b>May be null</b> if not yet re-attached after loading from CSV.
     * Controllers should ensure internship objects are re-attached before
     * operations that require the internship reference.</p>
     * 
     * @return the internship, or null if not attached
     */
    public Internship getInternship() { return internship; }

    /**
     * Returns the current status of this application.
     * 
     * @return the status
     * @see ApplicationStatus
     */
    public ApplicationStatus getStatus() { return status; }
    
    /**
     * Checks if the student has accepted this application's offer.
     * 
     * @return {@code true} if student accepted, {@code false} otherwise
     */
    public boolean isStudentAccepted() { return studentAccepted; }

    /**
     * Checks if the student can accept this application.
     * 
     * <p>An application can be accepted if:</p>
     * <ul>
     *   <li>Status is SUCCESSFUL (company offered the position)</li>
     *   <li>Student has not already accepted it</li>
     * </ul>
     * 
     * @return {@code true} if can be accepted, {@code false} otherwise
     */
    public boolean canAccept() {
        return status == ApplicationStatus.SUCCESSFUL && !studentAccepted;
    }

    /**
     * Checks if this application counts toward the student's active application cap.
     * 
     * <p>Active applications are:</p>
     * <ul>
     *   <li>PENDING - Under review</li>
     *   <li>SUCCESSFUL but not accepted - Awaiting student decision</li>
     * </ul>
     * 
     * @return {@code true} if counts as active, {@code false} otherwise
     */
    public boolean isActiveTowardCap() {
        return (status == ApplicationStatus.PENDING) ||
               (status == ApplicationStatus.SUCCESSFUL && !studentAccepted);
    }

    // ---------- Transitions (rep/staff actions) ----------

    /**
     * Marks this application as SUCCESSFUL (company offers the position).
     * 
     * <p>Called by company representatives to offer the position to the student.
     * Student can then choose to accept or reject the offer.</p>
     * 
     * @throws IllegalStateException if current status is not PENDING
     */
    public void markSuccessful() {
        ensureStatus(ApplicationStatus.PENDING, "Only PENDING can be marked SUCCESSFUL");
        this.status = ApplicationStatus.SUCCESSFUL;
    }

    /**
     * Marks this application as UNSUCCESSFUL (company rejects the application).
     * 
     * <p>Called by company representatives to reject the application.
     * Cannot reject if student has already accepted the offer.</p>
     * 
     * @throws IllegalStateException if current status is not PENDING, or if
     *         status is SUCCESSFUL and student has already accepted
     */
    public void markUnsuccessful() {
        if (status == ApplicationStatus.SUCCESSFUL && studentAccepted) {
            throw new IllegalStateException("Cannot reject after student accepted");
        }
        ensureStatus(ApplicationStatus.PENDING, "Only PENDING can be marked UNSUCCESSFUL");
        this.status = ApplicationStatus.UNSUCCESSFUL;
    }

    /**
     * Marks this application as WITHDRAWN.
     * 
     * <p>Called when a withdrawal request is approved by career center staff,
     * releasing the student from their accepted placement.</p>
     */
    public void markWithdrawn() {
        this.status = ApplicationStatus.WITHDRAWN;
    }

    // ---------- Transitions (student actions) ----------

    /**
     * Confirms the student's acceptance of this offer.
     * 
     * <p>This method:</p>
     * <ul>
     *   <li>Validates the student can accept (SUCCESSFUL status, no existing placement)</li>
     *   <li>Increments the internship's confirmed slots</li>
     *   <li>Marks this application as accepted</li>
     * </ul>
     * 
     * <p>After acceptance, the student has a confirmed placement and cannot
     * apply for other internships.</p>
     * 
     * @param appReadPort port for checking if student already has placement
     * @throws IllegalStateException if application cannot be accepted, student
     *         already has placement, or internship not attached
     */
    public void confirmAcceptance(Student.AppReadPort appReadPort) {
        if (!canAccept()) throw new IllegalStateException("Cannot accept in status " + status);
        if (student.hasConfirmedPlacement(appReadPort)) {
            throw new IllegalStateException("Student already has a confirmed placement");
        }
        if (internship == null) {
            throw new IllegalStateException("Internship details not attached; cannot confirm acceptance");
        }
        internship.incrementConfirmedSlots();
        this.studentAccepted = true;
    }

    /**
     * Revokes the student's acceptance after an approved withdrawal request.
     * 
     * <p>Called by the system when a withdrawal request is approved by staff.
     * Decrements the internship's confirmed slots, freeing the position.</p>
     * 
     * @throws IllegalStateException if student has not accepted, or internship not attached
     */
    public void revokeAcceptanceAfterApprovedWithdrawal() {
        if (!studentAccepted) throw new IllegalStateException("No accepted placement to revoke");
        if (internship == null) {
            throw new IllegalStateException("Internship details not attached; cannot revoke acceptance");
        }
        internship.decrementConfirmedSlots();
        this.studentAccepted = false;
    }

    // ---------- ID & Re-attachment for Persistence ----------

    /**
     * Sets the application ID (for repository loading only).
     * 
     * <p>Used by repositories when loading applications from CSV to restore
     * the original application ID.</p>
     * 
     * @param applicationId the application ID to set
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Sets the internship ID when loading from CSV.
     * 
     * <p>Used by repositories to establish the internship reference
     * before the internship object is re-attached.</p>
     * 
     * @param internshipId the internship ID to set
     */
    public void setInternshipId(String internshipId) {
        this.internshipId = internshipId;
    }

    /**
     * Re-attaches the Internship object after repository load.
     * 
     * <p>Controllers call this method after loading applications from CSV
     * to restore the object reference to the associated internship.
     * Safe to call with null to detach.</p>
     * 
     * @param internship the internship to attach, or null to detach
     */
    public void setInternship(Internship internship) {
        this.internship = internship;
        if (internship != null) {
            this.internshipId = internship.getInternshipId();
        }
    }

    // ---------- Helpers ----------

    /**
     * Ensures the current status matches the expected status.
     * 
     * <p>Internal helper method for state transition validation.</p>
     * 
     * @param expected the required current status
     * @param msg error message if status doesn't match
     * @throws IllegalStateException if current status doesn't match expected
     */
    private void ensureStatus(ApplicationStatus expected, String msg) {
        if (this.status != expected) throw new IllegalStateException(msg + " (current=" + this.status + ")");
    }
}
//
//    // ---------- Identity & debug ----------
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof InternshipApplication)) return false;
//        return applicationId.equals(((InternshipApplication) o).applicationId);
//    }
//
//    @Override
//    public int hashCode() { return applicationId.hashCode(); }
//
//    @Override
//    public String toString() {
//        return "Application{" +
//               "id='" + applicationId + '\'' +
//               ", student=" + student.getUserId() +
//               ", internshipId=" + getInternshipId() +
//               ", status=" + status +
//               ", accepted=" + studentAccepted +
//               '}';
//    }
//}
