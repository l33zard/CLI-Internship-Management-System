package entity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a student's application to an internship, managing the complete application lifecycle.
 * 
 * <p>This aggregate entity handles the state transitions of an internship application from submission
 * through decision, acceptance, and potential withdrawal. It enforces domain rules regarding student
 * eligibility, application caps, and internship availability.
 *
 * <p><b>Key Lifecycle States:</b>
 * <ul>
 *   <li><b>PENDING</b> - Application submitted, awaiting company decision</li>
 *   <li><b>SUCCESSFUL</b> - Company offered the position to student</li>
 *   <li><b>UNSUCCESSFUL</b> - Company rejected the application</li>
 *   <li><b>WITHDRAWN</b> - Student or system withdrew the application</li>
 * </ul>
 *
 * <p><b>Domain Validations:</b>
 * <ul>
 *   <li>Internship must be open for applications</li>
 *   <li>Student must be eligible for internship level</li>
 *   <li>Student must not have existing confirmed placement</li>
 *   <li>Student must be under application cap</li>
 * </ul>
 *
 */
public class InternshipApplication {
    private static int idCounter = 1;

    private String applicationId;          // (mutable for repo loading)
    private final LocalDate appliedOn;
    private final Student student;

    // CHANGED: store durable internshipId for persistence/re-attachment
    private String internshipId;

    // CHANGED: make internship non-final so we can re-attach after load
    private Internship internship;         // may be null right after CSV load

    private boolean studentAccepted;
    private ApplicationStatus status;

    // ---------- Construction ----------

