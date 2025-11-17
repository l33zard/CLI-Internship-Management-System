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
 * In-memory repository for managing {@link entity.Student} entities.
 *
 * <p>This repository provides CRUD operations for student records and supports CSV-based
 * persistence. Students are uniquely identified by their user ID following the format
 * "U\d{7}[A-Z]" (e.g., "U1234567A"). The repository maintains insertion order using a
 * {@code LinkedHashMap} for stable export order and predictable iteration.
 *
 * <p>The repository includes domain-specific query methods for filtering students by
 * major and year of study, along with robust CSV import/export functionality with
 * flexible header recognition.
 *
 */
public class StudentRepository implements CrudRepository<Student, String> {

    // Keep stable export order (nicer diffs)
    private final Map<String, Student> map = new LinkedHashMap<>();

    // ----- CrudRepository implementation -----

    /**
     * Finds a student by their unique user identifier.
     *
     * <p>The user ID must follow the format "U\d{7}[A-Z]" (e.g., "U1234567A").
     *
     * @param id the student user ID to search for
     * @return an {@code Optional} containing the found student, or empty if not found
     * @throws NullPointerException if the id is null
     */
    @Override
    public Optional<Student> findById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    /**
     * Retrieves all students in the repository.
     *
     * @return a list of all students, maintaining insertion order
     */
    @Override
    public List<Student> findAll() {
        return new ArrayList<>(map.values());
    }

    /**
     * Saves a student to the repository.
     *
     * <p>If a student with the same user ID already exists, it will be replaced.
     *
     * @param entity the student to save
     * @return the saved student
     * @throws NullPointerException if the student is null
     */
    @Override
    public Student save(Student entity) {
        map.put(entity.getUserId(), entity); // upsert
        return entity;
    }

    /**
     * Saves all students in the provided iterable.
     *
     * @param entities the students to save
     * @return a list of the saved students
     * @throws NullPointerException if the iterable or any element is null
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
     * Deletes a student by their user ID.
     *
     * @param id the user ID of the student to delete
     * @throws NullPointerException if the id is null
     */
    @Override
    public void deleteById(String id) {
        map.remove(id);
    }

    /**
     * Deletes all students with the specified user IDs.
     *
     * @param ids the user IDs of students to delete
     * @throws NullPointerException if the iterable or any element is null
     */
    @Override
    public void deleteAllById(Iterable<String> ids) {
        for (String id : ids) map.remove(id);
    }

    /**
     * Deletes all students from the repository.
     */
    @Override
    public void deleteAll() {
        map.clear();
    }

    /**
     * Checks whether a student with the given user ID exists.
     *
     * @param id the user ID to check
     * @return true if a student with the ID exists, false otherwise
     * @throws NullPointerException if the id is null
     */
    @Override
    public boolean existsById(String id) {
        return map.containsKey(id);
    }

    /**
     * Returns the total number of students in the repository.
     *
     * @return the count of students
     */
    @Override
    public long count() {
        return map.size();
    }

    // ----- Convenience domain finders -----

    /**
     * Finds all students in a specific major (case-insensitive).
     *
     * @param major the major to filter by
     * @return a list of students in the specified major, or empty list if major is null
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
     * @param year the year of study to filter by (typically 1-4)
     * @return a list of students in the specified year
     */
    public List<Student> findByYear(int year) {
        List<Student> out = new ArrayList<>();
        for (Student s : map.values()) if (s.getYearOfStudy() == year) out.add(s);
        return out;
    }

    // ----- CSV I/O -----

    /**
     * Loads students from a CSV file with flexible header recognition.
     *
     * <p>The CSV file can use various column headers for each field:
     * <ul>
     *   <li><b>Student ID:</b> "studentid", "userid", "id", "student id"</li>
     *   <li><b>Name:</b> "name", "full name"</li>
     *   <li><b>Major:</b> "major", "programme", "program", "course"</li>
     *   <li><b>Year:</b> "year", "yearofstudy", "study year"</li>
     *   <li><b>Email:</b> "email", "mail"</li>
     *   <li><b>Password:</b> "password", "pwd"</li>
     * </ul>
     * 
     *
     * <p><b>Validation and Defaults:</b>
     * <ul>
     *   <li>Student ID must match format "U\d{7}[A-Z]" (e.g., "U1234567A")</li>
     *   <li>Invalid student IDs are skipped with an error message</li>
     *   <li>Major defaults to "CSC" if not specified</li>
     *   <li>Year defaults to 1 if not specified</li>
     *   <li>Password defaults to "password" if not specified</li>
     *   <li>Email defaults to empty string if not specified</li>
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
     * Saves all students to a CSV file with standardized column headers.
     *
     * <p>The CSV file is created with the following columns in this exact order:
     * <ol>
     *   <li>StudentID - Unique user ID following format "U\d{7}[A-Z]"</li>
     *   <li>Name - Student's full name</li>
     *   <li>Major - Academic major/program</li>
     *   <li>Year - Year of study (1-4)</li>
     *   <li>Email - Email address</li>
     *   <li>Password - Account password (stored in plaintext)</li>
     * </ol>
     * 
     *
     * <p>Parent directories are created automatically if they don't exist.
     * If the file already exists, it will be overwritten.
     *
     * <p><b>Security Note:</b> Passwords are stored in plaintext in the CSV file.
     * In a production environment, consider hashing passwords before storage.
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