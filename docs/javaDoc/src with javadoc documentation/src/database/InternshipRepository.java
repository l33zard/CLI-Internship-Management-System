package database;

import entity.Internship;
import entity.InternshipLevel;
import entity.InternshipStatus;
import util.CsvUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.BufferedReader;

/**
 * In-memory repository for Internship entities with comprehensive query capabilities and CSV persistence.
 * 
 * <p>This repository manages internship postings through their complete lifecycle from creation
 * through approval, publication, and fulfillment. It provides rich querying capabilities for
 * filtering internships by various criteria and handles complex state management.
 * 
 * <p><strong>Internship Lifecycle States:</strong>
 * <ul>
 *   <li><strong>PENDING:</strong> Newly created by company, awaiting staff approval</li>
 *   <li><strong>APPROVED:</strong> Approved by staff, can be made visible to students</li>
 *   <li><strong>REJECTED:</strong> Denied by staff, not shown to students</li>
 *   <li><strong>FILLED:</strong> All available slots have been confirmed (auto-set when slots full)</li>
 * </ul>
 * 
 * <p><strong>Visibility Control:</strong>
 * Internships have a separate visibility flag independent of approval status.
 * An approved internship can be visible (shown to students) or hidden (not shown).
 * This allows companies and staff to control when postings go live.
 * 
 * <p><strong>Slot Management:</strong>
 * <ul>
 *   <li><strong>maxSlots:</strong> Maximum number of students that can be accepted</li>
 *   <li><strong>confirmedSlots:</strong> Number of students who have accepted offers</li>
 *   <li><strong>FILLED status:</strong> Automatically set when confirmedSlots == maxSlots</li>
 * </ul>
 * 
 * <p><strong>Query Capabilities:</strong>
 * Supports filtering by:
 * <ul>
 *   <li>Status (PENDING, APPROVED, REJECTED, FILLED)</li>
 *   <li>Level (FRESHMAN, SOPHOMORE, JUNIOR, SENIOR, BASIC)</li>
 *   <li>Preferred major (case-insensitive)</li>
 *   <li>Company name (case-insensitive)</li>
 *   <li>Date ranges (open/close dates)</li>
 *   <li>Visibility status</li>
 *   <li>Availability (has remaining slots)</li>
 * </ul>
 * 
 * <p><strong>CSV Persistence Features:</strong>
 * <ul>
 *   <li>Preserves internship IDs using Java reflection</li>
 *   <li>Restores ID counter to continue from highest loaded ID</li>
 *   <li>Handles all status states including FILLED</li>
 *   <li>Restores visibility, slot counts, and approval states</li>
 * </ul>
 * 
 * <p><strong>CSV Format:</strong>
 * <pre>
 * Header: InternshipID,Title,Description,Level,PreferredMajor,OpenDate,CloseDate,Status,CompanyName,MaxSlots,ConfirmedSlots,Visible
 * Example: INT001,Software Intern,Java development,JUNIOR,Computer Science,2025-01-01,2025-03-01,APPROVED,Tech Corp,5,2,true
 * </pre>
 * 
 * @see Internship
 * @see InternshipStatus
 * @see InternshipLevel
 * @see CrudRepository
 */
public class InternshipRepository implements CrudRepository<Internship, String> {

    // Use LinkedHashMap for stable iteration/export order.
    private final Map<String, Internship> map = new LinkedHashMap<>();

    // ---------- CrudRepository implementation ----------

    @Override
    public Optional<Internship> findById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    @Override
    public List<Internship> findAll() {
        return new ArrayList<>(map.values());
    }

    @Override
    public Internship save(Internship entity) {
        map.put(entity.getInternshipId(), entity); // upsert
        return entity;
    }

    @Override
    public List<Internship> saveAll(Iterable<Internship> entities) {
        List<Internship> out = new ArrayList<>();
        for (Internship i : entities) {
            save(i);
            out.add(i);
        }
        return out;
    }

    @Override
    public void deleteById(String id) {
        map.remove(id);
    }

    @Override
    public void deleteAllById(Iterable<String> ids) {
        for (String id : ids) map.remove(id);
    }

    @Override
    public void deleteAll() {
        map.clear();
    }

    @Override
    public boolean existsById(String id) {
        return map.containsKey(id);
    }

    @Override
    public long count() {
        return map.size();
    }

    // ---------- Domain finders / helpers ----------

