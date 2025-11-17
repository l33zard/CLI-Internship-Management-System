package database;

import entity.CompanyRep;
import util.CsvUtils;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * In-memory repository for CompanyRep entities with approval status tracking and CSV persistence.
 * 
 * <p>This repository manages company representative accounts through their complete lifecycle
 * from registration through approval/rejection. It handles authentication, company affiliation,
 * and access control for the internship posting system.
 * 
 * <p><strong>Account Lifecycle States:</strong>
 * <ul>
 *   <li><strong>PENDING:</strong> Newly registered, awaiting career center staff review</li>
 *   <li><strong>APPROVED:</strong> Verified by staff, can log in and post internships</li>
 *   <li><strong>REJECTED:</strong> Denied access, includes rejection reason</li>
 * </ul>
 * 
 * <p><strong>Company Representative Data:</strong>
 * <ul>
 *   <li><strong>Identity:</strong> Name, email (used as unique ID)</li>
 *   <li><strong>Affiliation:</strong> Company name, department, position/title</li>
 *   <li><strong>Authentication:</strong> Password for system login</li>
 *   <li><strong>Status:</strong> Approval state and rejection reason (if applicable)</li>
 * </ul>
 * 
 * <p><strong>Email Normalization:</strong>
 * All email addresses are automatically normalized to lowercase to ensure
 * case-insensitive matching. This prevents duplicate accounts due to
 * capitalization differences (e.g., "John@Company.com" vs "john@company.com").
 * 
 * <p><strong>CSV Format:</strong>
 * <pre>
 * Header: CompanyRepID,Name,CompanyName,Department,Position,Email,Status,Password,RejectionReason
 * Example: john@acme.com,John Smith,Acme Corp,HR,Recruiter,john@acme.com,APPROVED,password123,
 * </pre>
 * 
 * <p><strong>Design Notes:</strong>
 * <ul>
 *   <li>Uses LinkedHashMap for stable iteration order in CSV exports</li>
 *   <li>Email serves as both the unique ID and login credential</li>
 *   <li>Supports saving/loading representatives in any approval state</li>
 *   <li>Gracefully handles missing or malformed CSV data</li>
 * </ul>
 * 
 * @see CompanyRep
 * @see CrudRepository
 */
public class CompanyRepRepository implements CrudRepository<CompanyRep, String> {

    /**
     * Internal storage map for company representatives, keyed by normalized email address.
     * 
     * <p>LinkedHashMap maintains insertion order for predictable CSV exports
     * and stable iteration. Keys are always stored in lowercase.
     */
    private final Map<String, CompanyRep> map = new LinkedHashMap<>();

    // ---------- CRUD ----------
    
