package controller;

import database.ApplicationRepository;
import database.InternshipRepository;
import database.StudentRepository;
import database.WithdrawalRequestRepository;
import entity.*;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Controller managing student-related operations and use cases.
 * 
 * <p>StudentController orchestrates all student interactions with the internship
 * system, including browsing opportunities, submitting applications, accepting offers,
 * and requesting withdrawals. It enforces business rules and coordinates between
 * entities and repositories.</p>
 * 
 * <h2>Core Responsibilities</h2>
 * <ul>
 *   <li><b>Internship Discovery:</b> View and filter available internships</li>
 *   <li><b>Application Management:</b> Submit applications, view status</li>
 *   <li><b>Offer Response:</b> Accept or reject successful offers</li>
 *   <li><b>Withdrawal Requests:</b> Request withdrawal from accepted placements</li>
 *   <li><b>Eligibility Enforcement:</b> Validate year-based and cap-based rules</li>
 * </ul>
 * 
 * <h2>Business Rules Enforced</h2>
 * <ul>
 *   <li>Year 1-2: BASIC level only; Year 3-4: All levels</li>
 *   <li>Maximum {@value Student#MAX_ACTIVE_APPLICATIONS} active applications per student</li>
 *   <li>Only one confirmed placement allowed per student</li>
 *   <li>Cannot apply for same internship twice</li>
 *   <li>Only visible, approved, open internships shown</li>
 *   <li>Auto-withdraw other applications when one offer accepted</li>
 * </ul>
 * 
 * <h2>Key Workflows</h2>
 * 
 * <h3>Application Submission</h3>
 * <ol>
 *   <li>Student browses eligible internships</li>
 *   <li>Selects internship to apply for</li>
 *   <li>System validates eligibility and application cap</li>
 *   <li>Application created in PENDING status</li>
 *   <li>Company representative notified for review</li>
 * </ol>
 * 
 * <h3>Offer Acceptance</h3>
 * <ol>
 *   <li>Student reviews SUCCESSFUL applications</li>
 *   <li>Selects offer to accept</li>
 *   <li>System confirms no existing placement</li>
 *   <li>Application marked as ACCEPTED</li>
 *   <li>Internship slot decremented</li>
 *   <li>All other applications auto-withdrawn</li>
 * </ol>
 * 
 * <h3>Withdrawal Request</h3>
 * <ol>
 *   <li>Student with accepted placement requests withdrawal</li>
 *   <li>Provides reason for withdrawal</li>
 *   <li>Withdrawal request created in PENDING status</li>
 *   <li>Career center staff reviews and processes</li>
 *   <li>If approved: placement freed, slot released</li>
 * </ol>
 * 
 * @author SC2002 Assignment Team
 * @version 1.0
 * @since 1.0
 * @see Student
 * @see Internship
 * @see InternshipApplication
 * @see BaseController
 */
public class StudentController extends BaseController {

    /**
     * Constructs a StudentController with required repositories.
     * 
     * <p>This controller only needs student, internship, application, and
     * withdrawal repositories. Company rep and staff repositories are not
     * needed and are passed as null to the superclass.</p>
     * 
     * @param students repository for student data access
     * @param internships repository for internship data access
     * @param applications repository for application data access
     * @param withdrawals repository for withdrawal request data access
     */
    public StudentController(StudentRepository students,
                             InternshipRepository internships,
                             ApplicationRepository applications,
                             WithdrawalRequestRepository withdrawals) {
        super(students, null, null, internships, applications, withdrawals);
    }

    /**
     * Retrieves a student by ID.
     * 
     * @param studentId the student's unique identifier
     * @return the Student entity
     * @throws IllegalArgumentException if student not found
     */
    public Student getStudent(String studentId) {
        return studentRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
    }

    /**
     * Returns all internships visible and eligible for a student, including already applied ones.
     * 
     * <p>This method provides a comprehensive view showing:</p>
     * <ul>
     *   <li>Internships the student has already applied to (regardless of status)</li>
     *   <li>Open, visible, eligible internships the student can apply to</li>
     * </ul>
     * 
     * <p>This allows the UI to show both active applications and new opportunities.</p>
     * 
     * @param studentId the student's unique identifier
     * @return list of eligible internships (including already applied)
     * @throws IllegalArgumentException if student not found
     */
    public List<Internship> viewEligibleInternships(String studentId) {
        Student s = getStudent(studentId);
        List<Internship> allInternships = internshipRepo.findAll();
        List<Internship> eligibleInternships = new ArrayList<>();
        
        List<String> appliedInternshipIds = applicationRepo.findByStudent(studentId).stream()
                .map(app -> app.getInternship().getInternshipId())
                .collect(Collectors.toList());
        
        for (Internship internship : allInternships) {
            if (appliedInternshipIds.contains(internship.getInternshipId())) {
                eligibleInternships.add(internship);
            } else if (internship.isOpenForApplications(LocalDate.now()) && 
                       internship.isVisible() && 
                       s.isEligibleFor(internship.getLevel())) {
                eligibleInternships.add(internship);
            }
        }
        
        return eligibleInternships;
    }

    /**
     * Returns filtered internships based on student preferences.
     * 
     * <p>Provides advanced filtering capabilities for students to narrow down
     * opportunities based on their preferences. All filters are optional (can be null).</p>
     * 
     * <p><b>Available Filters:</b></p>
     * <ul>
     *   <li><b>major:</b> Filter by preferred major (case-insensitive)</li>
     *   <li><b>level:</b> Filter by difficulty level</li>
     *   <li><b>companyName:</b> Filter by company name (case-insensitive)</li>
     * </ul>
     * 
     * <p><b>Base filtering (always applied):</b></p>
     * <ul>
     *   <li>Only visible internships</li>
     *   <li>Only open for applications (within date range)</li>
     *   <li>Only levels student is eligible for (based on year)</li>
     * </ul>
     * 
     * @param studentId the student's unique identifier
     * @param major preferred major filter (null to skip)
     * @param level difficulty level filter (null to skip)
     * @param companyName company name filter (null to skip)
     * @return filtered list of eligible internships
     * @throws IllegalArgumentException if student not found
     */
    public List<Internship> viewFilteredInternships(String studentId, String major, 
                                                   InternshipLevel level, String companyName) {
        Student s = getStudent(studentId);
        List<Internship> eligible = s.filterEligibleVisibleOpen(internshipRepo.findAll());
        
        return eligible.stream()
                .filter(internship -> major == null || internship.getPreferredMajor().equalsIgnoreCase(major))
                .filter(internship -> level == null || internship.getLevel() == level)
                .filter(internship -> companyName == null || internship.getCompanyName().equalsIgnoreCase(companyName))
                .toList();
    }

    /**
     * Submits an application for an internship on behalf of a student.
     * 
     * <p>This method performs comprehensive validation before creating the application:</p>
     * 
     * <h3>Validations Performed:</h3>
     * <ul>
     *   <li>Student exists in system</li>
     *   <li>Internship exists and is accessible</li>
     *   <li>Student hasn't already applied to this internship</li>
     *   <li>Internship is open, visible, and student is eligible for level</li>
     *   <li>Student doesn't have a confirmed placement</li>
     *   <li>Student hasn't exceeded {@value Student#MAX_ACTIVE_APPLICATIONS} application cap</li>
     * </ul>
     * 
     * <p>If all validations pass, an application is created in PENDING status
     * and saved to the repository.</p>
     * 
     * @param studentId the student submitting the application
     * @param internshipId the internship to apply for
     * @return the generated application ID
     * @throws IllegalArgumentException if student or internship not found
     * @throws IllegalStateException if already applied, internship not open,
     *         student not eligible, has confirmed placement, or exceeded cap
     */
    public String applyForInternship(String studentId, String internshipId) {
        Student s = getStudent(studentId);
        Internship i = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));

        if (applicationRepo.existsByStudentAndInternship(studentId, internshipId)) {
            throw new IllegalStateException("You have already applied to this internship.");
        }

        s.assertCanApply(i, applicationRepo);
        InternshipApplication app = new InternshipApplication(LocalDate.now(), s, i, applicationRepo);
        applicationRepo.save(app);
        return app.getApplicationId();
    }

    /**
     * Confirms a student's acceptance of a successful internship offer.
     * 
     * <p>This method handles the critical workflow when a student accepts an offer:</p>
     * 
     * <h3>Process Steps:</h3>
     * <ol>
     *   <li>Validate application belongs to the student</li>
     *   <li>Verify application is in SUCCESSFUL status</li>
     *   <li>Confirm not already accepted</li>
     *   <li>Check student has no other confirmed placement</li>
     *   <li>Accept the offer (decrements internship slot)</li>
     *   <li>Auto-withdraw all other PENDING/SUCCESSFUL applications</li>
     * </ol>
     * 
     * <p><b>Important:</b> Accepting an offer automatically withdraws all other
     * applications to enforce the "one placement per student" rule.</p>
     * 
     * @param studentId the student accepting the offer
     * @param applicationId the application to accept
     * @throws IllegalArgumentException if application not found
     * @throws SecurityException if application doesn't belong to student
     * @throws IllegalStateException if application not SUCCESSFUL, already accepted,
     *         or student has confirmed placement
     */
    public void confirmAcceptance(String studentId, String applicationId) {
        var app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        if (app.getStudent() == null || !studentId.equals(app.getStudent().getUserId())) {
            throw new SecurityException("You can only accept your own application.");
        }
        if (app.getStatus() != ApplicationStatus.SUCCESSFUL) {
            throw new IllegalStateException("Only SUCCESSFUL applications can be accepted.");
        }
        if (app.isStudentAccepted()) {
            throw new IllegalStateException("This offer is already accepted.");
        }

        app.confirmAcceptance(applicationRepo);
        applicationRepo.save(app);

        // Auto-withdraw other applications
        var others = applicationRepo.findByStudent(studentId);
        for (var other : others) {
            if (other.getApplicationId().equals(applicationId)) continue;
            var st = other.getStatus();
            if (st == ApplicationStatus.PENDING || st == ApplicationStatus.SUCCESSFUL) {
                other.markWithdrawn();
                applicationRepo.save(other);
            }
        }
    }

    /**
     * Creates a withdrawal request for an accepted internship placement.
     * 
     * <p>Students with accepted placements can request withdrawal if circumstances
     * change. All withdrawals require career center staff approval to ensure proper
     * oversight and slot management.</p>
     * 
     * <h3>Validation:</h3>
     * <ul>
     *   <li>Application must exist and belong to requesting student</li>
     *   <li>Reason must be provided (not null or empty)</li>
     *   <li>No existing pending withdrawal for this application</li>
     * </ul>
     * 
     * <p>The withdrawal request is created in PENDING status and must be
     * approved by staff before the placement is released.</p>
     * 
     * @param studentId the student requesting withdrawal
     * @param applicationId the accepted application to withdraw from
     * @param reason explanation for withdrawal (required)
     * @return the generated withdrawal request ID
     * @throws IllegalArgumentException if application not found or reason missing
     * @throws SecurityException if application doesn't belong to student
     * @throws IllegalStateException if pending withdrawal request already exists
     */
    public String requestWithdrawal(String studentId, String applicationId, String reason) {
        InternshipApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        
        if (!app.getStudent().getUserId().equals(studentId)) {
            throw new SecurityException("Cannot withdraw another student's application");
        }
        
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Withdrawal reason is required.");
        }
        
        var existingRequest = withdrawalRepo.findByApplicationId(applicationId);
        if (existingRequest.isPresent() && existingRequest.get().isPending()) {
            throw new IllegalStateException("There is already a pending withdrawal request for this application.");
        }

        WithdrawalRequest wr = new WithdrawalRequest(app, app.getStudent(), reason);
        withdrawalRepo.save(wr);
        return wr.getRequestId();
    }

    /**
     * Retrieves all applications submitted by a student.
     * 
     * <p>Returns complete application history including:</p>
     * <ul>
     *   <li>PENDING applications under review</li>
     *   <li>SUCCESSFUL offers awaiting student decision</li>
     *   <li>ACCEPTED placements</li>
     *   <li>UNSUCCESSFUL rejections</li>
     *   <li>WITHDRAWN applications</li>
     * </ul>
     * 
     * <p>This method also re-attaches internship objects to applications
     * after loading from the repository.</p>
     * 
     * @param studentId the student's unique identifier
     * @return list of all applications for the student
     */
    public List<InternshipApplication> viewMyApplications(String studentId) {
        List<InternshipApplication> apps = applicationRepo.findByStudent(studentId);
        for (InternshipApplication app : apps) {
            String itId = app.getInternshipId();
            Internship it = internshipRepo.findById(itId).orElse(null);
            app.setInternship(it);
        }
        return apps;
    }

    /**
     * Retrieves all withdrawal requests submitted by a student.
     * 
     * <p>Returns withdrawal request history including:</p>
     * <ul>
     *   <li>PENDING requests awaiting staff review</li>
     *   <li>APPROVED withdrawals (placement freed)</li>
     *   <li>REJECTED withdrawals (placement maintained)</li>
     * </ul>
     * 
     * @param studentId the student's unique identifier
     * @return list of all withdrawal requests for the student
     */
    public List<WithdrawalRequest> viewMyWithdrawalRequests(String studentId) {
        return withdrawalRepo.findByStudent(studentId);
    }

    /**
     * Retrieves a specific application with ownership verification.
     * 
     * @param studentId the student requesting access
     * @param applicationId the application to retrieve
     * @return the application
     * @throws IllegalArgumentException if application not found
     * @throws SecurityException if application doesn't belong to student
     */
    public InternshipApplication getApplication(String studentId, String applicationId) {
        InternshipApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        
        if (!app.getStudent().getUserId().equals(studentId)) {
            throw new SecurityException("Cannot access another student's application");
        }
        
        return app;
    }

    /**
     * Checks if student can submit another application without exceeding the cap.
     * 
     * @param studentId the student's unique identifier
     * @return {@code true} if student can apply (under cap), {@code false} otherwise
     * @throws IllegalArgumentException if student not found
     */
    public boolean canApplyMore(String studentId) {
        Student s = getStudent(studentId);
        return s.canStartAnotherApplication(applicationRepo);
    }

    /**
     * Returns the count of active applications for a student.
     * 
     * <p>Active applications include PENDING and SUCCESSFUL (unaccepted) status.</p>
     * 
     * @param studentId the student's unique identifier
     * @return count of active applications
     * @throws IllegalArgumentException if student not found
     */
    public int getActiveApplicationCount(String studentId) {
        Student s = getStudent(studentId);
        return s.activeApplicationsCount(applicationRepo);
    }

    /**
     * Checks if student has a confirmed internship placement.
     * 
     * @param studentId the student's unique identifier
     * @return {@code true} if student has confirmed placement, {@code false} otherwise
     * @throws IllegalArgumentException if student not found
     */
    public boolean hasConfirmedPlacement(String studentId) {
        Student s = getStudent(studentId);
        return s.hasConfirmedPlacement(applicationRepo);
    }

    /**
     * Retrieves an internship by ID.
     * 
     * @param internshipId the internship's unique identifier
     * @return the Internship entity
     * @throws IllegalArgumentException if internship not found
     */
    public Internship getInternship(String internshipId) {
        return internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));
    }
}