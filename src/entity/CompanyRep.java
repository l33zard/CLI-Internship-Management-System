package entity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a Company Representative who manages internship postings and applications for their company.
 * 
 * <p>This entity is responsible for creating and managing internship opportunities on behalf of their company.
 * Company representatives must be approved by career center staff before they can create postings or manage applications.
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li>Create and manage internship postings (subject to approval and posting limits)</li>
 *   <li>Set visibility of approved internship postings</li>
 *   <li>Review and make decisions on internship applications</li>
 *   <li>Close/hide internship postings when needed</li>
 * </ul>
 *
 * <p><b>Posting Limits:</b> Each representative is limited to {@value #MAX_POSTINGS} active postings.
 *
 */
public class CompanyRep extends User {

    /**
     * Maximum number of active postings allowed per company representative.
     */
    public static final int MAX_POSTINGS = 5;

    private String companyName;
    private String department;
    private String position;
    private String email;

    // Registration status (controlled by staff actions)
    private boolean approved;             // must be true before using platform
    private String rejectionReason;       // last rejection reason (optional)

    /**
     * Creates a new Company Representative account (initially not approved).
     * The representative must be approved by career center staff before performing any platform operations.
     *
     * @param userId the unique identifier for the representative
     * @param name the full name of the representative
     * @param companyName the company the representative belongs to
     * @param department the department within the company
     * @param position the representative's job position/title
     * @param email the contact email address
     * @throws IllegalArgumentException if any required field is null or blank
     */
    public CompanyRep(String userId, String name,
                      String companyName, String department, String position, String email) {
        super(userId, name);
        this.companyName = requireText(companyName, "companyName");
        this.department  = requireText(department, "department");
        this.position    = requireText(position, "position");
        this.email       = requireText(email, "email");
        this.approved    = false;
        this.rejectionReason = "";
    }

    // ---------- Queries ----------

    /**
     * Returns the company name this representative works for.
     *
     * @return the company name
     */
    public String getCompanyName() { return companyName; }

    /**
     * Returns the department within the company.
     *
     * @return the department name
     */
    public String getDepartment()  { return department; }

    /**
     * Returns the representative's job position/title.
     *
     * @return the position title
     */
    public String getPosition()    { return position; }

    /**
     * Returns the contact email address.
     *
     * @return the email address
     */
    public String getEmail()       { return email; }

    /**
     * Checks if this representative has been approved by career center staff.
     *
     * @return true if approved, false otherwise
     */
    public boolean isApproved()    { return approved; }

    /**
     * Checks if this representative has been rejected by career center staff.
     *
     * @return true if rejected (not approved and has a rejection reason), false otherwise
     */
    public boolean isRejected()    { return !approved && rejectionReason != null && !rejectionReason.isEmpty(); }

    /**
     * Returns the reason for rejection, if any.
     *
     * @return the rejection reason, or empty string if not rejected
     */
    public String getRejectionReason() { return rejectionReason; }

    // ---------- Registration (called by staff service/controller) ----------

    /**
     * Approves this company representative for platform access.
     * Clears any previous rejection reason and enables all representative capabilities.
     */
    public void approve() {
        this.approved = true;
        this.rejectionReason = "";
    }

    /**
     * Rejects this company representative with a specified reason.
     * Prevents the representative from using platform capabilities until approved.
     *
     * @param reason the reason for rejection
     */
    public void reject(String reason) {
        this.approved = false;
        this.rejectionReason = reason == null ? "" : reason.trim();
    }

    // ---------- Posting management ----------

    /**
     * Checks whether the representative has capacity to create another posting.
     * Verifies that the current number of active postings is below the maximum limit.
     *
     * @param port the read port used to count existing active postings
     * @return true if under the posting cap, false otherwise
     * @throws NullPointerException if the port is null
     */
    public boolean canCreateAnotherPosting(RepPostingReadPort port) {
        Objects.requireNonNull(port, "port");
        return port.countActivePostingsForRep(getUserId()) < MAX_POSTINGS;
    }

    /**
     * Creates a new internship posting owned by this representative's company.
     * 
     * <p><b>Prerequisites:</b>
     * <ul>
     *   <li>Representative must be approved</li>
     *   <li>Representative must be under the posting cap</li>
     * </ul>
     *
     * @param title the internship title
     * @param description the internship description
     * @param level the internship level
     * @param preferredMajor the preferred major for applicants
     * @param openDate the date when applications open
     * @param closeDate the date when applications close
     * @param maxSlots the maximum number of confirmed slots available
     * @param port the read port used to verify posting capacity
     * @return a new Internship instance (initially not approved or visible)
     * @throws IllegalStateException if representative is not approved or has reached posting cap
     * @throws NullPointerException if any required parameter is null
     */
    public Internship createInternship(
            String title, String description, InternshipLevel level,
            String preferredMajor, LocalDate openDate, LocalDate closeDate,
            int maxSlots, RepPostingReadPort port) {

        if (!isApproved()) throw new IllegalStateException("Rep not approved");
        if (!canCreateAnotherPosting(port))
            throw new IllegalStateException("Posting cap reached (" + MAX_POSTINGS + ")");

        return new Internship(
                title, description, level, preferredMajor,
                openDate, closeDate, this.companyName, maxSlots
        );
    }

    /**
     * Sets visibility for an internship posting owned by this representative.
     * Only approved internships can be made visible to students.
     *
     * @param internship the internship to modify visibility for
     * @param visible true to make visible, false to hide
     * @throws IllegalArgumentException if the internship does not belong to this representative's company
     * @throws NullPointerException if the internship is null
     */
    public void setInternshipVisibility(Internship internship, boolean visible) {
        ensureOwns(internship);
        internship.setVisible(visible);
    }

    /**
     * Closes and hides a posting owned by this representative without changing its approval status.
     * This is typically used when an internship position is filled or no longer available.
     *
     * @param internship the posting to close
     * @throws IllegalArgumentException if the internship does not belong to this representative's company
     * @throws NullPointerException if the internship is null
     */
    public void closePosting(Internship internship) {
        ensureOwns(internship);
        if (internship.getStatus() == InternshipStatus.APPROVED) {
            internship.setVisible(false);
        }
    }

    // ---------- Application decisions (for OWN postings) ----------

    /**
     * Approves a pending internship application (offers the position).
     * Changes the application status to SUCCESSFUL.
     *
     * @param app the application to approve
     * @throws IllegalArgumentException if the application's internship does not belong to this representative's company
     * @throws NullPointerException if the application is null
     */
    public void approveApplication(InternshipApplication app) {
        ensureOwns(app.getInternship());
        app.markSuccessful();
    }

    /**
     * Rejects a pending internship application.
     * Changes the application status to UNSUCCESSFUL.
     *
     * @param app the application to reject
     * @throws IllegalArgumentException if the application's internship does not belong to this representative's company
     * @throws NullPointerException if the application is null
     */
    public void rejectApplication(InternshipApplication app) {
        ensureOwns(app.getInternship());
        app.markUnsuccessful();
    }

    // ---------- Ownership & validation ----------

    /**
     * Ensures that the given internship belongs to this representative's company.
     *
     * @param internship the internship to validate ownership for
     * @throws IllegalArgumentException if the internship does not belong to this representative's company
     * @throws NullPointerException if the internship is null
     */
    private void ensureOwns(Internship internship) {
        Objects.requireNonNull(internship, "internship");
        if (!this.companyName.equalsIgnoreCase(internship.getCompanyName())) {
            throw new IllegalArgumentException("Rep can only manage their own company's postings");
        }
    }

    /**
     * Validates that a string is not null or blank.
     *
     * @param s the string to validate
     * @param field the field name for error reporting
     * @return the trimmed string if valid
     * @throws IllegalArgumentException if the string is null or blank
     */
    private static String requireText(String s, String field) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(field + " required");
        return s.trim();
    }

//    // ---------- Identity & debug ----------
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof CompanyRep)) return false;
//        CompanyRep that = (CompanyRep) o;
//        return getUserId().equals(that.getUserId());
//    }
//
//    @Override
//    public int hashCode() { return getUserId().hashCode(); }
//
//    @Override
//    public String toString() {
//        return "CompanyRep{" +
//                "id='" + getUserId() + '\'' +
//                ", name='" + getName() + '\'' +
//                ", company='" + companyName + '\'' +
//                ", approved=" + approved +
//                '}';
//    }

    // ---------- Ports ----------

    /**
     * Read-port interface for querying posting information from persistence.
     * This allows the domain to query how many active postings a representative has
     * without depending directly on persistence details.
     */
    public interface RepPostingReadPort {
        
        /**
         * Counts the number of active postings for a given representative.
         *
         * @param repId the representative's user ID
         * @return the number of active postings for the representative
         */
        int countActivePostingsForRep(String repId);
    }
}