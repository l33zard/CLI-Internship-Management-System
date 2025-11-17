package database;

import entity.Student;
import util.CsvUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * In-memory repository for Student entities with CSV persistence capabilities.
 * 
 * <p>This repository manages student account information including authentication
 * credentials, personal details, and academic information. It provides both
 * standard CRUD operations and domain-specific query methods for finding
 * students by their academic characteristics.
 * 
 * <p><strong>Student Data Includes:</strong>
 * <ul>
 *   <li><strong>User ID:</strong> Unique identifier in format U#######X (7 digits + 1 letter)</li>
 *   <li><strong>Personal Info:</strong> Name and email address</li>
 *   <li><strong>Academic Info:</strong> Major/programme and year of study (1-4)</li>
 *   <li><strong>Authentication:</strong> Password for system login</li>
 * </ul>
 * 
 * <p><strong>Storage Mechanism:</strong>
 * Uses LinkedHashMap to maintain insertion order for predictable iteration
 * and cleaner CSV file diffs during version control. This ensures students
 * appear in a consistent order when exported to CSV.
 * 
 * <p><strong>CSV Format:</strong>
 * <pre>
 * Header: StudentID,Name,Major,Year,Email,Password
 * Example: U1234567A,John Doe,Computer Science,3,john@example.com,password123
 * </pre>
 * 
 * <p><strong>CSV Features:</strong>
 * <ul>
 *   <li>Flexible header recognition (supports multiple column name variations)</li>
 *   <li>Student ID format validation (U#######X pattern)</li>
 *   <li>Default values for missing fields</li>
 *   <li>Automatic password setting (default "password" if not provided)</li>
 *   <li>Graceful error handling (skips invalid lines, continues loading)</li>
 * </ul>
 * 
 * @see Student
 * @see CrudRepository
 */
public class StudentRepository implements CrudRepository<Student, String> {

    /**
     * Internal storage map for students, keyed by user ID.
     * 
     * <p>LinkedHashMap maintains insertion order for stable CSV exports
     * and predictable iteration. This produces cleaner diffs in version
     * control when the CSV file is modified.
     */
    private final Map<String, Student> map = new LinkedHashMap<>();

    // ----- CrudRepository implementation -----

    /**
     * {@inheritDoc}
     * 
     * <p>Retrieves a student by their unique user ID.
     * 
     * @param id the student's user ID (e.g., "U1234567A")
     * @return an Optional containing the student if found, empty otherwise
     */
    @Override
    public Optional<Student> findById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Returns all students in the order they were added to the repository.
     */
    @Override
    public List<Student> findAll() {
        return new ArrayList<>(map.values());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Saves a student (insert or update). If a student with the same
     * user ID already exists, it will be replaced with the new entity.
     * 
     * @param entity the student to save
     * @return the saved student entity
     */
    @Override
    public Student save(Student entity) {
        map.put(entity.getUserId(), entity); // upsert
        return entity;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Saves multiple students in a single operation.
     */
    @Override
    public List<Student> saveAll(Iterable<Student> entities) {
        List<Student> out = new ArrayList<>();
        for (Student s : entities) {
            save(s);
            out.add(s);
        }
        return out;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Removes a student by their user ID. If the student doesn't exist,
     * this method has no effect.
     */
    @Override
    public void deleteById(String id) {
        map.remove(id);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Removes multiple students by their user IDs.
     */
    @Override
    public void deleteAllById(Iterable<String> ids) {
        for (String id : ids) map.remove(id);
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Warning:</strong> This removes all student data from the repository.
     */
    @Override
    public void deleteAll() {
        map.clear();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Checks if a student with the specified user ID exists.
     */
    @Override
    public boolean existsById(String id) {
        return map.containsKey(id);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Returns the total number of students in the repository.
     */
    @Override
    public long count() {
        return map.size();
    }

    // ----- Convenience domain finders -----

    /**
     * Finds all students enrolled in a specific major or programme.
     * 
     * <p>Performs case-insensitive matching on the major name, allowing
     * queries like "computer science", "Computer Science", or "COMPUTER SCIENCE"
     * to all return the same results.
     * 
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Finding students eligible for major-specific internships</li>
     *   <li>Generating statistics by academic programme</li>
     *   <li>Filtering student lists for department-specific communications</li>
     * </ul>
     * 
     * @param major the name of the major/programme to search for (case-insensitive)
     * @return a list of all students in the specified major (empty list if none found or major is null)
     */
    public List<Student> findByMajor(String major) {
        if (major == null) return List.of();
        String m = major.trim();
        List<Student> out = new ArrayList<>();
        for (Student s : map.values()) {
            if (s.getMajor().equalsIgnoreCase(m)) out.add(s);
        }
        return out;
    }

    /**
     * Finds all students in a specific year of study.
     * 
     * <p>Year of study typically ranges from 1 (freshman) to 4 (senior),
     * though the repository accepts any integer value.
     * 
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Finding students eligible for year-specific internship levels</li>
     *   <li>Matching students to internships requiring certain experience</li>
     *   <li>Generating year-group statistics or cohort analysis</li>
     * </ul>
     * 
     * @param year the year of study (typically 1-4)
     * @return a list of all students in the specified year (empty list if none found)
     */
    public List<Student> findByYear(int year) {
        List<Student> out = new ArrayList<>();
        for (Student s : map.values()) if (s.getYearOfStudy() == year) out.add(s);
        return out;
    }

    // ----- CSV I/O -----

    /**
     * Loads student data from a CSV file.
     * 
     * <p><strong>CSV Format Expected:</strong>
     * <pre>
     * Header: StudentID,Name,Major,Year,Email,Password
     * Row:    U1234567A,John Doe,Computer Science,3,john@example.com,password123
     * </pre>
     * 
     * <p><strong>Flexible Header Recognition:</strong>
     * The loader accepts multiple variations of column names:
     * <ul>
     *   <li><strong>Student ID:</strong> "studentid", "userid", "id", "student id"</li>
     *   <li><strong>Name:</strong> "name", "full name"</li>
     *   <li><strong>Major:</strong> "major", "programme", "program", "course"</li>
     *   <li><strong>Year:</strong> "year", "yearofstudy", "study year"</li>
     *   <li><strong>Email:</strong> "email", "mail"</li>
     *   <li><strong>Password:</strong> "password", "pwd"</li>
     * </ul>
     * 
     * <p><strong>Validation & Defaults:</strong>
     * <ul>
     *   <li>Student ID must match pattern: U#######X (7 digits + 1 uppercase letter)</li>
     *   <li>Invalid IDs cause the line to be skipped with an error message</li>
     *   <li>Missing major defaults to "CSC" (Computer Science)</li>
     *   <li>Missing year defaults to 1</li>
     *   <li>Missing password defaults to "password"</li>
     *   <li>Empty email is allowed (stored as empty string)</li>
     * </ul>
     * 
     * <p><strong>Error Handling:</strong>
     * <ul>
     *   <li>File not found: Prints message and returns without error</li>
     *   <li>Invalid lines: Prints error to stderr and continues with next line</li>
     *   <li>Parse errors: Skips problematic line and continues loading</li>
     * </ul>
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Check if file exists (return if not found)</li>
     *   <li>Read and parse header line to create column index</li>
     *   <li>For each data line:
     *     <ul>
     *       <li>Extract values using flexible column name matching</li>
     *       <li>Validate student ID format</li>
     *       <li>Apply defaults for missing values</li>
     *       <li>Create Student entity and set password</li>
     *       <li>Save to repository</li>
     *     </ul>
     *   </li>
     *   <li>Handle errors gracefully, continuing with remaining lines</li>
     * </ol>
     * 
     * @param path the file path to the CSV file
     * @throws IOException if there is an error reading the file
     */
    public void loadFromCsv(String path) throws IOException {
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            System.out.println("Students file not found: " + path);
            return;
        }

        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String headerLine = br.readLine();
            if (headerLine == null) return;

            // Create header index manually
            String[] headers = CsvUtils.splitCsv(headerLine);
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i] != null ? headers[i].trim().toLowerCase() : "";
                idx.put(header, i);
            }

            String line;
            int lineNo = 1;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;

                String[] row = CsvUtils.splitCsv(line);
                try {
                    String userId = getCsvValue(row, idx, "studentid", "userid", "id", "student id");
                    String name = getCsvValue(row, idx, "name", "full name");
                    String major = getCsvValue(row, idx, "major", "programme", "program", "course");
                    String yearS = getCsvValue(row, idx, "year", "yearofstudy", "study year");
                    String email = getCsvValue(row, idx, "email", "mail");
                    String password = getCsvValue(row, idx, "password", "pwd");

                    // Validate student ID format
                    if (userId == null || !userId.matches("^U\\d{7}[A-Z]$")) {
                        System.err.println("Invalid student ID format at line " + lineNo + ": " + userId);
                        continue;
                    }

                    int year = yearS != null ? Integer.parseInt(yearS.trim()) : 1;
                    
                    // Use provided password or default
                    if (password == null || password.isEmpty()) {
                        password = "password";
                    }

                    Student student = new Student(userId.trim(), name.trim(), 
                        major != null ? major.trim() : "CSC", year, 
                        email != null ? email.trim() : "");
                    student.setPassword(password); // SET THE PASSWORD
                    save(student);
                    
                } catch (Exception ex) {
                    System.err.println("Skipping line " + lineNo + ": " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Saves all students to a CSV file.
     * 
     * <p><strong>CSV Format Generated:</strong>
     * <pre>
     * Header: StudentID,Name,Major,Year,Email,Password
     * Row:    U1234567A,John Doe,Computer Science,3,john@example.com,password123
     * </pre>
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Create parent directories if they don't exist</li>
     *   <li>Open file for writing (creates new or truncates existing)</li>
     *   <li>Write CSV header line</li>
     *   <li>For each student in repository (in insertion order):
     *     <ul>
     *       <li>Escape special characters in string fields</li>
     *       <li>Write comma-separated values</li>
     *     </ul>
     *   </li>
     *   <li>Close file</li>
     * </ol>
     * 
     * <p><strong>Data Integrity:</strong>
     * <ul>
     *   <li>Uses CsvUtils.esc() to properly escape commas, quotes, and newlines</li>
     *   <li>Maintains data order using LinkedHashMap insertion sequence</li>
     *   <li>UTF-8 encoding ensures international characters are preserved</li>
     *   <li>Includes passwords for complete data backup</li>
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
            bw.write("StudentID,Name,Major,Year,Email,Password");
            bw.newLine();
            for (Student s : map.values()) {
                bw.write(String.join(",",
                    CsvUtils.esc(s.getUserId()),
                    CsvUtils.esc(s.getName()),
                    CsvUtils.esc(s.getMajor()),
                    String.valueOf(s.getYearOfStudy()),
                    CsvUtils.esc(s.getEmail()),
                    CsvUtils.esc(s.getPassword())  // ADDED PASSWORD
                ));
                bw.newLine();
            }
        }
    }

    /**
     * Helper method to retrieve CSV cell values with flexible header name matching.
     * 
     * <p>This method enables the CSV loader to work with different CSV file formats
     * by accepting multiple possible names for the same column. For example, a student
     * ID column might be named "StudentID", "UserID", "ID", or "Student ID" in
     * different files.
     * 
     * <p><strong>Search Strategy:</strong>
     * <ol>
     *   <li>Tries each possible header name in order</li>
     *   <li>Performs case-insensitive lookup in the header index</li>
     *   <li>Returns the first matching non-empty value found</li>
     *   <li>Returns null if no match found or all values are empty</li>
     * </ol>
     * 
     * @param row the array of cell values from a CSV line
     * @param idx the header-to-index mapping (header names in lowercase)
     * @param possibleHeaders array of possible column names to try (will be lowercased)
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