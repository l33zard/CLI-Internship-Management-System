package entity;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a Career Center Staff member who manages internship programs and company relationships.
 * 
 * <p>This entity is responsible for administrative oversight of the internship ecosystem,
 * including approving company representatives, internship postings, and processing withdrawal requests.
 * All operations are pure domain logic without persistence or UI concerns.
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li>Approve/reject company representative registrations</li>
 *   <li>Approve/reject internship postings</li>
 *   <li>Process withdrawal requests (approve/reject)</li>
 *   <li>Generate filtered reports of internships and applications</li>
 * </ul>
 *
 */
public class CareerCenterStaff extends User {
    private String department;
    private String email;
    private String role;

    /**
     * Constructs a new Career Center Staff member with the specified details.
     *
     * @param userId the unique identifier for the staff member
     * @param name the full name of the staff member
     * @param role the staff role (e.g., "Officer", "Coordinator")
     * @param department the department the staff member belongs to
     * @param email the contact email address for the staff member
     * @throws IllegalArgumentException if any required field is null or blank
     */
    public CareerCenterStaff(String userId, String name, String role, String department, String email) {
        super(userId, name);
        this.role = requireText(role, "role");
        this.department = requireText(department, "department");
        this.email = requireText(email, "email");
    }

    /**
     * Returns the role of this staff member.
     *
     * @return the staff role (e.g., "Officer", "Coordinator")
     */
    public String getRole() { return role; }

    /**
     * Returns the department this staff member belongs to.
     *
     * @return the staff department
     */
    public String getDepartment() { return department; }

    /**
     * Returns the contact email address for this staff member.
     *
     * @return the staff email address
     */
    public String getEmail() { return email; }

    // ---------------- Core Capabilities ----------------

    /**
     * Approves a company representative account for system access.
     * This method changes the representative's status to approved, allowing them
     * to post internships and manage company information.
     *
     * @param rep the company representative to approve
     * @throws NullPointerException if the representative is null
     * @see CompanyRep#approve()
     */
    public void approveCompanyRep(CompanyRep rep) {
        Objects.requireNonNull(rep, "rep");
        rep.approve();
    }

    /**
     * Rejects a company representative account with a specified reason.
     * This method prevents the representative from accessing the system
     * and records the rejection reason for audit purposes.
     *
     * @param rep the company representative to reject
     * @param reason the reason for rejection
     * @throws NullPointerException if the representative is null
     * @see CompanyRep#reject(String)
     */
    public void rejectCompanyRep(CompanyRep rep, String reason) {
        Objects.requireNonNull(rep, "rep");
        rep.reject(reason);
    }

    /**
     * Approves an internship posting and makes it visible to students.
     * This method both approves the internship and sets it to visible status,
     * allowing students to view and apply for the position.
     *
     * @param internship the internship posting to approve
     * @throws NullPointerException if the internship is null
     * @see Internship#approve()
     * @see Internship#setVisible(boolean)
     */
    public void approveInternship(Internship internship) {
        Objects.requireNonNull(internship, "internship");
        internship.approve();       // uses its own guard to prevent double approval
        internship.setVisible(true); // typically turned on right away
    }

    /**
     * Rejects an internship posting.
     * This method changes the internship status to rejected and typically
     * makes it invisible to students.
     *
     * @param internship the internship posting to reject
     * @throws NullPointerException if the internship is null
     * @see Internship#reject()
     */
    public void rejectInternship(Internship internship) {
        Objects.requireNonNull(internship, "internship");
        internship.reject();
    }

    /**
     * Processes a withdrawal request by either approving or rejecting it.
     * This method handles student requests to withdraw from internships and
     * records staff decision notes for audit purposes.
     *
     * @param req the withdrawal request to process
     * @param approve true to approve the withdrawal, false to reject it
     * @param note the staff note explaining the decision
     * @throws NullPointerException if the withdrawal request is null
     * @see WithdrawalRequest#approve(CareerCenterStaff, String)
     * @see WithdrawalRequest#reject(CareerCenterStaff, String)
     */
    public void approveWithdrawal(WithdrawalRequest req, boolean approve, String note) {
        Objects.requireNonNull(req, "req");
        if (approve) req.approve(this, note);
        else req.reject(this, note);
    }

    /**
     * Filters internships based on multiple criteria for reporting purposes.
     * This method provides a flexible way to generate customized reports by
     * applying optional filters for status, major, level, and company.
     *
     * @param all the complete list of internships to filter
     * @param status the internship status to filter by (optional, null to ignore)
     * @param major the preferred major to filter by (optional, null to ignore)
     * @param level the internship level to filter by (optional, null to ignore)
     * @param companyName the company name to filter by (optional, null to ignore)
     * @return a filtered list of internships matching all specified criteria
     * <p>
     * Implementation Note: Returns an empty list if the input list is null
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
     * Validates that a string is not null or blank.
     *
     * @param s the string to validate
     * @param field the field name for error reporting
     * @return the trimmed string if valid
     * @throws IllegalArgumentException if the string is null or blank
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