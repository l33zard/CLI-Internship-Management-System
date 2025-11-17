package entity;

import java.time.LocalDate;

/**
 * Represents an internship posting with details such as title, description,
 * level, preferred major, application dates, company name, and slot availability.
 * Internships can be approved, rejected, made visible, and have their application
 * status checked.
 */
public class Internship {

    /**
     * Counter for generating unique internship IDs.
     * Increments with each new internship created.
     */
    private static int idCounter = 1;

    /**
     * Unique identifier for this internship.
     * Format: "INTXXXX" where XXXX is a zero-padded 4-digit number.
     */
    private final String internshipId;
    
    /**
     * The job title or position name for this internship.
     * Example: "Software Engineering Intern", "Data Analyst Intern"
     */
    private String title;
    
    /**
     * Detailed description of the internship role, responsibilities, and requirements.
     */
    private String description;
    
    /**
     * The difficulty/eligibility level of the internship.
     * Determines which students can apply based on year of study.
     * 
     * @see InternshipLevel
     * @see Student#isEligibleFor(InternshipLevel)
     */
    private InternshipLevel level;
    
    /**
     * Preferred major for applicants (optional filter).
     * Example: "CSC", "EEE", "Any". Used as a recommendation, not a hard requirement.
     */
    private String preferredMajor;
    
    /**
     * First date when students can begin applying.
     * Applications submitted before this date are not allowed.
     */
    private LocalDate openDate;
    
    /**
     * Last date when students can apply.
     * Applications submitted after this date are not allowed.
     */
    private LocalDate closeDate;
    
    /**
     * Name of the company offering this internship.
     * Used to match internships with company representatives.
     */
    private String companyName;
    
    /**
     * Maximum number of students that can be accepted for this internship.
     */
    private int maxSlots;
    
    /**
     * Current number of confirmed slots (students who accepted offers).
     * When this reaches maxSlots, status automatically becomes FILLED.
     */
    private int confirmedSlots = 0;
    
    /**
     * Visibility flag indicating if students can see this internship.
     * Only APPROVED internships can be made visible.
     */
    private boolean visible = false;
    
    /**
     * Current approval and availability status of the internship.
     * 
     * @see InternshipStatus
     */
    private InternshipStatus status = InternshipStatus.PENDING;

    /* ===================== Constructors ===================== */
    /**
     * Constructs a new Internship with the specified details.
     * 
     * <p>The internship is created in PENDING status and is not visible until
     * approved by career center staff. A unique internship ID is automatically
     * generated in the format "INTXXXX".</p>
     * 
     * @param title the internship position title
     * @param description detailed description of the role and requirements
     * @param level the difficulty level determining student eligibility
     * @param preferredMajor preferred major for applicants (can be "Any")
     * @param openDate first date students can apply
     * @param closeDate last date students can apply
     * @param companyName name of the company offering the internship
     * @param maxSlots maximum number of students that can be accepted
     */
    public Internship(String title,
                      String description,
                      InternshipLevel level,
                      String preferredMajor,
                      LocalDate openDate,
                      LocalDate closeDate,
                      String companyName,
                      int maxSlots) {

        this.internshipId = "INT" + String.format("%04d", idCounter++);  // CHANGED THIS
        this.title = title;
        this.description = description;
        this.level = level;
        this.preferredMajor = preferredMajor;
        this.openDate = openDate;
        this.closeDate = closeDate;
        this.companyName = companyName;
        this.maxSlots = maxSlots;
    }

    // REST OF YOUR EXISTING METHODS REMAIN EXACTLY THE SAME...
   /**
     * Returns the unique internship identifier.
     * 
     * @return the internship ID (format: "INTXXXX")
     */
    public String getInternshipId() { return internshipId; }
    
    /**
     * Returns the internship position title.
     * 
     * @return the title
     */
    public String getTitle() { return title; }
    
    /**
     * Returns the detailed description of the internship.
     * 
     * @return the description
     */
    public String getDescription() { return description; }
    
    /**
     * Returns the difficulty level of the internship.
     * 
     * @return the level (BASIC, INTERMEDIATE, or ADVANCED)
     */
    public InternshipLevel getLevel() { return level; }
    
    /**
     * Returns the preferred major for applicants.
     * 
     * @return the preferred major (e.g., "CSC", "Any")
     */
    public String getPreferredMajor() { return preferredMajor; }
    
    /**
     * Returns the first date students can apply.
     * 
     * @return the opening date
     */
    public LocalDate getOpenDate() { return openDate; }
    
    /**
     * Returns the last date students can apply.
     * 
     * @return the closing date
     */
    public LocalDate getCloseDate() { return closeDate; }
    
    /**
     * Returns the name of the company offering this internship.
     * 
     * @return the company name
     */
    public String getCompanyName() { return companyName; }
    
