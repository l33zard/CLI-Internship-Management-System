package controller;

import database.ApplicationRepository;
import database.CareerCenterStaffRepository;
import database.CompanyRepRepository;
import database.InternshipRepository;
import database.WithdrawalRequestRepository;
import entity.CareerCenterStaff;
import entity.CompanyRep;
import entity.Internship;
import entity.InternshipLevel;
import entity.InternshipStatus;
import entity.WithdrawalRequest;
import entity.WithdrawalRequestStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Career Center staff operations such as approving internships,
 * managing company representative registrations and processing withdrawal
 * requests.
 * <p>
 * This controller provides administrative functionality for Career Center staff including:
 * <ul>
 *   <li>Reviewing and approving/rejecting internship postings</li>
 *   <li>Managing company representative account approvals</li>
 *   <li>Processing student withdrawal requests</li>
 *   <li>Viewing and filtering all system data for oversight</li>
 * </ul>
 * 
 */
public class CareerCenterStaffController extends BaseController {

    /**
     * Constructs a CareerCenterStaffController with required repositories.
     *
     * @param companyRepRepo repository for company representative data operations
     * @param internshipRepo repository for internship data operations
     * @param applicationRepo repository for application data operations
     * @param withdrawalsRepo repository for withdrawal request data operations
     * @param staffRepo repository for career center staff data operations
     */
    public CareerCenterStaffController(CompanyRepRepository companyRepRepo,
                                       InternshipRepository internshipRepo,
                                       ApplicationRepository applicationRepo,
                                       WithdrawalRequestRepository withdrawalsRepo,
                                       CareerCenterStaffRepository staffRepo) {
        super(null, companyRepRepo, staffRepo, internshipRepo, applicationRepo, withdrawalsRepo);
    }

    /**
     * Returns all internships currently awaiting Career Center approval.
     * <p>
     * These are internships that have been submitted by company representatives
     * but have not yet been reviewed by staff.
     *
     * @return list of internships with status PENDING, or empty list if none pending
     */
    public List<Internship> listPendingInternships() {
        return internshipRepo.findByStatus(InternshipStatus.PENDING);
    }

    /**
     * Returns company representatives pending approval (neither approved nor rejected).
     * <p>
     * These are newly registered representatives who require staff review before
     * they can access the system.
     *
     * @return list of unapproved company representatives, or empty list if none pending
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
     * Returns all company representatives in the system regardless of approval status.
     *
     * @return list of all company representatives, or empty list if none exist
     */
    public List<CompanyRep> listAllReps() {
        return repRepo.findAll();
    }

    /**
     * Returns all withdrawal requests currently pending staff review.
     * <p>
     * These are requests from students to withdraw applications that require
     * staff approval before being processed.
     *
     * @return list of pending withdrawal requests, or empty list if none pending
     */
    public List<WithdrawalRequest> listPendingWithdrawals() {
        return withdrawalRepo.findPending();
    }
    
    /**
     * Returns all internships in the system regardless of status or visibility.
     * <p>
     * Includes approved, rejected, pending, and hidden internships for complete
     * administrative oversight.
     *
     * @return list of all internships, or empty list if none exist
     */
    public List<Internship> listAllInternships() {
        return internshipRepo.findAll();
    }

    /**
     * Approves a pending internship and optionally makes it visible to students.
     * <p>
     * Once approved, the internship can accept applications from students.
     * The visibility flag determines if it appears in student browsing immediately.
     *
     * @param staffId the unique identifier of the staff member performing the approval
     * @param internshipId the unique identifier of the internship to approve
     * @param makeVisible if true, the internship will be immediately visible to students;
     *        if false, it remains hidden until made visible separately
     * @throws IllegalArgumentException if the internship cannot be found
     * @throws IllegalStateException if the internship is not in PENDING state
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
     * Rejects a pending internship (cannot be reopened).
     * <p>
     * Rejected internships are permanently closed and cannot be resubmitted
     * or made visible to students.
     *
     * @param staffId the unique identifier of the staff member performing the rejection
     * @param internshipId the unique identifier of the internship to reject
     * @throws IllegalArgumentException if the internship cannot be found
     * @throws IllegalStateException if the internship is not in PENDING state
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
     * Approves a pending company representative registration.
     * <p>
     * Once approved, the representative can log in and manage internships
     * for their company.
     *
     * @param staffId the unique identifier of the staff member performing the approval
     * @param repEmail the email address of the representative to approve
     * @throws IllegalArgumentException if the representative cannot be found
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
     * Rejects a company representative registration with an optional reason.
     * <p>
     * The rejection reason is communicated to the representative and the account
     * cannot be used to access the system.
     *
     * @param staffId the unique identifier of the staff member performing the rejection
     * @param repEmail the email address of the representative to reject
     * @param reason optional rejection reason communicated to the representative;
     *        if null, a default message is used
     * @throws IllegalArgumentException if the representative cannot be found
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
     * Approves a withdrawal request and marks the associated application as withdrawn.
     * <p>
     * The application is immediately withdrawn and the student's slot is freed up
     * for other applicants.
     *
     * @param staffId the unique identifier of the staff member performing the approval
     * @param wrId the unique identifier of the withdrawal request
     * @param note optional staff notes added to the withdrawal record;
     *        if null, a default message is used
     * @throws IllegalArgumentException if the withdrawal request cannot be found
     * @throws IllegalStateException if the request is already processed (not PENDING)
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
     * Rejects a withdrawal request (student keeps their application active).
     * <p>
     * The application remains in its current status and the withdrawal request
     * is marked as rejected with the provided reason.
     *
     * @param staffId the unique identifier of the staff member performing the rejection
     * @param wrId the unique identifier of the withdrawal request
     * @param note optional staff notes added to the withdrawal record;
     *        if null, a default message is used
     * @throws IllegalArgumentException if the withdrawal request cannot be found
     * @throws IllegalStateException if the request is already processed (not PENDING)
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
     * Filters internships by optional criteria (any null parameter is ignored).
     * <p>
     * Provides flexible searching across all internships for administrative purposes.
     * All filters are applied conjunctively (AND logic).
     *
     * @param status filter by internship status, or null for all statuses
     * @param major filter by preferred major (case-insensitive), or null for all majors
     * @param companyName filter by company name (case-insensitive), or null for all companies
     * @param level filter by internship level, or null for all levels
     * @return filtered list of internships matching all specified criteria,
     *         or empty list if no matches found
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
     * Validates that a staff member with the given ID exists and returns the entity.
     *
     * @param staffId the unique identifier of the staff member to validate
     * @return the CareerCenterStaff entity if found
     * @throws IllegalArgumentException if no staff member is found with the given ID
     */
    private CareerCenterStaff validateStaffExists(String staffId) {
        return staffRepo.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found: " + staffId));
    }
}