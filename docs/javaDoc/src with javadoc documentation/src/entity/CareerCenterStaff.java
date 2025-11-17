package entity;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a career center staff member with administrative and oversight responsibilities.
 * 
 * <p>Career center staff are university employees who manage the internship platform,
 * ensuring quality and appropriateness of opportunities. They serve as gatekeepers
 * for company registrations and internship postings.</p>
 * 
 * <h2>Core Responsibilities</h2>
 * <ul>
 *   <li><b>Company Representative Management:</b> Approve or reject company rep registrations</li>
 *   <li><b>Internship Approval:</b> Review and approve/reject internship postings</li>
 *   <li><b>Withdrawal Processing:</b> Handle student withdrawal requests from accepted placements</li>
 *   <li><b>Reporting:</b> Generate filtered reports on internships and applications</li>
 *   <li><b>System Oversight:</b> Monitor platform usage and maintain quality standards</li>
 * </ul>
 * 
 * <h2>Approval Workflows</h2>
 * 
 * <h3>Company Representative Registration</h3>
 * <ol>
 *   <li>Company rep submits registration with company details</li>
 *   <li>Staff reviews registration information</li>
 *   <li>Staff approves → Rep gains access to create postings</li>
 *   <li>Staff rejects → Rep notified with reason</li>
 * </ol>
 * 
 * <h3>Internship Posting Approval</h3>
 * <ol>
 *   <li>Company rep creates internship (PENDING status)</li>
 *   <li>Staff reviews posting for appropriateness and completeness</li>
 *   <li>Staff approves → Internship becomes visible to students</li>
 *   <li>Staff rejects → Rep can revise and resubmit</li>
 * </ol>
 * 
 * <h3>Withdrawal Request Processing</h3>
 * <ol>
 *   <li>Student with accepted placement requests withdrawal</li>
 *   <li>Staff reviews request and reason</li>
 *   <li>Staff approves → Placement freed, slot released</li>
 *   <li>Staff rejects → Student remains in placement</li>
 * </ol>
 * 
 * <h2>Domain Layer Purity</h2>
 * <p>This class contains only pure domain methods with no dependencies on:</p>
 * <ul>
 *   <li>Persistence layer (no database operations)</li>
 *   <li>UI layer (no display or input handling)</li>
 *   <li>External services (no email, notifications)</li>
 * </ul>
 * <p>All orchestration and side effects are handled by the controller layer.</p>
 * 
 * @author SC2002 Assignment Team
 * @version 1.0
 * @since 1.0
 * @see User
 * @see CompanyRep
 * @see Internship
 * @see WithdrawalRequest
 */
public class CareerCenterStaff extends User {
    /**
     * Department or unit within the career center.
     * Example: "Student Services", "Career Development"
     */
    private String department;
    
    /**
     * Official university email address for communication.
     */
    private String email;
    
    /**
     * Job role or position title within the career center.
     * Example: "Career Counselor", "Internship Coordinator", "Director"
     */
    private String role;

    /**
     * Constructs a new CareerCenterStaff member with the specified details.
     * 
     * <p>All text fields are validated to ensure they are not null or blank.
     * Staff members are immediately active upon creation (no approval workflow
     * needed for staff accounts).</p>
     * 
     * @param userId unique identifier for this staff member (NTU username)
     * @param name full name of the staff member
     * @param role job title or position (e.g., "Career Counselor")
     * @param department department within career center
     * @param email official university email address
     * @throws IllegalArgumentException if any required field is null or blank
     */
    public CareerCenterStaff(String userId, String name, String role, String department, String email) {
        super(userId, name);
        this.role = requireText(role, "role");
        this.department = requireText(department, "department");
        this.email = requireText(email, "email");
    }

    /**
     * Returns the staff member's job role or position title.
     * 
     * @return the role (e.g., "Career Counselor", "Internship Coordinator")
     */
    public String getRole() { return role; }
    
    /**
     * Returns the department within the career center.
     * 
     * @return the department
     */
    public String getDepartment() { return department; }
    
    /**
     * Returns the staff member's official email address.
     * 
     * @return the email address
     */
    public String getEmail() { return email; }

    // ---------------- Core Capabilities ----------------

    /**
     * Approves a company representative's registration, granting platform access.
     * 
     * <p>Once approved, the representative can:</p>
     * <ul>
     *   <li>Login to the system</li>
     *   <li>Create internship postings</li>
     *   <li>Review student applications</li>
     * </ul>
     * 
     * <p>This method delegates to the {@link CompanyRep#approve()} method,
     * following domain-driven design principles.</p>
     * 
     * @param rep the company representative to approve
     * @throws NullPointerException if rep is null
     * @see CompanyRep#approve()
     */
    public void approveCompanyRep(CompanyRep rep) {
        Objects.requireNonNull(rep, "rep");
        rep.approve();
    }

    /**
     * Rejects a company representative's registration with a reason.
     * 
     * <p>The representative is notified of the rejection and provided with
     * the reason. They may address the concerns and reapply.</p>
     * 
     * @param rep the company representative to reject
     * @param reason explanation for rejection (can be null)
     * @throws NullPointerException if rep is null
     * @see CompanyRep#reject(String)
     */
    public void rejectCompanyRep(CompanyRep rep, String reason) {
        Objects.requireNonNull(rep, "rep");
        rep.reject(reason);
    }

