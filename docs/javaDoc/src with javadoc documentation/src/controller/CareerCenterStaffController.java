package controller;

import database.CompanyRepRepository;
import database.InternshipRepository;
import database.ApplicationRepository;
import database.WithdrawalRequestRepository;
import database.CareerCenterStaffRepository;
import entity.CompanyRep;
import entity.Internship;
import entity.InternshipStatus;
import entity.InternshipLevel;
import entity.WithdrawalRequest;
import entity.WithdrawalRequestStatus;
import entity.CareerCenterStaff;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for managing career center staff operations and administrative functions.
 * 
 * <p>This controller handles the business logic for career center staff to:
 * <ul>
 *   <li>Review and approve/reject internship postings</li>
 *   <li>Review and approve/reject company representative registrations</li>
 *   <li>Process student withdrawal requests</li>
 *   <li>Filter and monitor all internship postings</li>
 *   <li>Manage the overall internship system</li>
 * </ul>
 * 
 * <p><strong>Key Responsibilities:</strong>
 * <ul>
 *   <li><strong>Quality Control:</strong> Ensure all internship postings meet standards before publication</li>
 *   <li><strong>Access Management:</strong> Control which companies can post internships</li>
 *   <li><strong>Student Protection:</strong> Review and approve legitimate withdrawal requests</li>
 *   <li><strong>System Oversight:</strong> Monitor all activities and maintain system integrity</li>
 * </ul>
 * 
 * @see CareerCenterStaff
 * @see Internship
 * @see CompanyRep
 * @see WithdrawalRequest
 * @see BaseController
 */
public class CareerCenterStaffController extends BaseController {

    /**
     * Constructs a new CareerCenterStaffController with the specified repositories.
     * 
     * @param companyRepRepo the repository for managing company representatives
     * @param internshipRepo the repository for managing internship postings
     * @param applicationRepo the repository for managing student applications
     * @param withdrawalsRepo the repository for managing withdrawal requests
     * @param staffRepo the repository for managing career center staff members
     */
    public CareerCenterStaffController(CompanyRepRepository companyRepRepo,
                                       InternshipRepository internshipRepo,
                                       ApplicationRepository applicationRepo,
                                       WithdrawalRequestRepository withdrawalsRepo,
                                       CareerCenterStaffRepository staffRepo) {
        super(null, companyRepRepo, staffRepo, internshipRepo, applicationRepo, withdrawalsRepo);
    }

    /**
     * Retrieves all internship postings with PENDING status awaiting staff review.
     * 
     * <p>Returns internships that have been submitted by company representatives
     * but have not yet been approved or rejected by career center staff.
     * 
     * @return a list of pending internship postings
     */
    public List<Internship> listPendingInternships() {
        return internshipRepo.findByStatus(InternshipStatus.PENDING);
    }

    /**
     * Retrieves all company representative registrations that have not been approved or rejected.
     * 
     * <p>Returns representatives who have registered but are awaiting staff decision.
     * These accounts cannot post internships until approved.
     * 
     * @return a list of unapproved company representatives
     */
    public List<CompanyRep> listUnapprovedReps() {
        List<CompanyRep> out = new ArrayList<>();
        for (CompanyRep r : repRepo.findAll()) {
            if (!r.isApproved() && !r.isRejected()) {
                out.add(r);
            }
        }
        return out;
    }
    
    /**
     * Retrieves all company representatives in the system.
     * 
     * <p>Returns all registered company representatives regardless of their
     * approval status (approved, rejected, or pending).
     * 
     * @return a list of all company representatives
     */
    public List<CompanyRep> listAllReps() {
        return repRepo.findAll();
    }

    /**
     * Retrieves all withdrawal requests with PENDING status awaiting staff review.
     * 
     * <p>Returns student withdrawal requests that have been submitted but have
     * not yet been approved or rejected by career center staff.
     * 
     * @return a list of pending withdrawal requests
     */
    public List<WithdrawalRequest> listPendingWithdrawals() {
        return withdrawalRepo.findPending();
    }
    