    /**
     * {@inheritDoc}
     * 
     * <p>Finds a company representative by their email address (case-insensitive).
     * 
     * <p>The email is automatically normalized to lowercase before lookup,
     * ensuring "John@Company.com" and "john@company.com" refer to the same account.
     * 
     * @param email the representative's email address (case-insensitive)
     * @return an Optional containing the representative if found, empty otherwise
     */
    @Override
    public Optional<CompanyRep> findById(String email) {
        if (email == null) return Optional.empty();
        return Optional.ofNullable(map.get(email.trim().toLowerCase()));
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Returns all company representatives regardless of approval status
     * (pending, approved, or rejected).
     */
    @Override
    public List<CompanyRep> findAll() {
        return new ArrayList<>(map.values());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Saves a company representative (insert or update).
     * 
     * <p>The email address is normalized to lowercase before storage, ensuring
     * case-insensitive uniqueness.
     * 
     * @param rep the company representative to save
     * @return the saved representative
     */
    @Override
    public CompanyRep save(CompanyRep rep) {
        map.put(rep.getEmail().trim().toLowerCase(), rep);
        return rep;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Saves multiple company representatives in a single operation.
     */
    @Override
    public List<CompanyRep> saveAll(Iterable<CompanyRep> entities) {
        List<CompanyRep> out = new ArrayList<>();
        for (CompanyRep e : entities) { save(e); out.add(e); }
        return out;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Deletes a company representative by email address (case-insensitive).
     * 
     * @param email the email address of the representative to delete
     */
    @Override
    public void deleteById(String email) {
        if (email != null) map.remove(email.trim().toLowerCase());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Deletes multiple representatives by their email addresses.
     */
    @Override
    public void deleteAllById(Iterable<String> emails) {
        for (String e : emails) deleteById(e);
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Warning:</strong> This removes all company representative data.
     */
    @Override
    public void deleteAll() {
        map.clear();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Checks if a representative exists by email address (case-insensitive).
     */
    @Override
    public boolean existsById(String email) {
        return email != null && map.containsKey(email.trim().toLowerCase());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Returns the total number of company representatives
     * (all statuses: pending, approved, and rejected).
     */
    @Override
    public long count() {
        return map.size();
    }

    // ---------- Helper ----------
    
    /**
     * Finds a company representative by email address.
     * 
     * <p>This is a convenience alias for {@link #findById(String)} that
     * provides a more semantically meaningful method name when searching
     * by email specifically.
     * 
     * @param email the representative's email address (case-insensitive)
     * @return an Optional containing the representative if found, empty otherwise
     */
    public Optional<CompanyRep> findByEmail(String email) {
        return findById(email);
    }

    // ---------- CSV PERSISTENCE ----------
    
    /**
     * Saves all company representatives to a CSV file.
     * 
     * <p>Exports representatives in all approval states (pending, approved, rejected)
     * to enable complete data backup and restoration.
     * 
     * <p><strong>CSV Format Generated:</strong>
     * <pre>
     * Header: CompanyRepID,Name,CompanyName,Department,Position,Email,Status,Password,RejectionReason
     * Row:    john@acme.com,John Smith,Acme Corp,HR,Recruiter,john@acme.com,APPROVED,pass123,
     * </pre>
     * 
     * <p><strong>Status Values:</strong>
     * <ul>
     *   <li>"APPROVED" - Representative can access the system</li>
     *   <li>"REJECTED" - Representative denied access, reason included</li>
     *   <li>"PENDING" - Awaiting staff approval (default for new registrations)</li>
     * </ul>
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Create parent directories if needed</li>
     *   <li>Open file for writing (create new or truncate existing)</li>
     *   <li>Write CSV header</li>
     *   <li>For each representative:
     *     <ul>
     *       <li>Determine status string from approval flags</li>
     *       <li>Escape all string fields for CSV safety</li>
     *       <li>Write comma-separated row</li>
     *     </ul>
     *   </li>
     * </ol>
     * 
     * @param path the file path where the CSV will be written
     * @throws java.io.IOException if there is an error writing the file
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
     * Loads company representatives from a CSV file.
     * 
     * <p>Imports representatives in all approval states, restoring their
     * complete status including rejection reasons.
     * 
     * <p><strong>CSV Format Expected:</strong>
     * <pre>
     * Header: CompanyRepID,Name,CompanyName,Department,Position,Email,Status,Password,RejectionReason
     * Row:    john@acme.com,John Smith,Acme Corp,HR,Recruiter,john@acme.com,APPROVED,pass123,
     * </pre>
     * 
     * <p><strong>Flexible Header Recognition:</strong>
     * Accepts multiple variations of column names:
     * <ul>
     *   <li><strong>Email/ID:</strong> "companyrepid", "email"</li>
     *   <li><strong>Name:</strong> "name"</li>
     *   <li><strong>Company:</strong> "companyname"</li>
     *   <li><strong>Department:</strong> "department"</li>
     *   <li><strong>Position:</strong> "position"</li>
     *   <li><strong>Status:</strong> "status" (PENDING/APPROVED/REJECTED)</li>
     *   <li><strong>Password:</strong> "password"</li>
     *   <li><strong>Reason:</strong> "rejectionreason"</li>
     * </ul>
     * 
     * <p><strong>Status Processing:</strong>
     * <ul>
     *   <li><strong>APPROVED:</strong> Calls approve() method on entity</li>
     *   <li><strong>REJECTED:</strong> Calls reject(reason) with stored reason</li>
     *   <li><strong>PENDING:</strong> Leaves in default pending state</li>
     *   <li><strong>Missing/Invalid:</strong> Defaults to PENDING</li>
     * </ul>
     * 
     * <p><strong>Password Handling:</strong>
     * <ul>
     *   <li>Uses password from CSV if present</li>
     *   <li>Defaults to "password" if missing or empty</li>
     * </ul>
     * 
     * <p><strong>Error Handling:</strong>
     * <ul>
     *   <li>File not found: Prints message and returns</li>
     *   <li>Missing email: Skips line silently</li>
     *   <li>Parse errors: Prints error and continues with next line</li>
     * </ul>
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Check if file exists (return if not found)</li>
     *   <li>Parse header to create column index</li>
     *   <li>For each data line:
     *     <ul>
     *       <li>Extract all field values using flexible matching</li>
     *       <li>Skip if email is missing</li>
     *       <li>Create CompanyRep entity</li>
     *       <li>Set password (from CSV or default)</li>
     *       <li>Apply approval status based on Status column</li>
     *       <li>Save to repository</li>
     *     </ul>
     *   </li>
     * </ol>
     * 
     * @param path the file path to the CSV file
     * @throws java.io.IOException if there is an error reading the file
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
     * Helper method to retrieve CSV cell values with flexible header name matching.
     * 
     * <p>Enables the CSV loader to work with different file formats by accepting
     * multiple possible column names. Performs case-insensitive matching.
     * 
     * @param row the array of cell values from a CSV line
     * @param idx the header-to-index mapping (lowercase header names)
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