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
 * In-memory repository for managing {@link entity.Internship} entities.
 *
 * <p>This repository provides CRUD operations for internship postings along with
 * domain-specific query methods for filtering by status, level, major, company,
 * and visibility. It also supports CSV-based persistence for application data storage.
 *
 * <p>The repository maintains internships in insertion order using a {@code LinkedHashMap}
 * for predictable iteration and export order. It includes sophisticated CSV loading
 * that preserves internship IDs and maintains the ID counter state.
 *
 * <p><b>Thread Safety:</b> This implementation is not thread-safe. External synchronization
 * is required if used in concurrent environments.
 *
 */
public class InternshipRepository implements CrudRepository<Internship, String> {

    // Use LinkedHashMap for stable iteration/export order.
    private final Map<String, Internship> map = new LinkedHashMap<>();

    // ---------- CrudRepository implementation ----------

    /**
     * Finds an internship by its unique identifier.
     *
     * @param id the internship ID to search for
     * @return an {@code Optional} containing the found internship, or empty if not found
     * @throws NullPointerException if the id is null
     */
    @Override
    public Optional<Internship> findById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    /**
     * Retrieves all internships in the repository.
     *
     * @return a list of all internships, maintaining insertion order
     */
    @Override
    public List<Internship> findAll() {
        return new ArrayList<>(map.values());
    }

    /**
     * Saves an internship to the repository.
     *
     * <p>If an internship with the same ID already exists, it will be replaced.
     *
     * @param entity the internship to save
     * @return the saved internship
     * @throws NullPointerException if the internship is null
     */
    @Override
    public Internship save(Internship entity) {
        map.put(entity.getInternshipId(), entity); // upsert
        return entity;
    }

    /**
     * Saves all internships in the provided iterable.
     *
     * @param entities the internships to save
     * @return a list of the saved internships
     * @throws NullPointerException if the iterable or any element is null
     */
    @Override
    public List<Internship> saveAll(Iterable<Internship> entities) {
        List<Internship> out = new ArrayList<>();
        for (Internship i : entities) {
            save(i);
            out.add(i);
        }
        return out;
    }

    /**
     * Deletes an internship by its ID.
     *
     * @param id the ID of the internship to delete
     * @throws NullPointerException if the id is null
     */
    @Override
    public void deleteById(String id) {
        map.remove(id);
    }

    /**
     * Deletes all internships with the specified IDs.
     *
     * @param ids the IDs of internships to delete
     * @throws NullPointerException if the iterable or any element is null
     */
    @Override
    public void deleteAllById(Iterable<String> ids) {
        for (String id : ids) map.remove(id);
    }

    /**
     * Deletes all internships from the repository.
     */
    @Override
    public void deleteAll() {
        map.clear();
    }

    /**
     * Checks whether an internship with the given ID exists.
     *
     * @param id the ID to check
     * @return true if an internship with the ID exists, false otherwise
     * @throws NullPointerException if the id is null
     */
    @Override
    public boolean existsById(String id) {
        return map.containsKey(id);
    }

    /**
     * Returns the total number of internships in the repository.
     *
     * @return the count of internships
     */
    @Override
    public long count() {
        return map.size();
    }

    // ---------- Domain finders / helpers ----------