    /** Find internships with a specific status. */
    public List<Internship> findByStatus(InternshipStatus status) {
        if (status == null) return List.of();
        return map.values().stream()
                .filter(i -> i.getStatus() == status)
                .collect(Collectors.toList());
    }

    /** Find internships of a given level. */
    public List<Internship> findByLevel(InternshipLevel level) {
        if (level == null) return List.of();
        return map.values().stream()
                .filter(i -> i.getLevel() == level)
                .collect(Collectors.toList());
    }

    /** Find internships for a particular major (case-insensitive). */
    public List<Internship> findByPreferredMajor(String major) {
        if (major == null) return List.of();
        String m = major.trim();
        return map.values().stream()
                .filter(i -> i.getPreferredMajor().equalsIgnoreCase(m))
                .collect(Collectors.toList());
    }

    /** List all internships that are open (date/status/slots) and visible for a given date. */
    public List<Internship> findOpenVisibleForDate(LocalDate date) {
        LocalDate d = (date == null) ? LocalDate.now() : date;
        return map.values().stream()
                .filter(i -> i.isOpenForApplications(d))
                .collect(Collectors.toList());
    }

    /** List all visible internships (regardless of date range/capacity). */
    public List<Internship> findAllVisible() {
        return map.values().stream()
                .filter(Internship::isVisible)
                .collect(Collectors.toList());
    }

    /** Count all internships currently approved (any visibility). */
    public long countApproved() {
        return map.values().stream()
                .filter(i -> i.getStatus() == InternshipStatus.APPROVED)
                .count();
    }

    /** All postings by company name (case-insensitive). */
    public List<Internship> findByCompanyName(String companyName) {
        if (companyName == null) return List.of();
        String c = companyName.trim();
        return map.values().stream()
                .filter(i -> i.getCompanyName().equalsIgnoreCase(c))
                .collect(Collectors.toList());
    }

    /** "Active" postings by company — count only PENDING or APPROVED. */
    public List<Internship> findActiveByCompanyName(String companyName) {
        if (companyName == null) return List.of();
        String c = companyName.trim();
        return map.values().stream()
            .filter(i -> i.getCompanyName().equalsIgnoreCase(c))
            .filter(i -> i.getStatus() == InternshipStatus.PENDING || i.getStatus() == InternshipStatus.APPROVED)
            .collect(Collectors.toList());
    }

