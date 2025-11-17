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

/**
 * In-memory repository for managing {@link entity.WithdrawalRequest} entities.
 *
 * <p>This repository provides CRUD operations for internship application withdrawal requests
 * and supports CSV-based persistence. Withdrawal requests represent student requests to
 * withdraw from internship applications, which require approval from career center staff.
 *
 * <p>The repository maintains withdrawal requests in insertion order using a {@code LinkedHashMap}
 * for predictable iteration and export order. It includes domain-specific query methods
 * for filtering by student, application, status, and internship.
 *
 */
public class WithdrawalRequestRepository implements CrudRepository<WithdrawalRequest, String> {

    // Stable order for predictable iteration/exports
    private final Map<String, WithdrawalRequest> map = new LinkedHashMap<>();

    // ---------- CrudRepository ----------

    /**
     * Finds a withdrawal request by its unique identifier.
     *
     * @param id the withdrawal request ID to search for
     * @return an {@code Optional} containing the found withdrawal request, or empty if not found
     * @throws NullPointerException if the id is null
     */
    @Override
    public Optional<WithdrawalRequest> findById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    /**
     * Retrieves all withdrawal requests in the repository.
     *
     * @return a list of all withdrawal requests, maintaining insertion order
     */
    @Override
    public List<WithdrawalRequest> findAll() {
        return new ArrayList<>(map.values());
    }

    /**
     * Saves a withdrawal request to the repository.
     *
     * <p>If a withdrawal request with the same ID already exists, it will be replaced.
     *
     * @param wr the withdrawal request to save
     * @return the saved withdrawal request
     * @throws NullPointerException if the withdrawal request is null
     */
    @Override
    public WithdrawalRequest save(WithdrawalRequest wr) {
        map.put(wr.getRequestId(), wr);
        return wr;
    }

    /**
     * Saves all withdrawal requests in the provided iterable.
     *
     * @param entities the withdrawal requests to save
     * @return a list of the saved withdrawal requests
     * @throws NullPointerException if the iterable or any element is null
     */
    @Override
    public List<WithdrawalRequest> saveAll(Iterable<WithdrawalRequest> entities) {
        List<WithdrawalRequest> out = new ArrayList<>();
        for (WithdrawalRequest e : entities) { save(e); out.add(e); }
        return out;
    }

    /**
     * Deletes a withdrawal request by its ID.
     *
     * @param id the ID of the withdrawal request to delete
     * @throws NullPointerException if the id is null
     */
    @Override
    public void deleteById(String id) {
        map.remove(id);
    }

    /**
     * Deletes all withdrawal requests with the specified IDs.
     *
     * @param ids the IDs of withdrawal requests to delete
     * @throws NullPointerException if the iterable or any element is null
     */
    @Override
    public void deleteAllById(Iterable<String> ids) {
        for (String id : ids) map.remove(id);
    }

    /**
     * Deletes all withdrawal requests from the repository.
     */
    @Override
    public void deleteAll() {
        map.clear();
    }

    /**
     * Checks whether a withdrawal request with the given ID exists.
     *
     * @param id the ID to check
     * @return true if a withdrawal request with the ID exists, false otherwise
     * @throws NullPointerException if the id is null
     */
    @Override
    public boolean existsById(String id) {
        return map.containsKey(id);
    }

    /**
     * Returns the total number of withdrawal requests in the repository.
     *
     * @return the count of withdrawal requests
     */
    @Override
    public long count() {
        return map.size();
    }

    // ---------- Domain Queries ----------

    /**
     * Finds all withdrawal requests from a specific student.
     *
     * @param studentId the student's user ID to search for
     * @return a list of withdrawal requests from the specified student, or empty list if studentId is null
     */
    public List<WithdrawalRequest> findByStudent(String studentId) {
        if (studentId == null) return List.of();
        return map.values().stream()
            .filter(wr -> wr.getRequestedBy().getUserId().equals(studentId))
            .collect(Collectors.toList());
    }

