package entity;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * - Y1–Y2 eligible for BASIC only; Y3–Y4 eligible for BASIC/INTERMEDIATE/ADVANCED.
 * - Max 3 active applications (PENDING or SUCCESSFUL but not yet accepted).
 * - Exactly one confirmed placement per student.
 */
public class Student extends User {
    /**
     * The student's major or field of study.
     * 
     * <p>Examples include: CSC (Computer Science), EEE (Electrical Engineering),
     * MAE (Mechanical Engineering), etc.</p>
     */
    private String major;
    /**
     * Current year of study (1-4).
     * 
     * <p>Determines internship level eligibility:
     * Year 1-2 can only apply for BASIC level, while Year 3-4 can apply for all levels.</p>
     */     
    private int yearOfStudy;
    /**
     * The student's email address.
     */ 
    private String email;
    /** 
     * Maximum number of active applications allowed per student.
     * 
     * <p>Active applications are those with PENDING or SUCCESSFUL (unaccepted) status.
     * This cap prevents students from overwhelming the system with excessive applications.</p>
     */
    public static final int MAX_ACTIVE_APPLICATIONS = 3;
    /**
     * Constructs a new Student with the specified details.
     * 
     * <p>Creates a student user with academic information required for
     * determining internship eligibility and managing applications.</p>
     * 
     * @param userId unique student identifier (format: UXXXXXXXY where X is digit, Y is letter)
     * @param name full name of the student
     * @param major the student's major/field of study (e.g., "CSC", "EEE")
     * @param yearOfStudy current year of study (1-4)
     * @param email student's email address for communication
     */
    public Student(String userId, String name, String major, int yearOfStudy, String email) {
        super(userId, name);
        this.yearOfStudy = yearOfStudy;
        this.major = major;
        this.email = email;
    }

    // -------------------- Queries --------------------
    /**
     * Returns the student's major or field of study.
     * @return the major as a String (eg. "EEE", "MAE")
     */
    public String getMajor() { return major; }
    /**
     * Returns the student's current year of study.
     * @return the year of study
     */
    public int getYearOfStudy() { return yearOfStudy; }
    /**
     * Returns the student's email address.
     * @return the email address
     */
    public String getEmail() { return email; }

    /**
     * Determine if the student is eligible for a given internship level.
     * @param level the internship level to check
     * @return {@code true} if eligible, {@code false} otherwise
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
     * Filter a list of internships to those the student is eligible for,
     * that are currently open for applications and visible.
     * @param all list of internships to filter
     * @return list of eligible, visible, open internships
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
     * check if the student has a confirmed internship placement.
     * @param apps application read port
     * @return {@code true} if a confirmed placement exists, {@code false} otherwise
     */
    public boolean hasConfirmedPlacement(AppReadPort apps) {
        return apps.hasConfirmedPlacement(getUserId());
    }

    /**
     * Count the number of active applications (PENDING or SUCCESSFUL but not yet accepted).
     * @param apps application read port
     * @return count of active applications
     */
    public int activeApplicationsCount(AppReadPort apps) {
        return apps.countActiveApplications(getUserId());
    }

    /** 
     * Determine if the student can start another application based on active application count.
     * @param apps application read port
     * @return {@code true} if another application can be started, {@code false} otherwise
     */
    public boolean canStartAnotherApplication(AppReadPort apps) {
        return activeApplicationsCount(apps) < MAX_ACTIVE_APPLICATIONS;
    }

    // -------------------- Commands (domain validations only) --------------------

    /**
     * Validate that student can apply for specified internship.
     * @param internship the internship to apply for
     * @param apps application read port
     * @throws IllegalStateException if any application condition is not met
     * @throws NullPointerException if internship or apps is null
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
     * Validate that student can confirm an internship offer.
     * @param apps application read port
     * @throws IllegalStateException if a placement is already confirmed
     */
    public void assertCanConfirmOffer(AppReadPort apps) {
        if (hasConfirmedPlacement(apps))
            throw new IllegalStateException("Cannot confirm: placement already confirmed");
    }

    // -------------------- Identity & debug --------------------
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Student)) return false;
//        Student that = (Student) o;
//        return Objects.equals(getUserId(), that.getUserId());
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(getUserId());
//    }
//
//    @Override
//    public String toString() {
//        return "Student{id=" + getUserId() + ", name=" + getName() +
//               ", major=" + major + ", year=" + yearOfStudy + "}";
//    }

    // -------------------- Ports (read-only) --------------------
    /**
     * Minimal read-only application view the domain needs.
     * Implement this in your repository/service layer; inject into methods above.
     * Count should include PENDING and SUCCESSFUL(unaccepted), and exclude UNSUCCESSFUL and ACCEPTED.
     */
    public interface AppReadPort {
        int countActiveApplications(String studentId);
        boolean hasConfirmedPlacement(String studentId);
    }
}
