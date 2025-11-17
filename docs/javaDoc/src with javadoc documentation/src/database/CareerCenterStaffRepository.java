package database;

import entity.CareerCenterStaff;
import util.CsvUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * In-memory repository for CareerCenterStaff entities with CSV persistence.
 * 
 * <p>This repository manages staff member accounts who have administrative privileges
 * to oversee the internship management system. Staff members are responsible for
 * maintaining system integrity and ensuring quality control.
 * 
 * <p><strong>Staff Responsibilities:</strong>
 * <ul>
 *   <li><strong>Quality Control:</strong> Review and approve/reject internship postings</li>
 *   <li><strong>Access Management:</strong> Approve/reject company representative registrations</li>
 *   <li><strong>Student Protection:</strong> Process and decide on withdrawal requests</li>
 *   <li><strong>System Oversight:</strong> Monitor all activities and maintain data integrity</li>
 * </ul>
 * 
 * <p><strong>Staff Data Includes:</strong>
 * <ul>
 *   <li><strong>User ID:</strong> Unique staff identifier (e.g., "STAFF001")</li>
 *   <li><strong>Personal Info:</strong> Name, email address</li>
 *   <li><strong>Role/Position:</strong> Job title or role within career center</li>
 *   <li><strong>Department:</strong> Department or division within career center</li>
 *   <li><strong>Authentication:</strong> Password for system login</li>
 * </ul>
 * 
 * <p><strong>Storage Mechanism:</strong>
 * Uses LinkedHashMap to maintain insertion order for stable CSV exports
 * and predictable iteration during staff listing operations.
 * 
 * <p><strong>CSV Format:</strong>
 * <pre>
 * Header: StaffID,Name,Role,Department,Email,Password
 * Example: STAFF001,Jane Doe,Senior Advisor,Career Services,jane@university.edu,password123
 * </pre>
 * 
 * <p><strong>CSV Features:</strong>
 * <ul>
 *   <li>Flexible header recognition with multiple column name variations</li>
 *   <li>Default values for missing non-critical fields</li>
 *   <li>Automatic password assignment if not provided</li>
 *   <li>Graceful error handling with detailed logging</li>
 * </ul>
 * 
 * @see CareerCenterStaff
 * @see CrudRepository
 */
public class CareerCenterStaffRepository implements CrudRepository<CareerCenterStaff, String> {

    /**
     * Internal storage map for staff members, keyed by user ID.
     * 
     * <p>LinkedHashMap maintains insertion order for stable iteration
     * and predictable CSV export ordering.
     */
    private final Map<String, CareerCenterStaff> map = new LinkedHashMap<>();

    // ---------- CrudRepository ----------

