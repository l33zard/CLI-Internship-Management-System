package database;

import entity.WithdrawalRequest;
import entity.WithdrawalRequestStatus;
import entity.CareerCenterStaff;
import util.CsvUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.BufferedReader;

/** In-memory "table" for WithdrawalRequest. */
public class WithdrawalRequestRepository implements CrudRepository<WithdrawalRequest, String> {

    // Stable order for predictable iteration/exports
    private final Map<String, WithdrawalRequest> map = new LinkedHashMap<>();

    // ---------- CrudRepository ----------

    @Override
    public Optional<WithdrawalRequest> findById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    @Override
    public List<WithdrawalRequest> findAll() {
        return new ArrayList<>(map.values());
    }

    @Override
    public WithdrawalRequest save(WithdrawalRequest wr) {
        map.put(wr.getRequestId(), wr);
        return wr;
    }

    @Override
    public List<WithdrawalRequest> saveAll(Iterable<WithdrawalRequest> entities) {
        List<WithdrawalRequest> out = new ArrayList<>();
        for (WithdrawalRequest e : entities) { save(e); out.add(e); }
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

    // ---------- Domain Queries ----------

    /** All requests from a specific student. */
    public List<WithdrawalRequest> findByStudent(String studentId) {
        if (studentId == null) return List.of();
        return map.values().stream()
            .filter(wr -> wr.getRequestedBy().getUserId().equals(studentId))
            .collect(Collectors.toList());
    }

    /** First request for a specific application (if any). */
    public Optional<WithdrawalRequest> findByApplicationId(String applicationId) {
        if (applicationId == null) return Optional.empty();
        return map.values().stream()
            .filter(wr -> wr.getApplication().getApplicationId().equals(applicationId))
            .findFirst();
    }

    /** All pending requests (for staff inbox). */
    public List<WithdrawalRequest> findPending() {
        return map.values().stream()
            .filter(wr -> wr.getStatus() == WithdrawalRequestStatus.PENDING)
            .collect(Collectors.toList());
    }

    /** Count pending requests affecting a given internship (optional dashboard). */
    public long countPendingForInternship(String internshipId) {
        if (internshipId == null) return 0;
        return map.values().stream()
            .filter(wr -> wr.getStatus() == WithdrawalRequestStatus.PENDING)
            .filter(wr -> wr.getApplication().getInternship().getInternshipId().equals(internshipId))
            .count();
    }
    
    public void saveToCsv(String path) throws java.io.IOException {
        Path p = Paths.get(path);
        try (var bw = Files.newBufferedWriter(p, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write("RequestID,ApplicationID,StudentID,RequestedOn,Reason,Status,ProcessedBy,ProcessedOn,StaffNote");
            bw.newLine();
            for (var wr : map.values()) {
                var app = wr.getApplication();
                var student = wr.getRequestedBy();
                var processedBy = wr.getProcessedBy();

                bw.write(String.join(",",
                    CsvUtils.esc(wr.getRequestId()),
                    CsvUtils.esc(app.getApplicationId()),
                    CsvUtils.esc(student.getUserId()),
                    CsvUtils.esc(String.valueOf(wr.getRequestedOn())),
                    CsvUtils.esc(wr.getReason()),
                    CsvUtils.esc(wr.getStatus() == null ? "" : wr.getStatus().name()),
                    CsvUtils.esc(processedBy == null ? "" : processedBy.getUserId()),
                    CsvUtils.esc(String.valueOf(wr.getProcessedOn())),
                    CsvUtils.esc(wr.getStaffNote() == null ? "" : wr.getStaffNote())
                ));
                bw.newLine();
            }
        }
    }
    
    /** Load withdrawal requests; needs app/student/staff repos to resolve references. */
    public void loadFromCsv(String path,
                            ApplicationRepository applications,
                            StudentRepository students,
                            CareerCenterStaffRepository staffRepo) throws java.io.IOException {
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            System.out.println("Withdrawal requests file not found: " + path);
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
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] row = CsvUtils.splitCsv(line);

                // Get values with fallback header names
                String appId = getCsvValue(row, idx, "applicationid", "app", "application");
                String studentId = getCsvValue(row, idx, "studentid", "student");
                String reason = getCsvValue(row, idx, "reason");
                String statusS = getCsvValue(row, idx, "status");
                String procBy = getCsvValue(row, idx, "processedby", "processedbystaffid", "staffid");
                String note = getCsvValue(row, idx, "staffnote", "note");

                var appOpt = applications.findById(appId);
                var stuOpt = students.findById(studentId);
                if (appOpt.isEmpty() || stuOpt.isEmpty()) {
                    System.err.println("Skipping withdrawal request - app or student not found: " + appId + ", " + studentId);
                    continue;
                }

                var wr = new WithdrawalRequest(appOpt.get(), stuOpt.get(), reason);

                // Handle status if present
                if (statusS != null && !statusS.isBlank()) {
                    try {
                        var st = WithdrawalRequestStatus.valueOf(statusS.trim().toUpperCase());
                        if (st == WithdrawalRequestStatus.APPROVED || st == WithdrawalRequestStatus.REJECTED) {
                            var staffOpt = (procBy == null || procBy.isBlank())
                                    ? Optional.<CareerCenterStaff>empty()
                                    : staffRepo.findById(procBy);
                            if (staffOpt.isPresent()) {
                                try {
                                    if (st == WithdrawalRequestStatus.APPROVED) {
                                        wr.approve(staffOpt.get(), note != null ? note : "");
                                    } else {
                                        wr.reject(staffOpt.get(), note != null ? note : "");
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error processing withdrawal status: " + e.getMessage());
                                }
                            }
                        } else if (st == WithdrawalRequestStatus.PENDING) {
                            // Already PENDING by default
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid withdrawal status: " + statusS);
                    }
                }

                save(wr);
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