    /**
     * Finds internships with a specific status.
     *
     * @param status the status to filter by
     * @return a list of internships with the specified status, or empty list if status is null
     * @see entity.InternshipStatus
     */
    public List<Internship> findByStatus(InternshipStatus status) {
        if (status == null) return List.of();
        return map.values().stream()
                .filter(i -> i.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * Finds internships of a given experience level.
     *
     * @param level the experience level to filter by
     * @return a list of internships with the specified level, or empty list if level is null
     * @see entity.InternshipLevel
     */
    public List<Internship> findByLevel(InternshipLevel level) {
        if (level == null) return List.of();
        return map.values().stream()
                .filter(i -> i.getLevel() == level)
                .collect(Collectors.toList());
    }

    /**
     * Finds internships for a particular major (case-insensitive).
     *
     * @param major the preferred major to filter by
     * @return a list of internships matching the specified major, or empty list if major is null
     */
    public List<Internship> findByPreferredMajor(String major) {
        if (major == null) return List.of();
        String m = major.trim();
        return map.values().stream()
                .filter(i -> i.getPreferredMajor().equalsIgnoreCase(m))
                .collect(Collectors.toList());
    }

    /**
     * Lists all internships that are open and visible for applications on a given date.
     *
     * <p>An internship is considered open if:
     * <ul>
     *   <li>The current date is within the open/close date range</li>
     *   <li>The internship status is APPROVED</li>
     *   <li>The internship has available slots</li>
     *   <li>The internship is marked as visible</li>
     * </ul>
     * 
     *
     * @param date the date to check for openness; uses current date if null
     * @return a list of open and visible internships
     * @see entity.Internship#isOpenForApplications(LocalDate)
     */
    public List<Internship> findOpenVisibleForDate(LocalDate date) {
        LocalDate d = (date == null) ? LocalDate.now() : date;
        return map.values().stream()
                .filter(i -> i.isOpenForApplications(d))
                .collect(Collectors.toList());
    }

    /**
     * Lists all visible internships regardless of date range or capacity.
     *
     * <p>This includes all APPROVED internships that are marked as visible,
     * even if they are outside their application period or full.
     *
     * @return a list of all visible internships
     * @see entity.Internship#isVisible()
     */
    public List<Internship> findAllVisible() {
        return map.values().stream()
                .filter(Internship::isVisible)
                .collect(Collectors.toList());
    }

    /**
     * Counts all internships that are currently approved (any visibility status).
     *
     * @return the count of approved internships
     */
    public long countApproved() {
        return map.values().stream()
                .filter(i -> i.getStatus() == InternshipStatus.APPROVED)
                .count();
    }

    /**
     * Finds all internships posted by a specific company (case-insensitive).
     *
     * @param companyName the company name to search for
     * @return a list of internships from the specified company, or empty list if companyName is null
     */
    public List<Internship> findByCompanyName(String companyName) {
        if (companyName == null) return List.of();
        String c = companyName.trim();
        return map.values().stream()
                .filter(i -> i.getCompanyName().equalsIgnoreCase(c))
                .collect(Collectors.toList());
    }

    /**
     * Finds active postings by a specific company.
     *
     * <p>Active postings are those with status PENDING or APPROVED.
     * This is useful for company representatives to see their active listings.
     *
     * @param companyName the company name to search for
     * @return a list of active internships from the specified company, or empty list if companyName is null
     */
    public List<Internship> findActiveByCompanyName(String companyName) {
        if (companyName == null) return List.of();
        String c = companyName.trim();
        return map.values().stream()
            .filter(i -> i.getCompanyName().equalsIgnoreCase(c))
            .filter(i -> i.getStatus() == InternshipStatus.PENDING || i.getStatus() == InternshipStatus.APPROVED)
            .collect(Collectors.toList());
    }

    /**
     * Saves all internships to a CSV file with standardized column headers.
     *
     * <p>The CSV file is created with the following columns in this exact order:
     * <ol>
     *   <li>InternshipID - Unique identifier (e.g., "INT001")</li>
     *   <li>Title - Internship position title</li>
     *   <li>Description - Detailed description of the internship</li>
     *   <li>Level - Experience level (BASIC, INTERMEDIATE, ADVANCED)</li>
     *   <li>PreferredMajor - Required or preferred academic major</li>
     *   <li>OpenDate - Start date for applications (YYYY-MM-DD)</li>
     *   <li>CloseDate - End date for applications (YYYY-MM-DD)</li>
     *   <li>Status - Current status (PENDING, APPROVED, REJECTED, FILLED)</li>
     *   <li>CompanyName - Name of the offering company</li>
     *   <li>MaxSlots - Maximum number of available positions</li>
     *   <li>ConfirmedSlots - Number of filled positions</li>
     *   <li>Visible - Visibility flag (true/false)</li>
     * </ol>
     * 
     *
     * <p>Parent directories are created automatically if they don't exist.
     * If the file already exists, it will be overwritten.
     *
     * @param path the output CSV file path
     * @throws java.io.IOException if an I/O error occurs while writing the file
     * @throws SecurityException if file access is denied
     */
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

    /**
     * Loads internships from a CSV file and populates the repository.
     *
     * <p>This method performs sophisticated CSV parsing with the following features:
     * <ul>
     *   <li>Flexible header recognition for common column name variations</li>
     *   <li>Preservation of original internship IDs using reflection</li>
     *   <li>Automatic ID counter adjustment to maintain sequential ID generation</li>
     *   <li>Default value handling for missing or invalid data</li>
     *   <li>Status restoration including FILLED status simulation</li>
     * </ul>
     * 
     *
     * <p>Default values applied during loading:
     * <ul>
     *   <li>Level: BASIC if not specified</li>
     *   <li>Open Date: Current date if not specified</li>
     *   <li>Close Date: Open date plus 1 month if not specified</li>
     *   <li>Max Slots: 1 if not specified</li>
     *   <li>Confirmed Slots: 0 if not specified</li>
     *   <li>Status: PENDING if not specified</li>
     * </ul>
     * 
     *
     * <p>Rows with missing title or company name are skipped with an error message.
     *
     * @param path the input CSV file path
     * @throws java.io.IOException if an I/O error occurs while reading the file
     * @throws SecurityException if file access is denied
     */
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

    /**
     * Extracts a value from a CSV row using flexible header matching.
     *
     * <p>This method tries each possible header name in order and returns the first
     * non-empty value found. Header matching is case-insensitive.
     *
     * @param row the CSV row data as a string array
     * @param idx a mapping of header names to column indices
     * @param possibleHeaders possible header names to try
     * @return the extracted value, or null if no matching header found or value is empty
     */
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