    /**
     * Creates a new internship application with the specified application date.
     * 
     * <p>Performs comprehensive domain validation including:
     * <ul>
     *   <li>Internship availability and visibility</li>
     *   <li>Student eligibility for internship level</li>
     *   <li>Student application cap limits</li>
     *   <li>Existing confirmed placements</li>
     * </ul>
     *
     * @param appliedOn the date when the application was submitted
     * @param student the student submitting the application
     * @param internship the internship being applied to
     * @param appReadPort the read port used to check existing applications and placements
     * @throws NullPointerException if any required parameter is null
     * @throws IllegalStateException if any domain validation rule is violated
     */
    public InternshipApplication(LocalDate appliedOn,
                                 Student student,
                                 Internship internship,
                                 Student.AppReadPort appReadPort) {
        this.applicationId = String.format("APP%04d", idCounter++);
        this.appliedOn = Objects.requireNonNull(appliedOn, "appliedOn");
        this.student = Objects.requireNonNull(student, "student");
        this.internship = Objects.requireNonNull(internship, "internship");
        this.internshipId = internship.getInternshipId();  // CHANGED: capture ID
        this.status = ApplicationStatus.PENDING;
        this.studentAccepted = false;

        // Creation-time domain checks (require internship object present at creation)
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
     * Creates a new internship application dated with the current date.
     *
     * @param student the student submitting the application
     * @param internship the internship being applied to
     * @param appReadPort the read port used to check existing applications and placements
     * @throws NullPointerException if any required parameter is null
     * @throws IllegalStateException if any domain validation rule is violated
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
     * @return the application ID in format "APP####"
     */
    public String getApplicationId() { return applicationId; }

    /**
     * Returns the date when the application was submitted.
     *
     * @return the application date
     */
    public LocalDate getAppliedOn() { return appliedOn; }

    /**
     * Returns the student who submitted this application.
     *
     * @return the student entity
     */
    public Student getStudent() { return student; }

    /**
     * Returns the internship ID associated with this application.
     * This provides a durable link even when the internship object is not attached.
     *
     * @return the internship ID, or null if not available
     */
    public String getInternshipId() {
        if (internshipId != null) return internshipId;
        return (internship != null ? internship.getInternshipId() : null);
    }

    /**
     * Returns the attached internship object.
     * Note: This may be null if not yet re-attached after loading from persistence.
     *
     * @return the internship entity, or null if not attached
     */
    public Internship getInternship() { return internship; }

    /**
     * Returns the current application status.
     *
     * @return the application status
     */
    public ApplicationStatus getStatus() { return status; }

    /**
     * Checks if the student has confirmed acceptance of this offer.
     *
     * @return true if student has accepted the offer, false otherwise
     */
    public boolean isStudentAccepted() { return studentAccepted; }

    /**
     * Checks if this application can be accepted by the student.
     * An application can be accepted when it's SUCCESSFUL and not already accepted.
     *
     * @return true if the student can accept this offer, false otherwise
     */
    public boolean canAccept() {
        return status == ApplicationStatus.SUCCESSFUL && !studentAccepted;
    }

    /**
     * Checks if this application counts toward the student's active application cap.
     * Applications count toward the cap when they are PENDING or SUCCESSFUL but not yet accepted.
     *
     * @return true if this application counts toward the cap, false otherwise
     */
    public boolean isActiveTowardCap() {
        return (status == ApplicationStatus.PENDING) ||
               (status == ApplicationStatus.SUCCESSFUL && !studentAccepted);
    }

    // ---------- Transitions (rep/staff actions) ----------

    /**
     * Marks the application as SUCCESSFUL (offer made to student).
     * Only PENDING applications can be marked as successful.
     *
     * @throws IllegalStateException if the application is not in PENDING status
     */
    public void markSuccessful() {
        ensureStatus(ApplicationStatus.PENDING, "Only PENDING can be marked SUCCESSFUL");
        this.status = ApplicationStatus.SUCCESSFUL;
    }

    /**
     * Marks the application as UNSUCCESSFUL (rejected by company).
     * Only PENDING applications can be marked as unsuccessful.
     * Cannot reject after student has accepted the offer.
     *
     * @throws IllegalStateException if the application is not in PENDING status or student has accepted
     */
    public void markUnsuccessful() {
        if (status == ApplicationStatus.SUCCESSFUL && studentAccepted) {
            throw new IllegalStateException("Cannot reject after student accepted");
        }
        ensureStatus(ApplicationStatus.PENDING, "Only PENDING can be marked UNSUCCESSFUL");
        this.status = ApplicationStatus.UNSUCCESSFUL;
    }

    /**
     * Marks the application as WITHDRAWN.
     * This can be called by the student or as part of an approved withdrawal request.
     */
    public void markWithdrawn() {
        // Keeps semantics the same but simpler
        this.status = ApplicationStatus.WITHDRAWN;
    }

    // ---------- Transitions (student actions) ----------

    /**
     * Confirms student acceptance of the internship offer.
     * Reserves a slot in the internship and marks the application as accepted.
     *
     * @param appReadPort the read port used to validate student placement status
     * @throws IllegalStateException if application cannot be accepted, student has existing placement,
     *                              or internship details are not attached
     */
    public void confirmAcceptance(Student.AppReadPort appReadPort) {
        if (!canAccept()) throw new IllegalStateException("Cannot accept in status " + status);
        if (student.hasConfirmedPlacement(appReadPort)) {
            throw new IllegalStateException("Student already has a confirmed placement");
        }
        // CHANGED: guard internship presence (must be attached by controller)
        if (internship == null) {
            throw new IllegalStateException("Internship details not attached; cannot confirm acceptance");
        }
        internship.incrementConfirmedSlots();
        this.studentAccepted = true;
    }

    /**
     * Revokes a previously confirmed acceptance after an approved withdrawal.
     * Releases the reserved slot in the internship.
     *
     * @throws IllegalStateException if no acceptance to revoke or internship details not attached
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
     * Sets the application ID (used by repository when loading persisted data).
     *
     * @param applicationId the external application ID
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Sets the internship ID (used when loading from persistence before re-attaching the object).
     *
     * @param internshipId the internship ID string
     */
    public void setInternshipId(String internshipId) {
        this.internshipId = internshipId;
    }

    /**
     * Re-attaches the Internship object after repository loading.
     * This method synchronizes the internship ID with the attached object.
     *
     * @param internship the internship instance to attach (or null to detach)
     */
    public void setInternship(Internship internship) {
        this.internship = internship;
        if (internship != null) {
            this.internshipId = internship.getInternshipId(); // keep in sync
        }
    }

    // ---------- Helpers ----------

    /**
     * Ensures the application is in the expected status.
     *
     * @param expected the expected application status
     * @param msg the error message prefix
     * @throws IllegalStateException if the current status doesn't match the expected status
     */
    private void ensureStatus(ApplicationStatus expected, String msg) {
        if (this.status != expected) throw new IllegalStateException(msg + " (current=" + this.status + ")");
    }

//    // ---------- Identity & debug ----------
//
//    /**
//     * Compares this application with another object for equality based on application ID.
//     *
//     * @param o the object to compare with
//     * @return true if the objects are equal, false otherwise
//     */
   @Override
   public boolean equals(Object o) {
       if (this == o) return true;
       if (!(o instanceof InternshipApplication)) return false;
       return applicationId.equals(((InternshipApplication) o).applicationId);
   }
//
//    /**
//     * Returns the hash code for this application based on the application ID.
//     *
//     * @return the hash code
//     */
//    @Override
   public int hashCode() { return applicationId.hashCode(); }
//
//    /**
//     * Returns a string representation of this application.
//     *
//     * @return formatted string with key application details
//     */
   @Override
   public String toString() {
       return "Application{" +
              "id='" + applicationId + '\'' +
              ", student=" + student.getUserId() +
              ", internshipId=" + getInternshipId() +
              ", status=" + status +
              ", accepted=" + studentAccepted +
              '}';
   }
}