package entity;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a student user who can apply for internship positions.
 * 
 * <p>This entity manages student-specific domain logic including eligibility rules,
 * application caps, and placement restrictions. Students are subject to the following
 * domain constraints:
 *
 * <p><b>Eligibility Rules:</b>
 * <ul>
 *   <li>Year 1-2 students: Eligible for BASIC level internships only</li>
 *   <li>Year 3-4 students: Eligible for BASIC, INTERMEDIATE, and ADVANCED level internships</li>
 * </ul>
 *
 * <p><b>Application Limits:</b>
 * <ul>
 *   <li>Maximum of {@value #MAX_ACTIVE_APPLICATIONS} active applications per student</li>
 *   <li>Active applications include PENDING and SUCCESSFUL (but not yet accepted) statuses</li>
 *   <li>Exactly one confirmed placement allowed per student</li>
 * </ul>
 *
 */
public class Student extends User {
    private String major;      
    private int yearOfStudy;   
    private String email;

    /**
     * Maximum number of active applications allowed per student.
     * Active applications are those in PENDING status or SUCCESSFUL but not yet accepted.
     */
    public static final int MAX_ACTIVE_APPLICATIONS = 3;

    /**
     * Creates a new student record.
     *
     * @param userId the unique user identifier
     * @param name the student's full name
     * @param major the student's academic major
     * @param yearOfStudy the numeric year of study (1-4)
     * @param email the student's contact email
     * @throws IllegalArgumentException if any required field is invalid
     */
    public Student(String userId, String name, String major, int yearOfStudy, String email) {
        super(userId, name);
        this.yearOfStudy = yearOfStudy;
        this.major = major;
        this.email = email;
    }

    // -------------------- Queries --------------------

    /**
     * Returns the student's academic major.
     *
     * @return the student's major
     */
    public String getMajor() { return major; }
    
    /**
     * Returns the student's year of study.
     *
     * @return the numeric year (1-4)
     */
    public int getYearOfStudy() { return yearOfStudy; }
    
    /**
     * Returns the student's contact email.
     *
     * @return the email address
     */
    public String getEmail() { return email; }

    /**
     * Checks if the student is eligible for internships at the given level.
     * Eligibility is based on year of study:
     * <ul>
     *   <li>Years 1-2: BASIC level only</li>
     *   <li>Years 3-4: BASIC, INTERMEDIATE, and ADVANCED levels</li>
     * </ul>
     *
     * @param level the internship level to check eligibility for
     * @return true if eligible, false otherwise
     */
    public boolean isEligibleFor(InternshipLevel level) {
        if (level == null) return false;
        if (yearOfStudy <= 2) {
            return level == InternshipLevel.BASIC;
        }
        return level == InternshipLevel.BASIC
            || level == InternshipLevel.INTERMEDIATE
            || level == InternshipLevel.ADVANCED;
    }

    /**
     * Filters a list of internships to show only those the student is eligible for,
     * that are currently visible, and open for applications.
     *
     * @param all the complete list of internships to filter
     * @return a filtered list containing only eligible, visible, open internships
     */
    public List<Internship> filterEligibleVisibleOpen(List<Internship> all) {
        if (all == null) return List.of();
        return all.stream()
                  .filter(i -> i != null
                           && i.isOpenForApplications(LocalDate.now())
                           && i.isVisible()  // ← THIS FILTERS OUT NON-VISIBLE INTERNSHIPS
                           && isEligibleFor(i.getLevel()))
                  .collect(Collectors.toList());
    }

    /**
     * Checks if the student currently has a confirmed internship placement.
     *
     * @param apps the application read port for querying placement status
     * @return true if the student has a confirmed placement, false otherwise
     * @throws NullPointerException if the apps port is null
     */
    public boolean hasConfirmedPlacement(AppReadPort apps) {
        return apps.hasConfirmedPlacement(getUserId());
    }

    /**
     * Returns the number of active applications counting toward the student's cap.
     * Active applications include PENDING and SUCCESSFUL (but not yet accepted) statuses.
     *
     * @param apps the application read port for querying application counts
     * @return the number of active applications
     * @throws NullPointerException if the apps port is null
     */
    public int activeApplicationsCount(AppReadPort apps) {
        return apps.countActiveApplications(getUserId());
    }

    /**
     * Checks if the student can start another application without exceeding the cap.
     *
     * @param apps the application read port for querying application counts
     * @return true if under the application cap, false otherwise
     * @throws NullPointerException if the apps port is null
     */
    public boolean canStartAnotherApplication(AppReadPort apps) {
        return activeApplicationsCount(apps) < MAX_ACTIVE_APPLICATIONS;
    }

    // -------------------- Commands (domain validations only) --------------------

    /**
     * Validates that the student can apply for the specified internship.
     * Checks all domain rules including eligibility, application caps, and existing placements.
     *
     * @param internship the internship to apply for
     * @param apps the application read port for validation checks
     * @throws NullPointerException if internship or apps port is null
     * @throws IllegalStateException if any domain validation rule is violated
     */
    public void assertCanApply(Internship internship, AppReadPort apps) {
        Objects.requireNonNull(internship, "Internship required");
        Objects.requireNonNull(apps, "AppReadPort required");

        if (!internship.isOpenForApplications(LocalDate.now()) || !internship.isVisible())
            throw new IllegalStateException("Internship is not open/visible");

        if (!isEligibleFor(internship.getLevel()))
            throw new IllegalStateException("Not eligible for level " + internship.getLevel());

        if (hasConfirmedPlacement(apps))
            throw new IllegalStateException("Already have a confirmed placement");

        if (!canStartAnotherApplication(apps))
            throw new IllegalStateException("Application cap reached (" + MAX_ACTIVE_APPLICATIONS + ")");
    }

    /**
     * Validates that the student can confirm an internship offer.
     * Ensures the student doesn't already have a confirmed placement.
     *
     * @param apps the application read port for validation checks
     * @throws IllegalStateException if the student already has a confirmed placement
     * @throws NullPointerException if the apps port is null
     */
    public void assertCanConfirmOffer(AppReadPort apps) {
        if (hasConfirmedPlacement(apps))
            throw new IllegalStateException("Cannot confirm: placement already confirmed");
    }

    // -------------------- Identity & debug --------------------
//
//    /**
//     * Compares this student with another object for equality based on user ID.
//     *
//     * @param o the object to compare with
//     * @return true if the objects are equal, false otherwise
//     */
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Student)) return false;
//        Student that = (Student) o;
//        return Objects.equals(getUserId(), that.getUserId());
//    }
//
//    /**
//     * Returns the hash code for this student based on the user ID.
//     *
//     * @return the hash code
//     */
//    @Override
//    public int hashCode() {
//        return Objects.hash(getUserId());
//    }
//
//    /**
//     * Returns a string representation of this student.
//     *
//     * @return formatted string with key student details
//     */
//    @Override
//    public String toString() {
//        return "Student{id=" + getUserId() + ", name=" + getName() +
//               ", major=" + major + ", year=" + yearOfStudy + "}";
//    }

    // -------------------- Ports (read-only) --------------------
    
    /**
     * Minimal read-only application view required by the domain layer.
     * This interface should be implemented by repository/service layers and injected
     * into student domain methods that need to query application data.
     * 
     * <p><b>Implementation Note:</b> Active applications count should include
     * PENDING and SUCCESSFUL (unaccepted) statuses, and exclude UNSUCCESSFUL
     * and accepted applications.
     */
    public interface AppReadPort {
        
        /**
         * Counts the number of active applications for a student.
         * Active applications are those in PENDING status or SUCCESSFUL but not yet accepted.
         *
         * @param studentId the student's unique identifier
         * @return the number of active applications
         */
        int countActiveApplications(String studentId);
        
        /**
         * Checks if a student has a confirmed internship placement.
         * A confirmed placement is typically an application in SUCCESSFUL status
         * that has been accepted by the student.
         *
         * @param studentId the student's unique identifier
         * @return true if the student has a confirmed placement, false otherwise
         */
        boolean hasConfirmedPlacement(String studentId);
    }
}