    /**
     * Finds the first withdrawal request for a specific application.
     *
     * <p>Note: Typically, there should be only one withdrawal request per application,
     * but this method returns the first match if multiple exist.
     *
     * @param applicationId the application ID to search for
     * @return an {@code Optional} containing the withdrawal request, or empty if not found
     */
    public Optional<WithdrawalRequest> findByApplicationId(String applicationId) {
        if (applicationId == null) return Optional.empty();
        return map.values().stream()
            .filter(wr -> wr.getApplication().getApplicationId().equals(applicationId))
            .findFirst();
    }

    /**
     * Finds all pending withdrawal requests.
     *
     * <p>This is primarily used by career center staff to see requests awaiting review
     * in their inbox.
     *
     * @return a list of all pending withdrawal requests
     * @see entity.WithdrawalRequestStatus#PENDING
     */
    public List<WithdrawalRequest> findPending() {
        return map.values().stream()
            .filter(wr -> wr.getStatus() == WithdrawalRequestStatus.PENDING)
            .collect(Collectors.toList());
    }

    /**
     * Counts pending withdrawal requests affecting a specific internship.
     *
     * <p>This can be used for dashboard displays to show how many pending withdrawal
     * requests exist for a particular internship posting.
     *
     * @param internshipId the internship ID to count requests for
     * @return the count of pending withdrawal requests for the specified internship
     */
    public long countPendingForInternship(String internshipId) {
        if (internshipId == null) return 0;
        return map.values().stream()
            .filter(wr -> wr.getStatus() == WithdrawalRequestStatus.PENDING)
            .filter(wr -> wr.getApplication().getInternship().getInternshipId().equals(internshipId))
            .count();
    }
    
    /**
     * Saves all withdrawal requests to a CSV file with standardized column headers.
     *
     * <p>The CSV file is created with the following columns in this exact order:
     * <ol>
     *   <li>RequestID - Unique withdrawal request identifier</li>
     *   <li>ApplicationID - Reference to the associated application</li>
     *   <li>StudentID - Reference to the student who made the request</li>
     *   <li>RequestedOn - Date when the request was made (YYYY-MM-DD)</li>
     *   <li>Reason - Student's reason for withdrawal</li>
     *   <li>Status - Current status (PENDING, APPROVED, REJECTED)</li>
     *   <li>ProcessedBy - Staff member who processed the request (if any)</li>
     *   <li>ProcessedOn - Date when the request was processed (YYYY-MM-DD)</li>
     *   <li>StaffNote - Notes from staff regarding the decision</li>
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
    
    /**
     * Loads withdrawal requests from a CSV file and resolves references to applications,
     * students, and staff using the provided repositories.
     *
     * <p>This method performs entity resolution where:
     * <ul>
     *   <li>Application IDs are resolved using the {@code applications} repository</li>
     *   <li>Student IDs are resolved using the {@code students} repository</li>
     *   <li>Staff IDs are resolved using the {@code staffRepo} repository</li>
     * </ul>
     * Rows with unresolved references (missing applications or students) are skipped
     * with an error message.
     *
     * <p>The CSV file can use various column headers for each field:
     * <ul>
     *   <li><b>Application ID:</b> "applicationid", "app", "application"</li>
     *   <li><b>Student ID:</b> "studentid", "student"</li>
     *   <li><b>Processed By:</b> "processedby", "processedbystaffid", "staffid"</li>
     *   <li><b>Staff Note:</b> "staffnote", "note"</li>
     * </ul>
     * 
     *
     * <p>Status restoration logic:
     * <ul>
     *   <li>APPROVED/REJECTED status requires a valid staff member for processing</li>
     *   <li>If staff reference is missing for processed requests, status remains PENDING</li>
     *   <li>PENDING status is maintained by default</li>
     * </ul>
     * 
     *
     * @param path the input CSV file path
     * @param applications the application repository for resolving application references
     * @param students the student repository for resolving student references
     * @param staffRepo the staff repository for resolving staff references
     * @throws java.io.IOException if an I/O error occurs while reading the file
     * @throws SecurityException if file access is denied
     */
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