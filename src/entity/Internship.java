package entity;

import java.time.LocalDate;

/**
 * Represents an internship posting created by a company representative.
 * 
 * <p>This entity manages the complete lifecycle of an internship posting, from creation through
 * approval, visibility management, application processing, and eventual filling or closure.
 * Internships must be approved by career center staff before becoming visible to students.
 *
 * <p><b>Lifecycle States:</b>
 * <ul>
 *   <li><b>PENDING</b> - Initial state, awaiting staff approval</li>
 *   <li><b>APPROVED</b> - Approved by staff, can be made visible</li>
 *   <li><b>REJECTED</b> - Rejected by staff, cannot be made visible</li>
 *   <li><b>FILLED</b> - All available slots have been filled</li>
 * </ul>
 *
 */
public class Internship {

    private static int idCounter = 1;  // ADD THIS

    private final String internshipId;
    private String title;
    private String description;
    private InternshipLevel level;
    private String preferredMajor;
    private LocalDate openDate;
    private LocalDate closeDate;
    private String companyName;
    private int maxSlots;
    private int confirmedSlots = 0;
    private boolean visible = false;
    private InternshipStatus status = InternshipStatus.PENDING;

    /* ===================== Constructors ===================== */

    /**
     * Creates a new internship posting in PENDING status.
     * The internship will be assigned a unique auto-generated ID and starts as not visible.
     *
     * @param title the title of the internship posting
     * @param description detailed description of the internship
     * @param level the internship level (e.g., UNDERGRADUATE, GRADUATE)
     * @param preferredMajor the preferred major for applicants (optional, can be null)
     * @param openDate the first date when applications are accepted
     * @param closeDate the last date when applications are accepted
     * @param companyName the name of the company offering the internship
     * @param maxSlots the maximum number of confirmed placements available
     * @throws IllegalArgumentException if any required parameter is invalid
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

    // ---------- Accessor Methods ----------

    /**
     * Returns the unique identifier for this internship.
     *
     * @return the internship ID in format "INT####"
     */
    public String getInternshipId() { return internshipId; }

    /**
     * Returns the title of the internship posting.
     *
     * @return the internship title
     */
    public String getTitle() { return title; }

    /**
     * Returns the detailed description of the internship.
     *
     * @return the internship description
     */
    public String getDescription() { return description; }

    /**
     * Returns the internship level.
     *
     * @return the internship level
     */
    public InternshipLevel getLevel() { return level; }

    /**
     * Returns the preferred major for applicants.
     *
     * @return the preferred major, or null if not specified
     */
    public String getPreferredMajor() { return preferredMajor; }

    /**
     * Returns the date when applications open.
     *
     * @return the open date
     */
    public LocalDate getOpenDate() { return openDate; }

    /**
     * Returns the date when applications close.
     *
     * @return the close date
     */
    public LocalDate getCloseDate() { return closeDate; }

    /**
     * Returns the name of the company offering the internship.
     *
     * @return the company name
     */
    public String getCompanyName() { return companyName; }

    /**
     * Returns the maximum number of available slots.
     *
     * @return the maximum slots
     */
    public int getMaxSlots() { return maxSlots; }

    /**
     * Returns the current number of confirmed slots filled.
     *
     * @return the number of confirmed slots
     */
    public int getConfirmedSlots() { return confirmedSlots; }

    /**
     * Checks if the internship is currently visible to students.
     *
     * @return true if visible, false otherwise
     */
    public boolean isVisible() { return visible; }

    /**
     * Returns the current status of the internship.
     *
     * @return the internship status
     */
    public InternshipStatus getStatus() { return status; }

    // ---------- Status Management ----------

    /**
     * Approves this internship posting.
     * Changes status to APPROVED but does not automatically make it visible.
     * If already approved, this method has no effect.
     */
    public void approve() {
        if (status == InternshipStatus.APPROVED) return;
        status = InternshipStatus.APPROVED;
    }

    /**
     * Rejects this internship posting.
     * Changes status to REJECTED and automatically makes it not visible.
     */
    public void reject() {
        status = InternshipStatus.REJECTED;
        visible = false;
    }

    /**
     * Sets the visibility of this internship posting.
     * Only APPROVED internships can be made visible to students.
     *
     * @param visible true to make visible, false to hide
     * @throws IllegalStateException if attempting to make a non-approved internship visible
     */
    public void setVisible(boolean visible) {
        if (status != InternshipStatus.APPROVED && visible) {
            throw new IllegalStateException("Only approved internships can be made visible.");
        }
        this.visible = visible;
    }

    // ---------- Application Management ----------

    /**
     * Checks whether the internship is currently open for applications.
     * 
     * <p>An internship is open for applications when:
     * <ul>
     *   <li>Status is APPROVED</li>
     *   <li>Visible to students</li>
     *   <li>Current date is within open/close date range</li>
     *   <li>Has available slots (confirmed slots &lt; max slots)</li>
     * </ul>
     *
     * @param now the reference date to check against (typically today's date)
     * @return true if open for applications, false otherwise
     */
    public boolean isOpenForApplications(LocalDate now) {
        if (status != InternshipStatus.APPROVED || !visible) return false;
        return (now != null && !now.isBefore(openDate) && !now.isAfter(closeDate) && confirmedSlots < maxSlots);
    }

    /**
     * Reserves a confirmed slot for a student acceptance.
     * Increments the confirmed slot count and updates status to FILLED when the last slot is taken.
     *
     * @throws IllegalStateException if no slots remain (confirmedSlots >= maxSlots)
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
     * Releases a previously confirmed slot.
     * Decrements the confirmed slot count and updates status from FILLED back to APPROVED when slots become available.
     */
    public void decrementConfirmedSlots() {
        if (confirmedSlots > 0) confirmedSlots--;
        if (status == InternshipStatus.FILLED && confirmedSlots < maxSlots) {
            status = InternshipStatus.APPROVED;
        }
    }

    // ---------- Editability Checks ----------

    /**
     * Checks if this internship posting is editable.
     * Only PENDING internships can be edited.
     *
     * @return true if editable, false otherwise
     */
    public boolean isEditable() {
        return status == InternshipStatus.PENDING;
    }

    /**
     * Checks if this internship posting can be deleted.
     * Only PENDING or REJECTED internships can be deleted.
     *
     * @return true if deletable, false otherwise
     */
    public boolean canBeDeleted() {
        return status == InternshipStatus.PENDING || status == InternshipStatus.REJECTED;
    }

//    /**
//     * Returns a string representation of this internship.
//     *
//     * @return formatted string with key internship details
//     */
//    @Override
//    public String toString() {
//        return String.format("%s [%s] %s (%s → %s) status=%s visible=%s slots=%d/%d",
//                title, level, companyName, openDate, closeDate, status, visible, confirmedSlots, maxSlots);
//    }
}