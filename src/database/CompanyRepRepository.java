package database;

import entity.CompanyRep;
import util.CsvUtils;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * In-memory repository for managing {@link entity.CompanyRep} entities.
 *
 * <p>This repository provides CRUD operations for company representative records and supports
 * CSV-based persistence. Company representatives are uniquely identified by their email address,
 * which serves as the primary key for all operations.
 *
 * <p>The repository maintains case-insensitive email handling and provides CSV load/save
 * functionality with flexible header parsing. The CSV schema includes comprehensive company
 * representative information including approval status and rejection reasons.
 *
 */
public class CompanyRepRepository implements CrudRepository<CompanyRep, String> {

    private final Map<String, CompanyRep> map = new LinkedHashMap<>();

    // ---------- CRUD ----------

    /**
     * Finds a company representative by their email address.
     *
     * <p>Email matching is case-insensitive. The email is normalized to lowercase
     * before searching.
     *
     * @param email the email address to search for
     * @return an {@code Optional} containing the found representative, or empty if not found
     */
    @Override
    public Optional<CompanyRep> findById(String email) {
        if (email == null) return Optional.empty();
        return Optional.ofNullable(map.get(email.trim().toLowerCase()));
    }

    /**
     * Retrieves all company representatives in the repository.
     *
     * @return a list of all company representatives, maintaining insertion order
     */
    @Override
    public List<CompanyRep> findAll() {
        return new ArrayList<>(map.values());
    }

    /**
     * Saves a company representative to the repository.
     *
     * <p>If a representative with the same email already exists, it will be replaced.
     * The email address is normalized to lowercase for storage.
     *
     * @param rep the company representative to save
     * @return the saved company representative
     * @throws NullPointerException if the representative is null
     */
    @Override
    public CompanyRep save(CompanyRep rep) {
        map.put(rep.getEmail().trim().toLowerCase(), rep);
        return rep;
    }

    /**
     * Saves all company representatives in the provided iterable.
     *
     * @param entities the company representatives to save
     * @return a list of the saved company representatives
     * @throws NullPointerException if the iterable or any element is null
     */
    @Override
    public List<CompanyRep> saveAll(Iterable<CompanyRep> entities) {
        List<CompanyRep> out = new ArrayList<>();
        for (CompanyRep e : entities) { save(e); out.add(e); }
        return out;
    }

    /**
     * Deletes a company representative by their email address.
     *
     * <p>Email matching is case-insensitive.
     *
     * @param email the email address of the representative to delete
     */
    @Override
    public void deleteById(String email) {
        if (email != null) map.remove(email.trim().toLowerCase());
    }

    /**
     * Deletes all company representatives with the specified email addresses.
     *
     * @param emails the email addresses of representatives to delete
     * @throws NullPointerException if the iterable is null
     */
    @Override
    public void deleteAllById(Iterable<String> emails) {
        for (String e : emails) deleteById(e);
    }

    /**
     * Deletes all company representatives from the repository.
     */
    @Override
    public void deleteAll() {
        map.clear();
    }

    /**
     * Checks whether a company representative with the given email exists.
     *
     * <p>Email matching is case-insensitive.
     * @param email the email address to check
     * @return true if a representative with the email exists, false otherwise
     */
    @Override
    public boolean existsById(String email) {
        return email != null && map.containsKey(email.trim().toLowerCase());
    }

    /**
     * Returns the total number of company representatives in the repository.
     *
     * @return the count of company representatives
     */
    @Override
    public long count() {
        return map.size();
    }

    // ---------- Helper ----------

    /**
     * Finds a company representative by their email address.
     *
     * <p>This is an alias for {@link #findById(String)} provided for semantic clarity
     * when searching by email.
     *
     * @param email the email address to search for
     * @return an {@code Optional} containing the found representative, or empty if not found
     * @see #findById(String)
     */
    public Optional<CompanyRep> findByEmail(String email) {
        return findById(email);
    }

    // ---------- CSV PERSISTENCE ----------