    /**
     * {@inheritDoc}
     * 
     * <p>Retrieves a staff member by their unique user ID.
     * 
     * @param id the staff member's user ID (e.g., "STAFF001")
     * @return an Optional containing the staff member if found, empty otherwise
     */
    @Override
    public Optional<CareerCenterStaff> findById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Returns all staff members in the order they were added.
     */
    @Override
    public List<CareerCenterStaff> findAll() {
        return new ArrayList<>(map.values());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Saves a staff member (insert or update). If a staff member
     * with the same user ID exists, it will be replaced.
     * 
     * @param s the staff member to save
     * @return the saved staff member
     */
    @Override
    public CareerCenterStaff save(CareerCenterStaff s) {
        map.put(s.getUserId(), s); // upsert
        return s;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Saves multiple staff members in a single operation.
     */
    @Override
    public List<CareerCenterStaff> saveAll(Iterable<CareerCenterStaff> entities) {
        List<CareerCenterStaff> out = new ArrayList<>();
        for (CareerCenterStaff e : entities) { save(e); out.add(e); }
        return out;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Removes a staff member by their user ID.
     */
    @Override
    public void deleteById(String id) { map.remove(id); }

    /**
     * {@inheritDoc}
     * 
     * <p>Removes multiple staff members by their user IDs.
     */
    @Override
    public void deleteAllById(Iterable<String> ids) {
        for (String id : ids) map.remove(id);
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Warning:</strong> This removes all staff data from the repository.
     */
    @Override
    public void deleteAll() { map.clear(); }

    /**
     * {@inheritDoc}
     * 
     * <p>Checks if a staff member with the specified user ID exists.
     */
    @Override
    public boolean existsById(String id) { return map.containsKey(id); }

    /**
     * {@inheritDoc}
     * 
     * <p>Returns the total number of staff members in the repository.
     */
    @Override
    public long count() { return map.size(); }

    // ---------- CSV I/O ----------

    /**
     * Predefined column name variations for Staff ID field.
     * Used for flexible CSV header matching.
     */
    private static final String[] COL_ID   = {"staffid","user id","userid","id","staff id","ntu id","ntu account"};
    
    /**
     * Predefined column name variations for Name field.
     */
    private static final String[] COL_NAME = {"name","full name"};
    
    /**
     * Predefined column name variations for Role/Position field.
     */
    private static final String[] COL_ROLE = {"role","position","title"};
    
    /**
     * Predefined column name variations for Department field.
     */
    private static final String[] COL_DEPT = {"department","dept"};
    
    /**
     * Predefined column name variations for Email field.
     */
    private static final String[] COL_MAIL = {"email","mail"};
    
    /**
     * Predefined column name variations for Password field.
     */
    private static final String[] COL_PASS = {"password","pwd"};

    /**
     * Loads staff member data from a CSV file.
     * 
     * <p><strong>CSV Format Expected:</strong>
     * <pre>
     * Header: StaffID,Name,Role,Department,Email,Password
     * Row:    STAFF001,Jane Doe,Senior Advisor,Career Services,jane@university.edu,password123
     * </pre>
     * 
     * <p><strong>Flexible Header Recognition:</strong>
     * The loader accepts multiple variations of column names for each field:
     * <ul>
     *   <li><strong>Staff ID:</strong> "staffid", "user id", "userid", "id", "staff id", "ntu id", "ntu account"</li>
     *   <li><strong>Name:</strong> "name", "full name"</li>
     *   <li><strong>Role:</strong> "role", "position", "title"</li>
     *   <li><strong>Department:</strong> "department", "dept"</li>
     *   <li><strong>Email:</strong> "email", "mail"</li>
     *   <li><strong>Password:</strong> "password", "pwd"</li>
     * </ul>
     * 
     * <p><strong>Default Values:</strong>
     * If certain fields are missing, sensible defaults are applied:
     * <ul>
     *   <li>Missing name → empty string ""</li>
     *   <li>Missing role → "Staff"</li>
     *   <li>Missing department → empty string ""</li>
     *   <li>Missing email → empty string ""</li>
     *   <li>Missing password → "password"</li>
     * </ul>
     * 
     * <p><strong>Validation:</strong>
     * <ul>
     *   <li>Staff ID is required - lines without a valid ID are skipped</li>
     *   <li>All other fields are optional with defaults applied</li>
     * </ul>
     * 
     * <p><strong>Error Handling:</strong>
     * <ul>
     *   <li>File not found: Prints message and returns without error</li>
     *   <li>Missing staff ID: Skips line with error message</li>
     *   <li>Parse errors: Skips problematic line and continues</li>
     *   <li>All errors logged to stderr with line numbers</li>
     * </ul>
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Check if file exists (return if not found)</li>
     *   <li>Read and parse header line to create column index</li>
     *   <li>For each data line:
     *     <ul>
     *       <li>Extract all field values using flexible column matching</li>
     *       <li>Validate that user ID is present</li>
     *       <li>Apply default values for missing fields</li>
     *       <li>Create CareerCenterStaff entity</li>
     *       <li>Set password (from CSV or default "password")</li>
     *       <li>Save to repository</li>
     *     </ul>
     *   </li>
     *   <li>Log any errors and continue with remaining lines</li>
     * </ol>
     * 
     * @param path the file path to the CSV file
     * @throws IOException if there is an error reading the file
     */
    public void loadFromCsv(String path) throws IOException {
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            System.out.println("Staff file not found: " + path);
            return;
        }

        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String headerLine = br.readLine();
            if (headerLine == null) return;

            String[] headers = CsvUtils.splitCsv(headerLine);
            Map<String,Integer> idx = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i] != null ? headers[i].trim().toLowerCase() : "";
                idx.put(header, i);
            }

            String line; 
            int lineNo = 1;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;

                String[] c = CsvUtils.splitCsv(line);
                try {
                    String userId = getCsvValue(c, idx, COL_ID);
                    String name   = getCsvValue(c, idx, COL_NAME);
                    String role   = getCsvValue(c, idx, COL_ROLE);
                    String dept   = getCsvValue(c, idx, COL_DEPT);
                    String email  = getCsvValue(c, idx, COL_MAIL);
                    String password = getCsvValue(c, idx, COL_PASS);

                    if (userId == null) {
                        System.err.println("Staff CSV skip line " + lineNo + ": missing user ID");
                        continue;
                    }

                    CareerCenterStaff staff = new CareerCenterStaff(
                        userId.trim(), 
                        name != null ? name.trim() : "", 
                        role != null ? role.trim() : "Staff", 
                        dept != null ? dept.trim() : "", 
                        email != null ? email.trim() : ""
                    );
                    
                    // Set password (provided or default)
                    if (password != null && !password.isEmpty()) {
                        staff.setPassword(password);
                    } else {
                        staff.setPassword("password");
                    }

                    save(staff);
                } catch (Exception ex) {
                    System.err.println("Staff CSV skip line " + lineNo + ": " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Saves all staff members to a CSV file.
     * 
     * <p><strong>CSV Format Generated:</strong>
     * <pre>
     * Header: StaffID,Name,Role,Department,Email,Password
     * Row:    STAFF001,Jane Doe,Senior Advisor,Career Services,jane@university.edu,password123
     * </pre>
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Create parent directories if they don't exist</li>
     *   <li>Open file for writing (creates new or truncates existing)</li>
     *   <li>Write CSV header line</li>
     *   <li>For each staff member in insertion order:
     *     <ul>
     *       <li>Escape special characters in all string fields</li>
     *       <li>Write comma-separated values</li>
     *     </ul>
     *   </li>
     *   <li>Close file</li>
     * </ol>
     * 
     * <p><strong>Data Integrity:</strong>
     * <ul>
     *   <li>Uses CsvUtils.esc() to properly escape commas, quotes, and newlines</li>
     *   <li>UTF-8 encoding preserves international characters</li>
     *   <li>Includes passwords for complete backup</li>
     *   <li>Maintains stable order via LinkedHashMap</li>
     * </ul>
     * 
     * @param path the file path where the CSV will be written
     * @throws IOException if there is an error writing the file or creating directories
     */
    public void saveToCsv(String path) throws IOException {
        Path p = Paths.get(path);
        Files.createDirectories(p.getParent() == null ? Paths.get(".") : p.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(p, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write("StaffID,Name,Role,Department,Email,Password");
            bw.newLine();
            for (CareerCenterStaff s : map.values()) {
                bw.write(String.join(",",
                    CsvUtils.esc(s.getUserId()),
                    CsvUtils.esc(s.getName()),
                    CsvUtils.esc(s.getRole()),
                    CsvUtils.esc(s.getDepartment()),
                    CsvUtils.esc(s.getEmail()),
                    CsvUtils.esc(s.getPassword())
                ));
                bw.newLine();
            }
        }
    }

    // ---------- Domain helpers ----------
    
    /**
     * Finds all staff members in a specific department.
     * 
     * <p>Performs case-insensitive matching on the department name,
     * allowing queries to work regardless of capitalization.
     * 
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Finding all staff in a specific division for task assignment</li>
     *   <li>Generating department-specific reports or statistics</li>
     *   <li>Organizing staff by functional area</li>
     * </ul>
     * 
     * @param department the name of the department to search for (case-insensitive)
     * @return a list of all staff members in the specified department
     *         (empty list if none found or department is null)
     */
    public List<CareerCenterStaff> findByDepartment(String department) {
        if (department == null) return List.of();
        String d = department.trim();
        List<CareerCenterStaff> out = new ArrayList<>();
        for (var s : map.values()) if (s.getDepartment().equalsIgnoreCase(d)) out.add(s);
        return out;
    }

    /**
     * Helper method to retrieve CSV cell values with flexible header name matching.
     * 
     * <p>Enables the CSV loader to work with different file formats by accepting
     * multiple possible column names for each field. This method is reused across
     * all field extractions during CSV loading.
     * 
     * <p><strong>Search Strategy:</strong>
     * <ol>
     *   <li>Tries each possible header name in the order provided</li>
     *   <li>Performs case-insensitive lookup in the header index</li>
     *   <li>Returns the first matching non-empty value found</li>
     *   <li>Returns null if no match found or all values are empty</li>
     * </ol>
     * 
     * @param row the array of cell values from a CSV line
     * @param idx the header-to-index mapping (header names in lowercase)
     * @param possibleHeaders array of possible column names to try
     * @return the cell value if found and non-empty, null otherwise
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