    /** Save ALL internships to CSV (regardless of status) */
    public void saveToCsv(String path) throws java.io.IOException {
        Path p = Paths.get(path);
        Files.createDirectories(p.getParent() == null ? Paths.get(".") : p.getParent());
        try (var bw = Files.newBufferedWriter(p, StandardCharsets.UTF_8, 
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write("InternshipID,Title,Description,Level,PreferredMajor,OpenDate,CloseDate,Status,CompanyName,MaxSlots,ConfirmedSlots,Visible");
            bw.newLine();
            for (var i : map.values()) {
                bw.write(String.join(",",
                    CsvUtils.esc(i.getInternshipId()),
                    CsvUtils.esc(i.getTitle()),
                    CsvUtils.esc(i.getDescription()),
                    CsvUtils.esc(i.getLevel() == null ? "" : i.getLevel().name()),
                    CsvUtils.esc(i.getPreferredMajor()),
                    CsvUtils.esc(String.valueOf(i.getOpenDate())),
                    CsvUtils.esc(String.valueOf(i.getCloseDate())),
                    CsvUtils.esc(i.getStatus() == null ? "" : i.getStatus().name()),
                    CsvUtils.esc(i.getCompanyName()),
                    String.valueOf(i.getMaxSlots()),
                    String.valueOf(i.getConfirmedSlots()),
                    String.valueOf(i.isVisible())
                ));
                bw.newLine();
            }
        }
    }

    /** Load ALL internships from CSV (any status) */
    public void loadFromCsv(String path) throws java.io.IOException {
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            System.out.println("Internships file not found: " + path);
            return;
        }

        int maxId = 0; // Track highest ID for counter

        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String headerLine = br.readLine();
            if (headerLine == null) return;

            String[] headers = CsvUtils.splitCsv(headerLine);
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i] != null ? headers[i].trim().toLowerCase() : "";
                idx.put(header, i);
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] row = CsvUtils.splitCsv(line);

                try {
                    String internshipId = getCsvValue(row, idx, "internshipid", "id");
                    String title = getCsvValue(row, idx, "title");
                    String desc = getCsvValue(row, idx, "description","desc");
                    String levelS = getCsvValue(row, idx, "level");
                    String major = getCsvValue(row, idx, "preferredmajor","major");
                    String openS = getCsvValue(row, idx, "opendate","open");
                    String closeS = getCsvValue(row, idx, "closedate","close");
                    String statusS = getCsvValue(row, idx, "status");
                    String company = getCsvValue(row, idx, "companyname","company");
                    String maxS = getCsvValue(row, idx, "maxslots","max");
                    String confS = getCsvValue(row, idx, "confirmedslots","confirmed");
                    String visS = getCsvValue(row, idx, "visible","visibility");

                    // Handle null values with defaults
                    if (title == null || company == null) {
                        System.err.println("Skipping internship - missing required fields");
                        continue;
                    }

                    InternshipLevel level = (levelS == null || levelS.isBlank())
                            ? InternshipLevel.BASIC
                            : InternshipLevel.valueOf(levelS.trim().toUpperCase());

                    LocalDate open = (openS == null || openS.isBlank())
                            ? LocalDate.now() : LocalDate.parse(openS.trim());
                    LocalDate close = (closeS == null || closeS.isBlank())
                            ? open.plusMonths(1) : LocalDate.parse(closeS.trim());

                    int maxSlots = (maxS == null || maxS.isBlank()) ? 1 : Integer.parseInt(maxS.trim());
                    int confirmed = (confS == null || confS.isBlank()) ? 0 : Integer.parseInt(confS.trim());
                    boolean visible = visS != null && visS.equalsIgnoreCase("true");

                    InternshipStatus status = (statusS == null || statusS.isBlank())
                            ? InternshipStatus.PENDING
                            : InternshipStatus.valueOf(statusS.trim().toUpperCase());

                    // Create internship - we'll manually set the ID
                    Internship internship = new Internship(title, desc, level, major, open, close, company, maxSlots);
                    
                    // Set the original ID from CSV
                    if (internshipId != null && !internshipId.isEmpty()) {
                        // Use reflection to set the ID to preserve it
                        try {
                            java.lang.reflect.Field idField = Internship.class.getDeclaredField("internshipId");
                            idField.setAccessible(true);
                            idField.set(internship, internshipId);
                            
                            // Track highest ID for counter
                            if (internshipId.startsWith("INT")) {
                                String numericPart = internshipId.substring(3);
                                try {
                                    int idNum = Integer.parseInt(numericPart);
                                    if (idNum > maxId) {
                                        maxId = idNum;
                                    }
                                } catch (NumberFormatException e) {
                                    // Ignore if not numeric
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Warning: Could not preserve internship ID: " + internshipId);
                        }
                    }

                    // Restore status and properties
                    if (status == InternshipStatus.APPROVED) {
                        internship.approve();
                        if (visible) {
                            try { internship.setVisible(true); } catch (Exception ignore) {}
                        }
                    } else if (status == InternshipStatus.REJECTED) {
                        internship.reject();
                    } else if (status == InternshipStatus.FILLED) {
                        internship.approve();
                        // Set confirmed slots to max to simulate filled status
                        for (int i = 0; i < maxSlots; i++) {
                            try { internship.incrementConfirmedSlots(); } catch (Exception ignore) {}
                        }
                    }
                    // REMOVED: CLOSED status handling since close() method doesn't exist

                    // Set confirmed slots (this handles partial fills)
                    int currentConfirmed = internship.getConfirmedSlots();
                    if (confirmed > currentConfirmed) {
                        for (int i = currentConfirmed; i < confirmed; i++) {
                            try { internship.incrementConfirmedSlots(); } catch (Exception ignore) {}
                        }
                    }

                    save(internship);
                } catch (Exception e) {
                    System.err.println("Error parsing internship line: " + e.getMessage());
                }
            }
            
            // Set the ID counter to continue from the highest loaded ID
            if (maxId > 0) {
                try {
                    java.lang.reflect.Field counterField = Internship.class.getDeclaredField("idCounter");
                    counterField.setAccessible(true);
                    counterField.set(null, maxId + 1);
                } catch (Exception e) {
                    System.err.println("Warning: Could not update internship ID counter");
                }
            }
        }
    }

    // Helper method to get CSV values with multiple possible header names
    private String getCsvValue(String[] row, Map<String, Integer> idx, String... possibleHeaders) {
        for (String header : possibleHeaders) {
            Integer index = idx.get(header.toLowerCase());
            if (index != null && index < row.length && row[index] != null) {
                String value = row[index].trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }
}