    /**
     * Retrieves all internship postings in the system.
     * 
     * <p>Returns all internships regardless of their status (pending, approved,
     * rejected, or filled), providing complete system oversight.
     * 
     * @return a list of all internship postings
     */
    public List<Internship> listAllInternships() {
        return internshipRepo.findAll();
    }

    /**
     * Approves an internship posting and optionally makes it visible to students.
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Validates the internship exists and is in PENDING status</li>
     *   <li>Marks the internship as APPROVED</li>
     *   <li>Sets the visibility based on the makeVisible parameter</li>
     *   <li>Saves the updated internship</li>
     * </ol>
     * 
     * <p><strong>Business Rules:</strong>
     * <ul>
     *   <li>Only PENDING internships can be approved</li>
     *   <li>Approved internships can be immediately visible or hidden initially</li>
     *   <li>Once approved, the internship cannot be edited by the company representative</li>
     * </ul>
     * 
     * @param staffId the unique identifier of the staff member performing the approval
     * @param internshipId the unique identifier of the internship to approve
     * @param makeVisible true to make the internship immediately visible to students, false to keep it hidden
     * @throws IllegalArgumentException if the internship is not found
     * @throws IllegalStateException if the internship is not in PENDING status
     */
    public void approveInternship(String staffId, String internshipId, boolean makeVisible) {
        
        Internship i = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));
        
        if (i.getStatus() != InternshipStatus.PENDING) {
            throw new IllegalStateException("Only pending internships can be approved.");
        }
        
        i.approve();
        i.setVisible(makeVisible);
        internshipRepo.save(i);
    }

    /**
     * Rejects an internship posting.
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Validates the internship exists and is in PENDING status</li>
     *   <li>Marks the internship as REJECTED</li>
     *   <li>Saves the updated internship</li>
     * </ol>
     * 
     * <p><strong>Business Rules:</strong>
     * <ul>
     *   <li>Only PENDING internships can be rejected</li>
     *   <li>Rejected internships are not visible to students</li>
     *   <li>Company representatives can edit or delete rejected postings</li>
     * </ul>
     * 
     * <p><strong>Common Rejection Reasons:</strong>
     * <ul>
     *   <li>Incomplete or unclear job description</li>
     *   <li>Inappropriate content or requirements</li>
     *   <li>Duplicate posting</li>
     *   <li>Company not verified or suspicious</li>
     * </ul>
     * 
     * @param staffId the unique identifier of the staff member performing the rejection
     * @param internshipId the unique identifier of the internship to reject
     * @throws IllegalArgumentException if the internship is not found
     * @throws IllegalStateException if the internship is not in PENDING status
     */
    public void rejectInternship(String staffId, String internshipId) {
        
        Internship i = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));
        
        if (i.getStatus() != InternshipStatus.PENDING) {
            throw new IllegalStateException("Only pending internships can be rejected.");
        }
        
        i.reject();
        internshipRepo.save(i);
    }

    /**
     * Approves a company representative's registration.
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Validates the company representative exists</li>
     *   <li>Checks that the representative is not already approved</li>
     *   <li>Marks the representative as approved</li>
     *   <li>Saves the updated representative record</li>
     * </ol>
     * 
     * <p><strong>Effects of Approval:</strong>
     * <ul>
     *   <li>Representative can log in to the system</li>
     *   <li>Representative can post internship opportunities</li>
     *   <li>Representative can manage their company's postings</li>
     *   <li>Representative can review student applications</li>
     * </ul>
     * 
     * @param staffId the unique identifier of the staff member performing the approval
     * @param repEmail the email address of the company representative to approve
     * @throws IllegalArgumentException if the company representative is not found
     * @throws IllegalStateException if the representative is already approved
     */
    public void approveCompanyRep(String staffId, String repEmail) {
        
        CompanyRep rep = repRepo.findByEmail(repEmail)
                .orElseThrow(() -> new IllegalArgumentException("Company representative not found: " + repEmail));
        
        if (rep.isApproved()) {
            throw new IllegalStateException("Company representative is already approved.");
        }
        
        rep.approve();
        repRepo.save(rep);
    }

    /**
     * Rejects a company representative's registration with a reason.
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Validates the company representative exists</li>
     *   <li>Checks that the representative is not already rejected</li>
     *   <li>Marks the representative as rejected with the provided reason</li>
     *   <li>Saves the updated representative record</li>
     * </ol>
     * 
     * <p><strong>Effects of Rejection:</strong>
     * <ul>
     *   <li>Representative cannot log in to the system</li>
     *   <li>Representative cannot post internship opportunities</li>
     *   <li>Rejection reason is recorded for future reference</li>
     * </ul>
     * 
     * <p><strong>Common Rejection Reasons:</strong>
     * <ul>
     *   <li>Invalid or unverifiable company information</li>
     *   <li>Suspicious or fraudulent registration</li>
     *   <li>Company not suitable for student internships</li>
     *   <li>Duplicate registration</li>
     * </ul>
     * 
     * @param staffId the unique identifier of the staff member performing the rejection
     * @param repEmail the email address of the company representative to reject
     * @param reason the reason for rejection (defaults to "Rejected by Career Center staff" if null)
     * @throws IllegalArgumentException if the company representative is not found
     * @throws IllegalStateException if the representative is already rejected
     */
    public void rejectCompanyRep(String staffId, String repEmail, String reason) {
        
        CompanyRep rep = repRepo.findByEmail(repEmail)
                .orElseThrow(() -> new IllegalArgumentException("Company representative not found: " + repEmail));
        
        if (rep.isRejected()) {
            throw new IllegalStateException("Company representative is already rejected.");
        }
        
        rep.reject(reason != null ? reason : "Rejected by Career Center staff");
        repRepo.save(rep);
    }

    /**
     * Approves a student's withdrawal request and withdraws the application.
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Validates the staff member exists</li>
     *   <li>Validates the withdrawal request exists and is PENDING</li>
     *   <li>Marks the withdrawal request as APPROVED with staff details</li>
     *   <li>Withdraws the associated application</li>
     *   <li>Saves both the withdrawal request and updated application</li>
     * </ol>
     * 
     * <p><strong>Effects of Approval:</strong>
     * <ul>
     *   <li>Application status changes to WITHDRAWN</li>
     *   <li>Internship slot becomes available again</li>
     *   <li>Student can apply to other internships</li>
     *   <li>Withdrawal is recorded with approval note</li>
     * </ul>
     * 
     * @param staffId the unique identifier of the staff member approving the withdrawal
     * @param wrId the unique identifier of the withdrawal request
     * @param note additional notes about the approval (defaults to "Approved by Career Center staff" if null)
     * @throws IllegalArgumentException if the staff member or withdrawal request is not found
     * @throws IllegalStateException if the withdrawal request is not in PENDING status
     */
    public void approveWithdrawal(String staffId, String wrId, String note) {
        CareerCenterStaff staff = validateStaffExists(staffId);
        WithdrawalRequest wr = withdrawalRepo.findById(wrId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal request not found: " + wrId));

        if (wr.getStatus() != WithdrawalRequestStatus.PENDING) {
            throw new IllegalStateException("Withdrawal request is already processed.");
        }

        wr.approve(staff, note != null ? note : "Approved by Career Center staff");
        withdrawalRepo.save(wr);
        applicationRepo.save(wr.getApplication());
    }

    /**
     * Rejects a student's withdrawal request.
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Validates the staff member exists</li>
     *   <li>Validates the withdrawal request exists and is PENDING</li>
     *   <li>Marks the withdrawal request as REJECTED with staff details</li>
     *   <li>Saves the withdrawal request (application remains unchanged)</li>
     * </ol>
     * 
     * <p><strong>Effects of Rejection:</strong>
     * <ul>
     *   <li>Application status remains unchanged (usually SUCCESSFUL)</li>
     *   <li>Student is expected to fulfill the internship commitment</li>
     *   <li>Rejection is recorded with explanation note</li>
     * </ul>
     * 
     * <p><strong>Common Rejection Reasons:</strong>
     * <ul>
     *   <li>Insufficient justification for withdrawal</li>
     *   <li>Withdrawal request submitted too late</li>
     *   <li>Student already committed to the internship</li>
     *   <li>Request made in bad faith</li>
     * </ul>
     * 
     * @param staffId the unique identifier of the staff member rejecting the withdrawal
     * @param wrId the unique identifier of the withdrawal request
     * @param note explanation for the rejection (defaults to "Rejected by Career Center staff" if null)
     * @throws IllegalArgumentException if the staff member or withdrawal request is not found
     * @throws IllegalStateException if the withdrawal request is not in PENDING status
     */
    public void rejectWithdrawal(String staffId, String wrId, String note) {
        CareerCenterStaff staff = validateStaffExists(staffId);
        WithdrawalRequest wr = withdrawalRepo.findById(wrId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal request not found: " + wrId));

        if (wr.getStatus() != WithdrawalRequestStatus.PENDING) {
            throw new IllegalStateException("Withdrawal request is already processed.");
        }

        wr.reject(staff, note != null ? note : "Rejected by Career Center staff");
        withdrawalRepo.save(wr);
    }
    
    /**
     * Filters internships based on multiple criteria.
     * 
     * <p>Provides advanced filtering capabilities to search and monitor internship
     * postings across various dimensions. All criteria are optional and can be
     * combined for more specific searches.
     * 
     * <p><strong>Filter Criteria:</strong>
     * <ul>
     *   <li><strong>Status:</strong> PENDING, APPROVED, REJECTED, or FILLED</li>
     *   <li><strong>Major:</strong> Preferred academic major (case-insensitive)</li>
     *   <li><strong>Company Name:</strong> Company offering the internship (case-insensitive)</li>
     *   <li><strong>Level:</strong> FRESHMAN, SOPHOMORE, JUNIOR, or SENIOR</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong>
     * <ul>
     *   <li>Find all PENDING postings: filterInternships(PENDING, null, null, null)</li>
     *   <li>Find Computer Science internships: filterInternships(null, "Computer Science", null, null)</li>
     *   <li>Find Google internships for seniors: filterInternships(null, null, "Google", "SENIOR")</li>
     * </ul>
     * 
     * @param status the internship status to filter by (null to ignore)
     * @param major the preferred major to filter by (null or empty to ignore)
     * @param companyName the company name to filter by (null or empty to ignore)
     * @param level the internship level to filter by as string (null or empty to ignore)
     * @return a list of internships matching all specified criteria
     * @throws IllegalArgumentException if the level string is invalid
     */
    public List<Internship> filterInternships(InternshipStatus status, String major, 
                                             String companyName, String level) {
        List<Internship> allInternships = internshipRepo.findAll();
        List<Internship> filtered = new ArrayList<>();
        
        for (Internship internship : allInternships) {
            boolean matches = true;
            
            if (status != null && internship.getStatus() != status) {
                matches = false;
            }
            if (major != null && !major.isEmpty() && 
                !internship.getPreferredMajor().equalsIgnoreCase(major)) {
                matches = false;
            }
            if (companyName != null && !companyName.isEmpty() &&
                !internship.getCompanyName().equalsIgnoreCase(companyName)) {
                matches = false;
            }
            if (level != null && !level.isEmpty() &&
                internship.getLevel() != InternshipLevel.valueOf(level.toUpperCase())) {
                matches = false;
            }
            
            if (matches) {
                filtered.add(internship);
            }
        }
        
        return filtered;
    }
    
    /**
     * Validates that a career center staff member exists in the repository.
     * 
     * <p>Used internally to ensure operations are performed by valid,
     * authenticated staff members.
     * 
     * @param staffId the staff identifier to validate
     * @return the staff member if found
     * @throws IllegalArgumentException if the staff member is not found
     */
    private CareerCenterStaff validateStaffExists(String staffId) {
        return staffRepo.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found: " + staffId));
    }
}