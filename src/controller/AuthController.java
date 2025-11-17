package controller;

import database.CareerCenterStaffRepository;
import database.CompanyRepRepository;
import database.StudentRepository;
import entity.CareerCenterStaff;
import entity.CompanyRep;
import entity.Student;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication controller responsible for validating login credentials and
 * managing in-memory password store used by the demo application.
 * <p>
 * This controller provides authentication services for all user types in the system:
 * <ul>
 *   <li>Students (authenticated by student ID)</li>
 *   <li>Company Representatives (authenticated by email)</li>
 *   <li>Career Center Staff (authenticated by staff ID)</li>
 * </ul>
 * 
 * <strong>Security Note:</strong> This implementation uses simple in-memory password
 * mapping with plain-text storage and is intended for demo/testing only. Production
 * systems should use secure hashed storage with proper salting and key derivation.
 * 
 */
public class AuthController extends BaseController {

    /**
     * Roles understood by the authentication system.
     */
    public enum Role { 
        /** Student user role */ 
        STUDENT, 
        /** Company representative user role */ 
        COMPANY_REP, 
        /** Career center staff user role */ 
        STAFF 
    }

    /**
     * In-memory password map keyed by a role-qualified key (e.g. "STUDENT:U1234567A").
     * Values are plain-text passwords for demo/testing only.
     * 
     * <strong>Security Warning:</strong> Plain-text password storage is not secure
     * and should not be used in production environments.
     */
    private final Map<String, String> pw = new ConcurrentHashMap<>();

    /**
     * No-argument constructor used for lightweight instantiation in tests.
     * <p>
     * Creates an AuthController without repository dependencies. The controller
     * must be properly initialized with repositories before use in production.
     */
    public AuthController() {
        super(null, null, null, null, null, null);
    }

    /**
     * Creates an AuthController wired to the given repositories and seeds default
     * password entries from their contents.
     * <p>
     * Automatically populates the password map with default entries ("password")
     * for all users found in the provided repositories.
     *
     * @param students student repository for student account authentication
     * @param reps company representative repository for rep account authentication
     * @param staff career center staff repository for staff account authentication
     */
    public AuthController(StudentRepository students,
                         CompanyRepRepository reps,
                         CareerCenterStaffRepository staff) {
        super(students, reps, staff, null, null, null);
        // Call seedDefaults after super() to ensure repositories are set
        seedDefaults(students, reps, staff);
    }

    /**
     * Populates the internal password map with default entries for loaded users.
     * <p>
     * For each user found in the repositories, creates a default password entry
     * with the value "password". Existing entries are preserved (not overwritten).
     *
     * @param students student repository containing student accounts
     * @param reps company representative repository containing rep accounts
     * @param staff career center staff repository containing staff accounts
     */
    public void seedDefaults(StudentRepository students,
                             CompanyRepRepository reps,
                             CareerCenterStaffRepository staff) {
        // These repositories are now available through the base class fields
        if (studentRepo != null) {
            for (Student s : studentRepo.findAll()) {
                String key = key(Role.STUDENT, s.getUserId());
                pw.putIfAbsent(key, "password");
            }
        }
        if (repRepo != null) {
            for (CompanyRep r : repRepo.findAll()) {
                String key = key(Role.COMPANY_REP, normEmail(r.getEmail()));
                pw.putIfAbsent(key, "password");
            }
        }
        if (staffRepo != null) {
            for (CareerCenterStaff st : staffRepo.findAll()) {
                String key = key(Role.STAFF, st.getUserId());
                pw.putIfAbsent(key, "password");
            }
        }
    }