    /**
     * Saves all company representatives to a CSV file with standardized column headers.
     *
     * <p>The CSV file is created with the following columns in this exact order:
     * <ol>
     *   <li>CompanyRepID - The representative's email address</li>
     *   <li>Name - The representative's full name</li>
     *   <li>CompanyName - The company name</li>
     *   <li>Department - The department within the company</li>
     *   <li>Position - The representative's job position</li>
     *   <li>Email - The email address (duplicate of CompanyRepID)</li>
     *   <li>Status - Approval status (PENDING, APPROVED, or REJECTED)</li>
     *   <li>Password - The account password</li>
     *   <li>RejectionReason - Reason for rejection if status is REJECTED</li>
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
            bw.write("CompanyRepID,Name,CompanyName,Department,Position,Email,Status,Password,RejectionReason");
            bw.newLine();
            for (var r : map.values()) {
                String status;
                if (r.isApproved()) status = "APPROVED";
                else if (r.isRejected()) status = "REJECTED";
                else status = "PENDING";

                bw.write(String.join(",",
                    CsvUtils.esc(r.getEmail()),          // CompanyRepID
                    CsvUtils.esc(r.getName()),
                    CsvUtils.esc(r.getCompanyName()),
                    CsvUtils.esc(r.getDepartment()),
                    CsvUtils.esc(r.getPosition()),
                    CsvUtils.esc(r.getEmail()),
                    CsvUtils.esc(status),
                    CsvUtils.esc(r.getPassword()),
                    CsvUtils.esc(r.getRejectionReason() != null ? r.getRejectionReason() : "")
                ));
                bw.newLine();
            }
        }
    }

    /**
     * Loads company representatives from a CSV file with flexible header recognition.
     *
     * <p>The CSV file can use various column headers for each field:
     * <ul>
     *   <li><b>Email/CompanyRepID:</b> "companyrepid", "email"</li>
     *   <li><b>Name:</b> "name"</li>
     *   <li><b>Company Name:</b> "companyname"</li>
     *   <li><b>Department:</b> "department"</li>
     *   <li><b>Position:</b> "position"</li>
     *   <li><b>Status:</b> "status"</li>
     *   <li><b>Password:</b> "password"</li>
     *   <li><b>Rejection Reason:</b> "rejectionreason"</li>
     * </ul>
     * 
     *
     * <p>If the email/CompanyRepID is missing from a row, that row is skipped.
     * Missing optional fields are handled as follows:
     * <ul>
     *   <li>Password defaults to "password" if not specified</li>
     *   <li>Status defaults to PENDING if not specified or unrecognized</li>
     *   <li>Rejection reason defaults to "Rejected by staff" for REJECTED status</li>
     * </ul>
     * 
     *
     * <p>Supported status values: "APPROVED", "REJECTED", "PENDING" (case-insensitive)
     *
     * @param path the input CSV file path
     * @throws java.io.IOException if an I/O error occurs while reading the file
     * @throws SecurityException if file access is denied
     */
    public void loadFromCsv(String path) throws java.io.IOException {
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            System.out.println("Company reps file not found: " + path);
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
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] row = CsvUtils.splitCsv(line);

                try {
                    String email = getCsvValue(row, idx, "companyrepid", "email");
                    String name = getCsvValue(row, idx, "name");
                    String company = getCsvValue(row, idx, "companyname");
                    String dept = getCsvValue(row, idx, "department");
                    String pos = getCsvValue(row, idx, "position");
                    String statusStr = getCsvValue(row, idx, "status");
                    String password = getCsvValue(row, idx, "password");
                    String rejectionReason = getCsvValue(row, idx, "rejectionreason");

                    if (email == null || email.isBlank()) continue;

                    var rep = new CompanyRep(email, name, company, dept, pos, email);
                    
                    // Set password (default if not provided)
                    if (password != null && !password.isEmpty()) {
                        rep.setPassword(password);
                    } else {
                        rep.setPassword("password");
                    }

                    // Set status
                    if (statusStr != null) {
                        switch (statusStr.trim().toUpperCase()) {
                            case "APPROVED" -> rep.approve();
                            case "REJECTED" -> rep.reject(rejectionReason != null ? rejectionReason : "Rejected by staff");
                            default -> { /* keep as pending */ }
                        }
                    }

                    save(rep);
                } catch (Exception e) {
                    System.err.println("Error parsing company rep line: " + e.getMessage());
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