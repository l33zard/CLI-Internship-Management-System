package entity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a company representative in the Internship Management System.
 * 
 * <p>Company representatives are users from companies who can post internship
 * opportunities and manage applications. They must be approved by career center
 * staff before gaining access to system features.</p>
 * 
 * <h2>Registration and Approval</h2>
 * <p>Company representatives go through an approval workflow:</p>
 * <ol>
 *   <li>Representative registers with company details</li>
 *   <li>Account created in unapproved state (approved = false)</li>
 *   <li>Career center staff reviews and approves or rejects</li>
 *   <li>If approved, representative can login and post internships</li>
 *   <li>If rejected, representative is notified with reason</li>
 * </ol>
 * 
 * <h2>Posting Limits</h2>
 * <p>To prevent system abuse, each representative is limited to:</p>
 * <ul>
 *   <li>Maximum {@value #MAX_POSTINGS} active internship postings</li>
 *   <li>Active postings include PENDING, APPROVED, and FILLED internships</li>
 *   <li>REJECTED internships do not count toward limit</li>
 * </ul>
 * 
 * <h2>Responsibilities</h2>
 * <p>Approved company representatives can:</p>
 * <ul>
 *   <li>Create new internship postings (subject to limit)</li>
 *   <li>Toggle visibility of their approved internships</li>
 *   <li>Review and approve/reject student applications</li>
 *   <li>Close postings when no longer accepting applications</li>
 * </ul>
 * 
 * <h2>Ownership Rules</h2>
 * <p>Representatives can only manage internships for their own company:</p>
 * <ul>
 *   <li>Company name matching is case-insensitive</li>
 *   <li>Attempting to manage other companies' postings throws exception</li>
 *   <li>Ensures data integrity and prevents unauthorized access</li>
 * </ul>
 * 
 * @author SC2002 Assignment Team
 * @version 1.0
 * @since 1.0
 * @see User
 * @see Internship
 * @see InternshipApplication
 */
public class CompanyRep extends User {

    /**
     * Maximum number of active internship postings allowed per representative.
     * 
     * <p>This limit prevents system abuse and ensures quality over quantity
     * in internship postings. Active postings include those in PENDING,
     * APPROVED, and FILLED status.</p>
     */
    public static final int MAX_POSTINGS = 5;

    /**
     * Name of the company this representative works for.
     * Used for ownership validation when managing internships.
     */
    private String companyName;
    
    /**
     * Department within the company.
     * Example: "Human Resources", "Engineering", "IT"
     */
    private String department;
    
    /**
     * Job position/title of the representative.
     * Example: "HR Manager", "Talent Acquisition Specialist"
     */
    private String position;
    
    /**
     * Corporate email address for communication.
     * Used as login identifier and for notifications.
     */
    private String email;

    /**
     * Approval status flag controlled by career center staff.
     * Must be true before the representative can use platform features.
     */
    private boolean approved;
    
    /**
     * Reason provided by staff if registration is rejected.
     * Empty string if never rejected or if subsequently approved.
     */
    private String rejectionReason;

    /**
     * Constructs a new CompanyRep with the specified details.
     * 
     * <p>The representative is created in unapproved state and must be
     * approved by career center staff before gaining access to system features.
     * All text fields are validated to ensure they are not null or blank.</p>
     * 
     * @param userId unique identifier for this user
     * @param name full name of the representative
     * @param companyName name of the company
     * @param department department within the company
     * @param position job title of the representative
     * @param email corporate email address
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
     * Returns the name of the company this representative works for.
     * 
     * @return the company name
     */
    public String getCompanyName() { return companyName; }
    
    /**
     * Returns the department within the company.
     * 
     * @return the department
     */
    public String getDepartment()  { return department; }
    
    /**
     * Returns the representative's job position or title.
     * 
     * @return the position
     */
    public String getPosition()    { return position; }
    
    /**
     * Returns the representative's corporate email address.
     * 
     * @return the email address
     */
    public String getEmail()       { return email; }
    
    /**
     * Checks if this representative has been approved by career center staff.
     * 
     * @return {@code true} if approved, {@code false} otherwise
     */
    public boolean isApproved()    { return approved; }
    
    /**
     * Checks if this representative's registration has been rejected.
     * 
     * <p>A representative is considered rejected if they are not approved
     * AND a rejection reason has been provided.</p>
     * 
     * @return {@code true} if rejected, {@code false} otherwise
     */
    public boolean isRejected()    { return !approved && rejectionReason != null && !rejectionReason.isEmpty(); }
    
    /**
     * Returns the reason provided by staff if registration was rejected.
     * 
     * @return the rejection reason, or empty string if never rejected
     */
    public String getRejectionReason() { return rejectionReason; }

    // ---------- Registration (called by staff service/controller) ----------
    
    /**
     * Approves this representative's registration.
     * 
     * <p>Called by career center staff to grant access to system features.
     * Clears any previous rejection reason.</p>
     */
    public void approve() {
        this.approved = true;
        this.rejectionReason = "";
    }

    /**
     * Rejects this representative's registration with a reason.
     * 
     * <p>Called by career center staff to deny access. The reason is stored
     * and can be communicated to the representative.</p>
     * 
     * @param reason explanation for rejection (can be null)
     */
    public void reject(String reason) {
        this.approved = false;
        this.rejectionReason = reason == null ? "" : reason.trim();
    }

    // ---------- Posting management ----------
    
    /**
     * Checks if this representative can create another internship posting.
     * 
     * <p>Representatives are limited to {@value #MAX_POSTINGS} active postings
     * to prevent system abuse and encourage quality postings.</p>
     * 
     * @param port port for counting current active postings
     * @return {@code true} if under the posting limit, {@code false} otherwise
     * @throws NullPointerException if port is null
     */
    public boolean canCreateAnotherPosting(RepPostingReadPort port) {
        Objects.requireNonNull(port, "port");
        return port.countActivePostingsForRep(getUserId()) < MAX_POSTINGS;
    }

    /**
     * Creates a new internship posting owned by this representative's company.
     * 
     * <p>This method validates that:</p>
     * <ul>
     *   <li>Representative is approved</li>
     *   <li>Posting cap has not been reached</li>
     * </ul>
     * 
     * <p>The internship is created in PENDING status and must be approved by
     * career center staff before becoming visible to students.</p>
     * 
     * @param title the internship position title
     * @param description detailed description of the role
     * @param level difficulty level determining student eligibility
     * @param preferredMajor preferred major for applicants
     * @param openDate first date students can apply
     * @param closeDate last date students can apply
     * @param maxSlots maximum number of students to accept
     * @param port port for checking posting count
     * @return the newly created Internship
     * @throws IllegalStateException if representative not approved or over posting cap
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
     * Sets the visibility of an internship owned by this representative's company.
     * 
     * <p>Only APPROVED internships can be made visible. This allows representatives
     * to control when their approved postings are shown to students.</p>
     * 
     * @param internship the internship to modify
     * @param visible {@code true} to show to students, {@code false} to hide
     * @throws IllegalArgumentException if internship is not owned by this company
     * @throws IllegalStateException if attempting to make non-approved internship visible
     */
    public void setInternshipVisibility(Internship internship, boolean visible) {
        ensureOwns(internship);
        internship.setVisible(visible);
    }

    /**
     * Closes an internship posting by making it non-visible.
     * 
     * <p>Optional helper method to hide an approved posting without changing
     * its approval status. Useful when no longer accepting applications but
     * wanting to preserve the posting record.</p>
     * 
     * @param internship the internship to close
     * @throws IllegalArgumentException if internship is not owned by this company
     */
    public void closePosting(Internship internship) {
        ensureOwns(internship);
        if (internship.getStatus() == InternshipStatus.APPROVED) {
            internship.setVisible(false);
        }
    }

    // ---------- Application decisions (for OWN postings) ----------
    
    /**
     * Approves a student application by marking it SUCCESSFUL (offering the position).
     * 
     * <p>Called when the representative wants to offer the internship to the student.
     * The student can then choose to accept or reject the offer.</p>
     * 
     * @param app the application to approve
     * @throws IllegalArgumentException if internship is not owned by this company
     * @throws IllegalStateException if application is not in PENDING status
     */
    public void approveApplication(InternshipApplication app) {
        ensureOwns(app.getInternship());
        app.markSuccessful();
    }

    /**
     * Rejects a student application by marking it UNSUCCESSFUL.
     * 
     * <p>Called when the representative decides not to offer the position
     * to the student.</p>
     * 
     * @param app the application to reject
     * @throws IllegalArgumentException if internship is not owned by this company
     * @throws IllegalStateException if application is not in PENDING status or
     *         if student has already accepted
     */
    public void rejectApplication(InternshipApplication app) {
        ensureOwns(app.getInternship());
        app.markUnsuccessful();
    }

    // ---------- Ownership & validation ----------
    
    /**
     * Ensures this representative owns the specified internship.
     * 
     * <p>Validates that the internship belongs to this representative's company
     * by performing case-insensitive company name comparison.</p>
     * 
     * @param internship the internship to validate
     * @throws NullPointerException if internship is null
     * @throws IllegalArgumentException if internship is not owned by this company
     */
    private void ensureOwns(Internship internship) {
        Objects.requireNonNull(internship, "internship");
        if (!this.companyName.equalsIgnoreCase(internship.getCompanyName())) {
            throw new IllegalArgumentException("Rep can only manage their own company's postings");
        }
    }

    /**
     * Validates that a text field is not null or blank.
     * 
     * <p>Internal helper method for constructor validation.</p>
     * 
     * @param s the string to validate
     * @param field the field name for error messages
     * @return the trimmed string
     * @throws IllegalArgumentException if string is null or blank
     */
    private static String requireText(String s, String field) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(field + " required");
        return s.trim();
    }

    // ---------- Ports ----------
    
    /**
     * Minimal read-port interface for querying posting counts.
     * 
     * <p>This port interface allows the domain layer to check posting limits
     * without depending on repository implementation details. Follows the
     * Dependency Inversion Principle.</p>
     * 
     * <p>Implementations should count internships with status PENDING, APPROVED,
     * or FILLED. REJECTED internships should not count toward the limit.</p>
     * 
     * @see CompanyRep#canCreateAnotherPosting(RepPostingReadPort)
     * @see CompanyRep#createInternship(String, String, InternshipLevel, String, LocalDate, LocalDate, int, RepPostingReadPort)
     */
    public interface RepPostingReadPort {
        /**
         * Counts the number of active postings for a company representative.
         * 
         * <p>Active postings include those with PENDING, APPROVED, or FILLED status.</p>
         * 
         * @param repId the representative's unique identifier
         * @return count of active postings
         */
        int countActivePostingsForRep(String repId);
    }
}