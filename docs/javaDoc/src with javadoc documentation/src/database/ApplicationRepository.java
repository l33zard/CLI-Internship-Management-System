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
 * In-memory repository for InternshipApplication entities.
 * Also implements Student.AppReadPort for domain-level checks.
 */
public class ApplicationRepository
        implements CrudRepository<InternshipApplication, String>, Student.AppReadPort {

    // Stable order for predictable iteration/exports
    private final Map<String, InternshipApplication> map = new LinkedHashMap<>();

    // ---------- CrudRepository ----------

    @Override
    public Optional<InternshipApplication> findById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    @Override
    public List<InternshipApplication> findAll() {
        return new ArrayList<>(map.values());
    }

    @Override
    public InternshipApplication save(InternshipApplication app) {
        map.put(app.getApplicationId(), app);
        return app;
    }

    @Override
    public List<InternshipApplication> saveAll(Iterable<InternshipApplication> entities) {
        List<InternshipApplication> out = new ArrayList<>();
        for (InternshipApplication e : entities) {
            save(e);
            out.add(e);
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

    // ---------- Business-related queries ----------

    /** Prevent duplicate applications by the same student to the same internship. */
    public boolean existsByStudentAndInternship(String studentId, String internshipId) {
        if (studentId == null || internshipId == null) return false;
        return map.values().stream().anyMatch(x ->
            x.getStudent().getUserId().equals(studentId) &&
            x.getInternship().getInternshipId().equals(internshipId)
        );
    }

    /** Count active applications for a student (PENDING or SUCCESSFUL but not yet accepted). */
    public long countActiveByStudent(String studentId) {
        if (studentId == null) return 0;
        return map.values().stream()
            .filter(x -> x.getStudent().getUserId().equals(studentId))
            .filter(x -> x.getStatus() == ApplicationStatus.PENDING ||
                        (x.getStatus() == ApplicationStatus.SUCCESSFUL && !x.isStudentAccepted()))
            .count();
    }

    /** Count accepted applications for an internship (used if you ever cross-check filled state). */
    public long countAcceptedForInternship(String internshipId) {
        if (internshipId == null) return 0;
        return map.values().stream()
            .filter(x -> x.getInternship().getInternshipId().equals(internshipId))
            .filter(InternshipApplication::isStudentAccepted)
            .count();
    }

    /** All applications created by a specific student (by studentId). */
    public List<InternshipApplication> findByStudent(String studentId) {
        if (studentId == null) return List.of();
        List<InternshipApplication> out = new ArrayList<>();
        for (InternshipApplication a : findAll()) {
            var s = a.getStudent();
            if (s != null && studentId.equals(s.getUserId())) out.add(a);
        }
        return out;
    }

    /** Get all applications for a specific internship. */
    public List<InternshipApplication> findByInternship(String internshipId) {
        if (internshipId == null) return List.of();
        return map.values().stream()
            .filter(x -> x.getInternship().getInternshipId().equals(internshipId))
            .collect(Collectors.toList());
    }

    /** Delete all applications for a given student (if needed for cleanup). */
    public void deleteByStudent(String studentId) {
        if (studentId == null) return;
        map.values().removeIf(x -> x.getStudent().getUserId().equals(studentId));
    }

    /** Remove everything (optional, for reset/testing). */
    public void clear() {
        map.clear();
    }

    // ---------- Student.AppReadPort ----------

    @Override
    public int countActiveApplications(String studentId) {
        return (int) countActiveByStudent(studentId);
    }

    @Override
    public boolean hasConfirmedPlacement(String studentId) {
        if (studentId == null) return false;
        for (var a : findByStudent(studentId)) {
            if (a.getStatus() == ApplicationStatus.SUCCESSFUL && a.isStudentAccepted()) return true;
        }
        return false;
    }
    
    // ---------- CSV Persistence ----------
    
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