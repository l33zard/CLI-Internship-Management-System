package database;

import entity.CareerCenterStaff;
import util.CsvUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * In-memory repository for managing {@link entity.CareerCenterStaff} entities.
 *
 * <p>This repository provides CRUD operations for career center staff records and supports
 * CSV-based persistence for application bootstrap and exit persistence routines.
 *
 * <p>The repository maintains staff records in insertion order for predictable iteration
 * and exports. It implements flexible CSV header parsing to accommodate various column
 * naming conventions.
 *
 */
public class CareerCenterStaffRepository implements CrudRepository<CareerCenterStaff, String> {

    // Stable iteration/export order
    private final Map<String, CareerCenterStaff> map = new LinkedHashMap<>();

    // ---------- CrudRepository ----------

    /**
     * Finds a staff member by their unique user identifier.
     *
     * @param id the user ID to search for
     * @return an {@code Optional} containing the found staff member, or empty if not found
     * @throws NullPointerException if the id is null
     */
    @Override
    public Optional<CareerCenterStaff> findById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    /**
     * Retrieves all staff members in the repository.
     *
     * @return a list of all staff members, maintaining insertion order
     */
    @Override
    public List<CareerCenterStaff> findAll() {
        return new ArrayList<>(map.values());
    }

    /**
     * Saves a staff member to the repository.
     *
     * <p>If a staff member with the same user ID already exists, it will be replaced.
     *
     * @param s the staff member to save
     * @return the saved staff member
     * @throws NullPointerException if the staff member is null
     */
    @Override
    public CareerCenterStaff save(CareerCenterStaff s) {
        map.put(s.getUserId(), s); // upsert
        return s;
    }

    /**
     * Saves all staff members in the provided iterable.
     *
     * @param entities the staff members to save
     * @return a list of the saved staff members
     * @throws NullPointerException if the iterable or any element is null
     */
    @Override
    public List<CareerCenterStaff> saveAll(Iterable<CareerCenterStaff> entities) {
        List<CareerCenterStaff> out = new ArrayList<>();
        for (CareerCenterStaff e : entities) { save(e); out.add(e); }
        return out;
    }

    /**
     * Deletes a staff member by their user ID.
     *
     * @param id the user ID of the staff member to delete
     * @throws NullPointerException if the id is null
     */
    @Override
    public void deleteById(String id) { map.remove(id); }

    /**
     * Deletes all staff members with the specified user IDs.
     *
     * @param ids the user IDs of staff members to delete
     * @throws NullPointerException if the iterable or any element is null
     */
    @Override
    public void deleteAllById(Iterable<String> ids) {
        for (String id : ids) map.remove(id);
    }

    /**
     * Deletes all staff members from the repository.
     */
    @Override
    public void deleteAll() { map.clear(); }

    /**
     * Checks whether a staff member with the given user ID exists.
     *
     * @param id the user ID to check
     * @return true if a staff member with the ID exists, false otherwise
     * @throws NullPointerException if the id is null
     */
    @Override
    public boolean existsById(String id) { return map.containsKey(id); }

    /**
     * Returns the total number of staff members in the repository.
     *
     * @return the count of staff members
     */
    @Override
    public long count() { return map.size(); }

    // ---------- CSV I/O ----------

    // Flexible column header mappings for CSV parsing
    private static final String[] COL_ID   = {"staffid","user id","userid","id","staff id","ntu id","ntu account"};
    private static final String[] COL_NAME = {"name","full name"};
    private static final String[] COL_ROLE = {"role","position","title"};
    private static final String[] COL_DEPT = {"department","dept"};
    private static final String[] COL_MAIL = {"email","mail"};
    private static final String[] COL_PASS = {"password","pwd"};

    /**
     * Loads staff records from a CSV file with flexible header recognition.
     *
     * <p>The CSV file can use various column headers for each field:
     * <ul>
     *   <li><b>User ID:</b> "staffid", "user id", "userid", "id", "staff id", "ntu id", "ntu account"</li>
     *   <li><b>Name:</b> "name", "full name"</li>
     *   <li><b>Role:</b> "role", "position", "title"</li>
     *   <li><b>Department:</b> "department", "dept"</li>
     *   <li><b>Email:</b> "email", "mail"</li>
     *   <li><b>Password:</b> "password", "pwd"</li>
     * </ul>
     * 
     *
     * <p>If the user ID is missing from a row, that row is skipped and an error is logged.
     * Missing optional fields are set to empty strings or default values:
     * <ul>
     *   <li>Role defaults to "Staff" if not specified</li>
     *   <li>Password defaults to "password" if not specified</li>
     * </ul>
     * 
     *
     * @param path the input CSV file path
     * @throws IOException if an I/O error occurs while reading the file
     * @throws SecurityException if file access is denied
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
     * Saves all staff records to a CSV file with standardized column headers.
     *
     * <p>The CSV file is created with the following columns in this exact order:
     * <ol>
     *   <li>StaffID</li>
     *   <li>Name</li>
     *   <li>Role</li>
     *   <li>Department</li>
     *   <li>Email</li>
     *   <li>Password</li>
     * </ol>
     *
     * <p>Parent directories are created automatically if they don't exist.
     * If the file already exists, it will be overwritten.
     *
     * @param path the output CSV file path
     * @throws IOException if an I/O error occurs while writing the file
     * @throws SecurityException if file access is denied
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
     * <p>The department comparison is case-insensitive. If the department parameter
     * is null, an empty list is returned.
     *
     * @param department the department to search for
     * @return a list of staff members in the specified department, or empty list if none found
     */
    public List<CareerCenterStaff> findByDepartment(String department) {
        if (department == null) return List.of();
        String d = department.trim();
        List<CareerCenterStaff> out = new ArrayList<>();
        for (var s : map.values()) if (s.getDepartment().equalsIgnoreCase(d)) out.add(s);
        return out;
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