    /**
     * Attempts to authenticate a user by ID or email and returns a role-qualified key
     * on success.
     * <p>
     * The authentication process:
     * <ol>
     *   <li>Validates input parameters</li>
     *   <li>Determines user type based on login key format (email vs ID)</li>
     *   <li>Verifies account exists and is approved (for company reps)</li>
     *   <li>Checks password against stored value</li>
     * </ol>
     * 
     * Company representatives must be approved by staff before they can log in.
     *
     * @param loginKey ID or email used to identify the account
     * @param password password attempt
     * @return role-qualified principal string in the format "ROLE:identifier" 
     *         (e.g., "STUDENT:U1234567A", "COMPANY_REP:rep@company.com")
     * @throws IllegalArgumentException when credentials are invalid, account not found,
     *         or company representative account is pending approval
     * @throws IllegalStateException if authentication system is not properly initialized
     */
    public String login(String loginKey, String password) {
        requireRepos();
        
        if (loginKey == null || loginKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Login ID/email cannot be empty.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        String keyRaw = loginKey.trim();
        String pass   = password;

        if (keyRaw.contains("@")) {
            String emailNorm = normEmail(keyRaw);
            CompanyRep rep = repRepo.findByEmail(emailNorm)
                    .orElseThrow(() -> new IllegalArgumentException("Company representative not found with email: " + keyRaw));
            if (!rep.isApproved()) {
                throw new IllegalArgumentException("Account pending approval by Career Center staff.");
            }
            String authKey = key(Role.COMPANY_REP, emailNorm);
            String expected = pw.getOrDefault(authKey, "password");
            if (!Objects.equals(expected, pass)) throw new IllegalArgumentException("Incorrect password.");
            return Role.COMPANY_REP + ":" + rep.getEmail();
        }

        Optional<Student> os = studentRepo.findById(keyRaw);
        if (os.isPresent()) {
            String authKey = key(Role.STUDENT, os.get().getUserId());
            String expected = pw.getOrDefault(authKey, "password");
            if (!Objects.equals(expected, pass)) throw new IllegalArgumentException("Incorrect password.");
            return Role.STUDENT + ":" + os.get().getUserId();
        }

        Optional<CareerCenterStaff> ost = staffRepo.findById(keyRaw);
        if (ost.isPresent()) {
            String authKey = key(Role.STAFF, ost.get().getUserId());
            String expected = pw.getOrDefault(authKey, "password");
            if (!Objects.equals(expected, pass)) throw new IllegalArgumentException("Incorrect password.");
            return Role.STAFF + ":" + ost.get().getUserId();
        }

        throw new IllegalArgumentException("ID/Email does not exist, please check.");
    }

    /**
     * Changes a user's password after verifying the existing password.
     * <p>
     * Validates that:
     * <ul>
     *   <li>All input parameters are provided and non-empty</li>
     *   <li>The current password matches the stored value</li>
     *   <li>The new password is not the default "password" value</li>
     *   <li>The user account exists in the system</li>
     * </ul>
     *
     * @param loginKey ID or email of the user
     * @param oldPw current password for verification
     * @param newPw new password to set
     * @throws IllegalArgumentException when validation fails, passwords don't match,
     *         or user not found
     * @throws IllegalStateException if authentication system is not properly initialized
     */
    public void changePassword(String loginKey, String oldPw, String newPw) {
        requireRepos();
        
        if (loginKey == null || loginKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Login ID/email cannot be empty.");
        }
        if (oldPw == null || oldPw.isEmpty()) {
            throw new IllegalArgumentException("Current password cannot be empty.");
        }
        if (newPw == null || newPw.isEmpty()) {
            throw new IllegalArgumentException("New password cannot be empty.");
        }
        if (newPw.equals("password")) {
            throw new IllegalArgumentException("New password cannot be the default password.");
        }

        String keyRaw = loginKey.trim();

        if (keyRaw.contains("@")) {
            String emailNorm = normEmail(keyRaw);
            repRepo.findByEmail(emailNorm).orElseThrow(() -> new IllegalArgumentException("Company representative not found: " + keyRaw));
            String authKey = key(Role.COMPANY_REP, emailNorm);
            verifyAndSet(authKey, oldPw, newPw);
            return;
        }

        if (studentRepo.findById(keyRaw).isPresent()) {
            String authKey = key(Role.STUDENT, keyRaw);
            verifyAndSet(authKey, oldPw, newPw);
            return;
        }

        if (staffRepo.findById(keyRaw).isPresent()) {
            String authKey = key(Role.STAFF, keyRaw);
            verifyAndSet(authKey, oldPw, newPw);
            return;
        }

        throw new IllegalArgumentException("User not found: " + keyRaw);
    }
    
    /**
     * Verifies credentials without throwing exceptions; returns true on match.
     * <p>
     * This is a silent version of the login method that returns a boolean instead
     * of throwing exceptions for invalid credentials. Useful for validation checks
     * where exception handling is not desired.
     *
     * @param loginKey ID or email
     * @param password password attempt
     * @return true when credentials are valid and account exists, false otherwise
     * @throws IllegalStateException if authentication system is not properly initialized
     */
    public boolean verify(String loginKey, String password) {
        requireRepos();
        
        if (loginKey == null || loginKey.trim().isEmpty() || password == null) {
            return false;
        }

        String keyRaw = loginKey.trim();
        String pass   = password;

        if (keyRaw.contains("@")) {
            String emailNorm = normEmail(keyRaw);
            if (repRepo.findByEmail(emailNorm).isEmpty()) {
                return false;
            }
            String authKey = key(Role.COMPANY_REP, emailNorm);
            String expected = pw.getOrDefault(authKey, "password");
            return Objects.equals(expected, pass);
        }

        if (studentRepo.findById(keyRaw).isPresent()) {
            String authKey = key(Role.STUDENT, keyRaw);
            String expected = pw.getOrDefault(authKey, "password");
            return Objects.equals(expected, pass);
        }

        if (staffRepo.findById(keyRaw).isPresent()) {
            String authKey = key(Role.STAFF, keyRaw);
            String expected = pw.getOrDefault(authKey, "password");
            return Objects.equals(expected, pass);
        }

        return false;
    }

    /**
     * Verifies the existing password for the given auth key and updates it to the new password.
     *
     * @param authKey role-qualified authentication key
     * @param oldPw current password to verify
     * @param newPw new password to set
     * @throws IllegalArgumentException if the current password does not match
     */
    private void verifyAndSet(String authKey, String oldPw, String newPw) {
        String current = pw.getOrDefault(authKey, "password");
        if (!Objects.equals(current, oldPw)) throw new IllegalArgumentException("Current password is incorrect.");
        pw.put(authKey, newPw);
    }

    /**
     * Asserts that the repositories required by authentication are available.
     *
     * @throws IllegalStateException if any required repository is missing
     */
    private void requireRepos() {
        if (studentRepo == null || repRepo == null || staffRepo == null) {
            throw new IllegalStateException("Authentication system not initialized. Please contact administrator.");
        }
    }

    /**
     * Builds the role-qualified map key used for password storage.
     *
     * @param r role of the user
     * @param id identifier (student ID, staff ID, or email)
     * @return string key in the format "ROLE:id"
     */
    private String key(Role r, String id) {
        return r.name() + ":" + ((id == null) ? "" : id.trim());
    }

    /**
     * Normalizes an email address for consistent lookups.
     * <p>
     * Converts email to lowercase and trims whitespace to ensure consistent
     * matching regardless of input formatting.
     *
     * @param email raw email address
     * @return trimmed lower-cased email, or empty string when null
     */
    private String normEmail(String email) {
        return (email == null) ? "" : email.trim().toLowerCase();
    }
    
    /**
     * Returns the inferred role for the given ID or email, or null if not found.
     * <p>
     * Determines user role based on:
     * <ul>
     *   <li>Email format (@ symbol) indicates company representative</li>
     *   <li>Student ID format indicates student</li>
     *   <li>Staff ID format indicates career center staff</li>
     * </ul>
     *
     * @param loginKey ID or email to look up
     * @return corresponding Role if user exists, null if not found
     * @throws IllegalStateException if authentication system is not properly initialized
     */
    public Role getUserRole(String loginKey) {
        requireRepos();
        
        if (loginKey == null || loginKey.trim().isEmpty()) {
            return null;
        }

        String keyRaw = loginKey.trim();

        if (keyRaw.contains("@")) {
            String emailNorm = normEmail(keyRaw);
            if (repRepo.findByEmail(emailNorm).isPresent()) {
                return Role.COMPANY_REP;
            }
        }

        if (studentRepo.findById(keyRaw).isPresent()) {
            return Role.STUDENT;
        }

        if (staffRepo.findById(keyRaw).isPresent()) {
            return Role.STAFF;
        }

        return null;
    }
}