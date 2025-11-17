package controller;

import database.ApplicationRepository;
import database.InternshipRepository;
import database.StudentRepository;
import database.WithdrawalRequestRepository;
import entity.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller exposing student-facing operations required by the UI boundaries.
 * <p>
 * This controller provides all student-specific functionality including:
 * <ul>
 *   <li>Browsing and filtering eligible internships</li>
 *   <li>Applying to internships with application cap enforcement</li>
 *   <li>Managing applications (viewing, accepting offers, withdrawing)</li>
 *   <li>Tracking application status and placement confirmation</li>
 *   <li>Submitting and tracking withdrawal requests</li>
 * </ul>
 * 
 * The controller enforces business rules such as:
 * <ul>
 *   <li>Student eligibility based on academic level and internship requirements</li>
 *   <li>Application caps and limits</li>
 *   <li>Ownership validation for all student operations</li>
 *   <li>Status-based restrictions on operations</li>
 *   <li>Automatic withdrawal of other applications upon offer acceptance</li>
 * </ul>
 * 
 */
public class StudentController extends BaseController {

    /**
     * Constructs a StudentController wired to the required repositories.
     *
     * @param students the student repository for student data operations
     * @param internships the internship repository for internship data operations
     * @param applications the application repository for application data operations
     * @param withdrawals the withdrawal request repository for withdrawal operations
     */
    public StudentController(StudentRepository students,
                             InternshipRepository internships,
                             ApplicationRepository applications,
                             WithdrawalRequestRepository withdrawals) {
        super(students, null, null, internships, applications, withdrawals);
    }

    /**
     * Retrieves a student by their unique identifier.
     *
     * @param studentId the unique identifier of the student to retrieve
     * @return the Student entity with the specified ID
     * @throws IllegalArgumentException if no student is found with the given ID
     */
    public Student getStudent(String studentId) {
        return studentRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
    }

    /**
     * Returns internships eligible for the student based on current date and eligibility criteria.
     * <p>
     * An internship is considered eligible if it:
     * <ul>
     *   <li>Is currently open for applications (based on open/close dates)</li>
     *   <li>Is visible to students</li>
     *   <li>Matches the student's academic level requirements</li>
     * </ul>
     *
     * @param studentId the unique identifier of the student
     * @return a list of internships eligible for the student to apply to,
     *         or an empty list if no eligible internships exist
     * @throws IllegalArgumentException if the student is not found
     */
    public List<Internship> viewEligibleInternships(String studentId) {
        Student s = getStudent(studentId);
        List<Internship> allInternships = internshipRepo.findAll();
        List<Internship> eligibleInternships = new ArrayList<>();
        
        for (Internship internship : allInternships) {
            if (internship.isOpenForApplications(LocalDate.now()) && 
                       internship.isVisible() && 
                       s.isEligibleFor(internship.getLevel())) {
                eligibleInternships.add(internship);
            }
        }
        
        return eligibleInternships;
    }