    /**
     * Returns the maximum number of students that can be accepted.
     * 
     * @return the maximum slots
     */
    public int getMaxSlots() { return maxSlots; }
    
    /**
     * Returns the current number of confirmed acceptances.
     * 
     * @return the number of confirmed slots
     */
    public int getConfirmedSlots() { return confirmedSlots; }
    
    /**
     * Checks if the internship is currently visible to students.
     * 
     * @return {@code true} if visible, {@code false} otherwise
     */
    public boolean isVisible() { return visible; }
    
    /**
     * Returns the current status of the internship.
     * 
     * @return the status (PENDING, APPROVED, REJECTED, or FILLED)
     */
    public InternshipStatus getStatus() { return status; }

    /**
     * Approves this internship, allowing it to be made visible to students.
     * 
     * <p>Called by career center staff to approve a PENDING internship.
     * Idempotent - calling on an already APPROVED internship has no effect.</p>
     */
    public void approve() {
        if (status == InternshipStatus.APPROVED) return;
        status = InternshipStatus.APPROVED;
    }

    /**
     * Rejects this internship and makes it non-visible to students.
     * 
     * <p>Called by career center staff to reject an internship.
     * Automatically sets visibility to false.</p>
     */
    public void reject() {
        status = InternshipStatus.REJECTED;
        visible = false;
    }

    /**
     * Sets the visibility of this internship to students.
     * 
     * <p>Only APPROVED internships can be made visible. Attempting to make
     * a non-approved internship visible will throw an exception.</p>
     * 
     * <p>Company representatives use this to publish or hide their approved postings.</p>
     * 
     * @param visible {@code true} to make visible to students, {@code false} to hide
     * @throws IllegalStateException if attempting to make a non-approved internship visible
     */
    public void setVisible(boolean visible) {
        if (status != InternshipStatus.APPROVED && visible) {
            throw new IllegalStateException("Only approved internships can be made visible.");
        }
        this.visible = visible;
    }

    /**
     * Checks if the internship is currently open for student applications.
     * 
     * <p>An internship is open for applications if ALL conditions are met:</p>
     * <ul>
     *   <li>Status is APPROVED</li>
     *   <li>Visible flag is true</li>
     *   <li>Current date is within the application window (openDate to closeDate)</li>
     *   <li>Not all slots are confirmed (confirmedSlots &lt; maxSlots)</li>
     * </ul>
     * 
     * @param now the current date to check against
     * @return {@code true} if open for applications, {@code false} otherwise
     */
    public boolean isOpenForApplications(LocalDate now) {
        if (status != InternshipStatus.APPROVED || !visible) return false;
        return (now != null && !now.isBefore(openDate) && !now.isAfter(closeDate) && confirmedSlots < maxSlots);
    }

    /**
     * Increments the confirmed slots count when a student accepts an offer.
     * 
     * <p>Automatically changes status to FILLED when all slots are confirmed.
     * This prevents additional applications once capacity is reached.</p>
     * 
     * @throws IllegalStateException if no remaining slots available
     */
    public void incrementConfirmedSlots() {
        if (confirmedSlots >= maxSlots) {
            status = InternshipStatus.FILLED;
            throw new IllegalStateException("No remaining slots.");
        }
        confirmedSlots++;
        if (confirmedSlots >= maxSlots) {
            status = InternshipStatus.FILLED;
        }
    }

    /**
     * Decrements the confirmed slots count when a placement is released.
     * 
     * <p>Called when a withdrawal is approved, freeing up a slot.
     * Automatically reverts status from FILLED to APPROVED if slots become available.</p>
     */
    public void decrementConfirmedSlots() {
        if (confirmedSlots > 0) confirmedSlots--;
        if (status == InternshipStatus.FILLED && confirmedSlots < maxSlots) {
            status = InternshipStatus.APPROVED;
        }
    }

    /**
     * Checks if this internship can be edited by the company representative.
     * 
     * <p>Internships can only be edited while in PENDING status.
     * Once approved, rejected, or filled, editing is not allowed to maintain
     * system integrity.</p>
     * 
     * @return {@code true} if editable (PENDING status), {@code false} otherwise
     */
    public boolean isEditable() {
        return status == InternshipStatus.PENDING;
    }

    /**
     * Checks if this internship can be deleted by the company representative.
     * 
     * <p>Internships can only be deleted if they are in PENDING or REJECTED status.
     * Approved or filled internships cannot be deleted to preserve application history.</p>
     * 
     * @return {@code true} if deletable (PENDING or REJECTED), {@code false} otherwise
     */
    public boolean canBeDeleted() {
        return status == InternshipStatus.PENDING || status == InternshipStatus.REJECTED;
    }
}

//    @Override
//    public String toString() {
//        return String.format("%s [%s] %s (%s → %s) status=%s visible=%s slots=%d/%d",
//                title, level, companyName, openDate, closeDate, status, visible, confirmedSlots, maxSlots);
//    }