    /**
     * Approves an internship posting and makes it visible to students.
     * 
     * <p>This method performs two operations:</p>
     * <ol>
     *   <li>Changes status from PENDING to APPROVED</li>
     *   <li>Sets visibility to true, making it searchable by students</li>
     * </ol>
     * 
     * <p>Company representatives can later toggle visibility on/off for
     * approved postings as needed.</p>
     * 
     * @param internship the internship to approve
     * @throws NullPointerException if internship is null
     * @see Internship#approve()
     * @see Internship#setVisible(boolean)
     */
    public void approveInternship(Internship internship) {
        Objects.requireNonNull(internship, "internship");
        internship.approve();
        internship.setVisible(true);
    }

    /**
     * Rejects an internship posting, preventing it from being visible to students.
     * 
     * <p>The company representative is notified and can revise the posting
     * to address staff concerns. Rejected internships automatically become
     * non-visible.</p>
     * 
     * @param internship the internship to reject
     * @throws NullPointerException if internship is null
     * @see Internship#reject()
     */
    public void rejectInternship(Internship internship) {
        Objects.requireNonNull(internship, "internship");
        internship.reject();
    }

    /**
     * Processes a student withdrawal request by approving or rejecting it.
     * 
     * <p>This is a convenience method that delegates to the withdrawal request's
     * approve or reject methods based on the staff's decision.</p>
     * 
     * <p><b>If approved:</b></p>
     * <ul>
     *   <li>Student is released from their accepted placement</li>
     *   <li>Internship slot is freed for other students</li>
     *   <li>Application status is updated appropriately</li>
     * </ul>
     * 
     * <p><b>If rejected:</b></p>
     * <ul>
     *   <li>Student remains in their accepted placement</li>
     *   <li>No changes to internship or application</li>
     * </ul>
     * 
     * @param req the withdrawal request to process
     * @param approve {@code true} to approve, {@code false} to reject
     * @param note optional staff note explaining the decision
     * @throws NullPointerException if req is null
     * @see WithdrawalRequest#approve(CareerCenterStaff, String)
     * @see WithdrawalRequest#reject(CareerCenterStaff, String)
     */
    public void approveWithdrawal(WithdrawalRequest req, boolean approve, String note) {
        Objects.requireNonNull(req, "req");
        if (approve) req.approve(this, note);
        else req.reject(this, note);
    }

    /**
     * Filters a list of internships based on multiple criteria for reporting.
     * 
     * <p>This method provides flexible filtering for staff to generate reports
     * and analyze internship data. All filter parameters are optional (can be null)
     * to allow for partial filtering.</p>
     * 
     * <p><b>Filter Criteria:</b></p>
     * <ul>
     *   <li><b>status:</b> Filter by approval status (PENDING, APPROVED, etc.)</li>
     *   <li><b>major:</b> Filter by preferred major (case-insensitive)</li>
     *   <li><b>level:</b> Filter by difficulty level (BASIC, INTERMEDIATE, ADVANCED)</li>
     *   <li><b>companyName:</b> Filter by company name (case-insensitive)</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>
     * // Get all PENDING internships
     * filterInternships(all, InternshipStatus.PENDING, null, null, null)
     * 
     * // Get BASIC level internships for CSC majors
     * filterInternships(all, null, "CSC", InternshipLevel.BASIC, null)
     * 
     * // Get all internships from specific company
     * filterInternships(all, null, null, null, "TechCorp")
     * </pre>
     * 
     * @param all the complete list of internships to filter
     * @param status filter by status (null to skip this filter)
     * @param major filter by preferred major (null to skip this filter)
     * @param level filter by level (null to skip this filter)
     * @param companyName filter by company name (null to skip this filter)
     * @return filtered list of internships matching all provided criteria
     */
    public List<Internship> filterInternships(
            List<Internship> all,
            InternshipStatus status,
            String major,
            InternshipLevel level,
            String companyName
    ) {
        if (all == null) return List.of();
        return all.stream()
                .filter(i -> (status == null || i.getStatus() == status))
                .filter(i -> (major == null || i.getPreferredMajor().equalsIgnoreCase(major)))
                .filter(i -> (level == null || i.getLevel() == level))
                .filter(i -> (companyName == null || i.getCompanyName().equalsIgnoreCase(companyName)))
                .collect(Collectors.toList());
    }

    // ---------------- Helpers ----------------

    /**
     * Validates that a text field is not null or blank.
     * 
     * <p>Internal helper method for constructor validation.
     * Trims whitespace from valid strings.</p>
     * 
     * @param s the string to validate
     * @param field the field name for error messages
     * @return the trimmed string
     * @throws IllegalArgumentException if string is null or blank
     */
    private static String requireText(String s, String field) {
        if (s == null || s.isBlank())
            throw new IllegalArgumentException(field + " required");
        return s.trim();
    }


//    @Override
//    public String toString() {
//        return "CareerCenterStaff{" +
//                "id='" + getUserId() + '\'' +
//                ", name='" + getName() + '\'' +
//                ", role='" + role + '\'' +
//                ", dept='" + department + '\'' +
//                '}';
//    }
}