    /**
     * Returns internships filtered by additional criteria from the student's eligible internships.
     * <p>
     * Applies additional filters to the student's eligible internships based on:
     * <ul>
     *   <li>Preferred major (case-insensitive match)</li>
     *   <li>Internship level (exact match)</li>
     *   <li>Company name (case-insensitive match)</li>
     * </ul>
     * Null filter parameters are ignored (no filtering applied for that criterion).
     *
     * @param studentId the unique identifier of the student
     * @param major the preferred major to filter by, or null for no major filtering
     * @param level the internship level to filter by, or null for no level filtering
     * @param companyName the company name to filter by, or null for no company filtering
     * @return a list of filtered internships that match all specified criteria,
     *         or an empty list if no matching internships exist
     * @throws IllegalArgumentException if the student is not found
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
     * Creates an application for a student to an internship.
     * <p>
     * Validates that:
     * <ul>
     *   <li>The student hasn't already applied to this internship</li>
     *   <li>The student is eligible to apply (level, application caps, etc.)</li>
     *   <li>The internship is open and visible</li>
     * </ul>
     *
     * @param studentId the unique identifier of the applying student
     * @param internshipId the unique identifier of the internship to apply to
     * @return the unique identifier of the created application
     * @throws IllegalArgumentException if the student or internship is not found
     * @throws IllegalStateException if the student has already applied to this internship,
     *         or if the student cannot apply due to eligibility or cap restrictions
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
     * Confirms a student's acceptance of an internship offer.
     * <p>
     * This operation:
     * <ul>
     *   <li>Reserves a slot in the internship</li>
     *   <li>Automatically withdraws all other active applications</li>
     *   <li>Marks the application as accepted by the student</li>
     * </ul>
     *
     * @param studentId the unique identifier of the student
     * @param applicationId the unique identifier of the application to accept
     * @throws IllegalArgumentException if the application is not found
     * @throws SecurityException if the student attempts to accept another student's application
     * @throws IllegalStateException if the application is not in SUCCESSFUL status,
     *         or if the offer has already been accepted
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
     * Creates a withdrawal request for a student's application.
     * <p>
     * Withdrawal requests require staff approval before the application is officially withdrawn.
     *
     * @param studentId the unique identifier of the student
     * @param applicationId the unique identifier of the application to withdraw
     * @param reason the reason for withdrawal (cannot be null or empty)
     * @return the unique identifier of the created withdrawal request
     * @throws IllegalArgumentException if the application is not found, or if the reason is null or empty
     * @throws SecurityException if the student attempts to withdraw another student's application
     * @throws IllegalStateException if there is already a pending withdrawal request for this application
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
     * Fetches all applications submitted by the student with internship details attached.
     *
     * @param studentId the unique identifier of the student
     * @return a list of the student's applications with populated internship details,
     *         or an empty list if the student has no applications
     * @throws IllegalArgumentException if the student is not found
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
     * Retrieves all withdrawal requests created by the student.
     *
     * @param studentId the unique identifier of the student
     * @return a list of withdrawal requests submitted by the student,
     *         or an empty list if the student has no withdrawal requests
     * @throws IllegalArgumentException if the student is not found
     */
    public List<WithdrawalRequest> viewMyWithdrawalRequests(String studentId) {
        return withdrawalRepo.findByStudent(studentId);
    }

    /**
     * Retrieves a specific application ensuring the requesting student owns it.
     *
     * @param studentId the unique identifier of the student
     * @param applicationId the unique identifier of the application to retrieve
     * @return the application with the specified ID
     * @throws IllegalArgumentException if the application is not found
     * @throws SecurityException if the application does not belong to the student
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
     * Checks whether the student can submit another application based on application caps.
     *
     * @param studentId the unique identifier of the student
     * @return true if the student is below the application cap and can apply to more internships,
     *         false otherwise
     * @throws IllegalArgumentException if the student is not found
     */
    public boolean canApplyMore(String studentId) {
        Student s = getStudent(studentId);
        return s.canStartAnotherApplication(applicationRepo);
    }

    /**
     * Returns the count of active applications that count toward the application cap.
     *
     * @param studentId the unique identifier of the student
     * @return the number of active applications submitted by the student
     * @throws IllegalArgumentException if the student is not found
     */
    public int getActiveApplicationCount(String studentId) {
        Student s = getStudent(studentId);
        return s.activeApplicationsCount(applicationRepo);
    }

    /**
     * Checks whether the student has a confirmed placement (accepted offer).
     *
     * @param studentId the unique identifier of the student
     * @return true if the student has accepted an offer for an internship,
     *         false otherwise
     * @throws IllegalArgumentException if the student is not found
     */
    public boolean hasConfirmedPlacement(String studentId) {
        Student s = getStudent(studentId);
        return s.hasConfirmedPlacement(applicationRepo);
    }

    /**
     * Retrieves an internship by its unique identifier.
     *
     * @param internshipId the unique identifier of the internship to retrieve
     * @return the Internship entity with the specified ID
     * @throws IllegalArgumentException if no internship is found with the given ID
     */
    public Internship getInternship(String internshipId) {
        return internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));
    }
}