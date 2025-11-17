package database;

import entity.ApplicationStatus;
import entity.InternshipApplication;
import entity.Student;
import entity.Internship;
import util.CsvUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.BufferedReader;
import java.time.LocalDate;

/**
 * In-memory repository for managing {@link entity.InternshipApplication} entities.
 * 
 * <p>This repository provides CRUD operations for internship applications while also
 * implementing {@link entity.Student.AppReadPort} to support domain-level validation
 * queries required by student business rules, such as counting active applications
 * and checking for confirmed placements.
 *
 * <p>The repository maintains applications in insertion order for predictable iteration
 * and supports CSV-based persistence for data storage and retrieval.
 */
public class ApplicationRepository
    implements CrudRepository<InternshipApplication, String>, Student.AppReadPort {

    // Stable order for predictable iteration/exports
    private final Map<String, InternshipApplication> map = new LinkedHashMap<>();

    // ---------- CrudRepository ----------

    /**
     * Finds an application by its unique identifier.
     *
     * @param id the application ID to search for
     * @return an {@code Optional} containing the found application, or empty if not found
     * @throws NullPointerException if the id is null
     */
    @Override
    public Optional<InternshipApplication> findById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    /**
     * Retrieves all applications in the repository.
     *
     * @return a list of all applications, maintaining insertion order
     */
    @Override
    public List<InternshipApplication> findAll() {
        return new ArrayList<>(map.values());
    }

    /**
     * Saves an application to the repository.
     *
     * <p>If an application with the same ID already exists, it will be replaced.
     *
     * @param app the application to save
     * @return the saved application
     * @throws NullPointerException if the application is null
     */
    @Override
    public InternshipApplication save(InternshipApplication app) {
        map.put(app.getApplicationId(), app);
        return app;
    }

    /**
     * Saves all applications in the provided iterable.
     *
     * @param entities the applications to save
     * @return a list of the saved applications
     * @throws NullPointerException if the iterable or any element is null
     */
    @Override
    public List<InternshipApplication> saveAll(Iterable<InternshipApplication> entities) {
        List<InternshipApplication> out = new ArrayList<>();
        for (InternshipApplication e : entities) {
            save(e);
            out.add(e);
        }
        return out;
    }

    /**
     * Deletes an application by its ID.
     *
     * @param id the ID of the application to delete
     * @throws NullPointerException if the id is null
     */
    @Override
    public void deleteById(String id) {
        map.remove(id);
    }

    /**
     * Deletes all applications with the specified IDs.
     *
     * @param ids the IDs of applications to delete
     * @throws NullPointerException if the iterable or any element is null
     */
    @Override
    public void deleteAllById(Iterable<String> ids) {
        for (String id : ids) map.remove(id);
    }

    /**
     * Deletes all applications from the repository.
     */
    @Override
    public void deleteAll() {
        map.clear();
    }

    /**
     * Checks whether an application with the given ID exists.
     *
     * @param id the ID to check
     * @return true if an application with the ID exists, false otherwise
     * @throws NullPointerException if the id is null
     */
    @Override
    public boolean existsById(String id) {
        return map.containsKey(id);
    }

    /**
     * Returns the total number of applications in the repository.
     *
     * @return the count of applications
     */
    @Override
    public long count() {
        return map.size();
    }

    // ---------- Business-related queries ----------

    /**
     * Checks if a duplicate application exists for the same student and internship.
     *
     * <p>This method prevents students from applying multiple times to the same internship.
     *
     * @param studentId the student's ID
     * @param internshipId the internship's ID
     * @return true if a duplicate application exists, false otherwise
     */
    public boolean existsByStudentAndInternship(String studentId, String internshipId) {
        if (studentId == null || internshipId == null) return false;
        return map.values().stream().anyMatch(x ->
            x.getStudent().getUserId().equals(studentId) &&
            x.getInternship().getInternshipId().equals(internshipId)
        );
    }

    /**
     * Counts active applications for a student.
     *
     * <p>Active applications are defined as those with status {@code PENDING} or 
     * {@code SUCCESSFUL} but not yet accepted by the student.
     *
     * @param studentId the student's ID
     * @return the count of active applications
     */
    public long countActiveByStudent(String studentId) {
        if (studentId == null) return 0;
        return map.values().stream()
            .filter(x -> x.getStudent().getUserId().equals(studentId))
            .filter(x -> x.getStatus() == ApplicationStatus.PENDING ||
                        (x.getStatus() == ApplicationStatus.SUCCESSFUL && !x.isStudentAccepted()))
            .count();
    }

    /**
     * Counts accepted applications for an internship.
     *
     * <p>This can be used to cross-check if an internship has reached its capacity.
     *
     * @param internshipId the internship's ID
     * @return the count of accepted applications
     */
    public long countAcceptedForInternship(String internshipId) {
        if (internshipId == null) return 0;
        return map.values().stream()
            .filter(x -> x.getInternship().getInternshipId().equals(internshipId))
            .filter(InternshipApplication::isStudentAccepted)
            .count();
    }

    /**
     * Finds all applications created by a specific student.
     *
     * @param studentId the student's ID
     * @return a list of applications for the student, or empty list if none found
     */
    public List<InternshipApplication> findByStudent(String studentId) {
        if (studentId == null) return List.of();
        List<InternshipApplication> out = new ArrayList<>();
        for (InternshipApplication a : findAll()) {
            var s = a.getStudent();
            if (s != null && studentId.equals(s.getUserId())) out.add(a);
        }
        return out;
    }

    /**
     * Finds all applications for a specific internship.
     *
     * @param internshipId the internship's ID
     * @return a list of applications for the internship, or empty list if none found
     */
    public List<InternshipApplication> findByInternship(String internshipId) {
        if (internshipId == null) return List.of();
        return map.values().stream()
            .filter(x -> x.getInternship().getInternshipId().equals(internshipId))
            .collect(Collectors.toList());
    }

    /**
     * Deletes all applications for a given student.
     *
     * <p>Useful for cleanup when a student account is deleted.
     *
     * @param studentId the student's ID
     */
    public void deleteByStudent(String studentId) {
        if (studentId == null) return;
        map.values().removeIf(x -> x.getStudent().getUserId().equals(studentId));
    }

    /**
     * Removes all applications from the repository.
     *
     * <p>Primarily intended for testing and reset scenarios.
     */
    public void clear() {
        map.clear();
    }

    // ---------- Student.AppReadPort ----------

    /**
     * Counts active applications for a student as required by the domain port.
     *
     * @param studentId the student's ID
     * @return the count of active applications
     * @see #countActiveByStudent(String)
     */
    @Override
    public int countActiveApplications(String studentId) {
        return (int) countActiveByStudent(studentId);
    }

    /**
     * Checks if a student has a confirmed placement.
     *
     * <p>A confirmed placement is defined as a SUCCESSFUL application that has been
     * accepted by the student.
     *
     * @param studentId the student's ID
     * @return true if the student has a confirmed placement, false otherwise
     */
    @Override
    public boolean hasConfirmedPlacement(String studentId) {
        if (studentId == null) return false;
        for (var a : findByStudent(studentId)) {
            if (a.getStatus() == ApplicationStatus.SUCCESSFUL && a.isStudentAccepted()) return true;
        }
        return false;
    }
    
    // ---------- CSV Persistence ----------
    
    /**
     * Saves all applications to a CSV file.
     *
     * <p>The CSV format includes the following columns:
     * <ul>
     *   <li>ApplicationID</li>
     *   <li>AppliedOn</li>
     *   <li>StudentID</li>
     *   <li>InternshipID</li>
     *   <li>Status</li>
     *   <li>StudentAccepted</li>
     * </ul>
     * 
     *
     * <p>Parent directories are created automatically if they don't exist.
     *
     * @param path the output CSV file path
     * @throws java.io.IOException if an I/O error occurs
     * @throws SecurityException if file access is denied
     */
    public void saveToCsv(String path) throws java.io.IOException {
        Path p = Paths.get(path);
        Files.createDirectories(p.getParent() == null ? Paths.get(".") : p.getParent());
        try (var bw = Files.newBufferedWriter(p, StandardCharsets.UTF_8, 
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write("ApplicationID,AppliedOn,StudentID,InternshipID,Status,StudentAccepted");
            bw.newLine();
            for (var a : map.values()) {
                bw.write(String.join(",",
                    CsvUtils.esc(a.getApplicationId()),
                    CsvUtils.esc(String.valueOf(a.getAppliedOn())),
                    CsvUtils.esc(a.getStudent().getUserId()),
                    CsvUtils.esc(a.getInternship().getInternshipId()),
                    CsvUtils.esc(a.getStatus() == null ? "" : a.getStatus().name()),
                    String.valueOf(a.isStudentAccepted())
                ));
                bw.newLine();
            }
        }
    }
    
    /**
     * Loads applications from a CSV file and attaches resolved student and internship references.
     *
     * <p>This method attempts to resolve student and internship references using the provided
     * repositories. Applications that cannot be resolved (missing student or internship) are
     * skipped and diagnostic information is printed to help identify resolution issues.
     *
     * <p>The CSV file should have a header row with case-insensitive column names that can include:
     * <ul>
     *   <li>ApplicationID, ID</li>
     *   <li>AppliedOn, Applied, Date</li>
     *   <li>StudentID, Student, SID</li>
     *   <li>InternshipID, Internship, IID</li>
     *   <li>Status</li>
     *   <li>StudentAccepted, Accepted</li>
     * </ul>
     * 
     *
     * @param path the path to the CSV file
     * @param students the student repository for resolving student references
     * @param internships the internship repository for resolving internship references
     * @throws java.io.IOException if an I/O error occurs while reading the file
     * @throws SecurityException if file access is denied
     */
    public void loadFromCsv(String path,
                            StudentRepository students,
                            InternshipRepository internships) throws java.io.IOException {
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            System.out.println("Applications file not found: " + path);
            return;
        }

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
                    String applicationId = getCsvValue(row, idx, "applicationid", "id");
                    String appliedOnStr = getCsvValue(row, idx, "appliedon", "applied", "date");
                    String studentId = getCsvValue(row, idx, "studentid", "student", "sid");
                    String internshipId = getCsvValue(row, idx, "internshipid", "internship", "iid");
                    String statusS = getCsvValue(row, idx, "status");
                    String acceptedS = getCsvValue(row, idx, "studentaccepted", "accepted");

                    // DEBUG: Print what we're trying to find

                    var studentOpt = students.findById(studentId);
                    var internOpt = internships.findById(internshipId);
                    
                    if (studentOpt.isEmpty()) {
                        System.out.println("Student not found: " + studentId + ". Available students:");
                        for (Student s : students.findAll()) {
                            System.out.println("  - " + s.getUserId() + ": " + s.getName());
                        }
                    }
                    
                    if (internOpt.isEmpty()) {
                        System.out.println("Internship not found: " + internshipId + ". Available internships:");
                        for (Internship i : internships.findAll()) {
                            System.out.println("  - " + i.getInternshipId() + ": " + i.getTitle());
                        }
                    }

                    if (studentOpt.isEmpty() || internOpt.isEmpty()) {
                        System.out.println("Skipping application - student or internship not found: " + studentId + ", " + internshipId);
                        continue;
                    }

                    LocalDate appliedOn;
                    if (appliedOnStr != null && !appliedOnStr.isEmpty()) {
                        appliedOn = LocalDate.parse(appliedOnStr.trim());
                    } else {
                        appliedOn = LocalDate.now();
                    }

                    Student.AppReadPort port = new Student.AppReadPort() {
                        @Override public int countActiveApplications(String sid) {
                            return (int) countActiveByStudent(sid);
                        }
                        @Override public boolean hasConfirmedPlacement(String sid) {
                            return ApplicationRepository.this.hasConfirmedPlacement(sid);
                        }
                    };

                    InternshipApplication app = new InternshipApplication(appliedOn, studentOpt.get(), internOpt.get(), port);

                    if (applicationId != null && !applicationId.isEmpty()) {
                        app.setApplicationId(applicationId);
                    }

                    if (statusS != null && !statusS.isBlank()) {
                        try {
                            var st = ApplicationStatus.valueOf(statusS.trim().toUpperCase());
                            switch (st) {
                                case PENDING -> { /* keep default */ }
                                case SUCCESSFUL -> app.markSuccessful();
                                case UNSUCCESSFUL -> app.markUnsuccessful();
                                case WITHDRAWN -> app.markWithdrawn();
                            }
                        } catch (IllegalArgumentException e) {
                            System.out.println("Invalid application status: " + statusS);
                        }
                    }

                    boolean accepted = acceptedS != null && acceptedS.equalsIgnoreCase("true");
                    if (accepted) {
                        try { 
                            app.confirmAcceptance(port); 
                        } catch (Exception e) {
                            System.out.println("Failed to confirm acceptance for application: " + e.getMessage());
                        }
                    }

                    save(app);
                    
                } catch (Exception e) {
                    System.out.println("Error parsing application line: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Extracts a value from a CSV row using flexible header matching.
     *
     * @param row the CSV row data
     * @param idx the header index mapping
     * @param possibleHeaders possible header names to try (case-insensitive)
     * @return the extracted value, or null if not found
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