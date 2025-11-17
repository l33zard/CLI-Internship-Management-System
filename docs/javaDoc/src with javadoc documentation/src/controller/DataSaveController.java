package controller;

import database.*;
import java.io.IOException;
import java.nio.file.*;

/**
 * Controller for managing data persistence operations across all system repositories.
 * 
 * <p>This controller handles the business logic for:
 * <ul>
 *   <li>Saving all system data to CSV files</li>
 *   <li>Loading all system data from CSV files</li>
 *   <li>Managing the data directory structure</li>
 *   <li>Coordinating persistence across multiple repositories</li>
 *   <li>Error handling and reporting for data operations</li>
 * </ul>
 * 
 * <p><strong>Data Persistence Strategy:</strong>
 * <ul>
 *   <li><strong>Format:</strong> CSV (Comma-Separated Values) for human readability</li>
 *   <li><strong>Directory:</strong> Configurable base directory (default: src/data)</li>
 *   <li><strong>Atomicity:</strong> All-or-nothing approach - reports all errors at once</li>
 *   <li><strong>Dependencies:</strong> Handles entity relationships during load operations</li>
 * </ul>
 * 
 * <p><strong>File Structure:</strong>
 * <pre>
 * src/data/
 * ├── students.csv          - Student accounts and profiles
 * ├── companyreps.csv       - Company representative accounts
 * ├── staffs.csv            - Career center staff accounts
 * ├── internships.csv       - Internship postings
 * ├── applications.csv      - Student applications (depends on students and internships)
 * └── withdrawals.csv       - Withdrawal requests (depends on applications)
 * </pre>
 * 
 * @see StudentRepository
 * @see CompanyRepRepository
 * @see CareerCenterStaffRepository
 * @see InternshipRepository
 * @see ApplicationRepository
 * @see WithdrawalRequestRepository
 * @see BaseController
 */
public class DataSaveController extends BaseController {

    /**
     * The base directory where all CSV data files are stored.
     * 
     * <p>This path is configurable via the constructor and defaults to "src/data".
     * The controller ensures this directory exists before performing any operations.
     */
    private final Path baseDir;

    /**
     * Constructs a DataSaveController with the default base directory "src/data".
     * 
     * <p>This constructor is typically used in production where the standard
     * data directory structure is desired.
     * 
     * @param students the repository for managing student data
     * @param reps the repository for managing company representative data
     * @param staff the repository for managing career center staff data
     * @param internships the repository for managing internship posting data
     * @param applications the repository for managing application data
     * @param withdrawals the repository for managing withdrawal request data
     */
    public DataSaveController(StudentRepository students,
                              CompanyRepRepository reps,
                              CareerCenterStaffRepository staff,
                              InternshipRepository internships,
                              ApplicationRepository applications,
                              WithdrawalRequestRepository withdrawals) {
        this(students, reps, staff, internships, applications, withdrawals, Paths.get("src", "data"));
    }

    /**
     * Constructs a DataSaveController with a custom base directory.
     * 
     * <p>This constructor allows specifying a custom data directory, useful for:
     * <ul>
     *   <li>Testing with temporary directories</li>
     *   <li>Using alternative storage locations</li>
     *   <li>Supporting multiple data environments</li>
     * </ul>
     * 
     * <p>The constructor automatically creates the base directory if it doesn't exist.
     * 
     * @param students the repository for managing student data
     * @param reps the repository for managing company representative data
     * @param staff the repository for managing career center staff data
     * @param internships the repository for managing internship posting data
     * @param applications the repository for managing application data
     * @param withdrawals the repository for managing withdrawal request data
     * @param baseDir the base directory for storing CSV files (defaults to "src/data" if null)
     */
    public DataSaveController(StudentRepository students,
                              CompanyRepRepository reps,
                              CareerCenterStaffRepository staff,
                              InternshipRepository internships,
                              ApplicationRepository applications,
                              WithdrawalRequestRepository withdrawals,
                              Path baseDir) {
        super(students, reps, staff, internships, applications, withdrawals);
        this.baseDir = baseDir == null ? Paths.get("src", "data") : baseDir;

        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            System.err.println("Warning: Could not create data directory: " + e.getMessage());
        }
    }

    /**
     * Saves all system data to CSV files in the base directory.
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Attempts to save each repository to its respective CSV file</li>
     *   <li>Continues attempting all saves even if some fail</li>
     *   <li>Collects all error messages encountered</li>
     *   <li>If any errors occurred, throws IOException with all error details</li>
     * </ol>
     * 
     * <p><strong>Files Created:</strong>
     * <ul>
     *   <li>{@code studentsCsv} - Student account data</li>
     *   <li>{@code repsCsv} - Company representative data</li>
     *   <li>{@code staffCsv} - Career center staff data</li>
     *   <li>{@code internshipsCsv} - Internship posting data</li>
     *   <li>{@code applicationsCsv} - Student application data</li>
     *   <li>{@code withdrawalsCsv} - Withdrawal request data</li>
     * </ul>
     * 
     * <p><strong>Error Handling:</strong>
     * The method uses a "continue on error" approach, attempting to save all
     * repositories even if some fail. This ensures maximum data preservation
     * in case of partial failures.
     * 
     * @param studentsCsv the filename for student data (e.g., "students.csv")
     * @param repsCsv the filename for company representative data
     * @param staffCsv the filename for staff data
     * @param internshipsCsv the filename for internship data
     * @param applicationsCsv the filename for application data
     * @param withdrawalsCsv the filename for withdrawal request data
     * @throws IOException if any save operation fails, with details of all failures
     */
    public void saveAll(String studentsCsv,
                        String repsCsv,
                        String staffCsv,
                        String internshipsCsv,
                        String applicationsCsv,
                        String withdrawalsCsv) throws IOException {

        boolean hasErrors = false;
        StringBuilder errorMessages = new StringBuilder();

        try {
            Path sPath = baseDir.resolve(studentsCsv);
            studentRepo.saveToCsv(sPath.toString());
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to save students: ").append(e.getMessage()).append("\n");
        }

        try {
            Path rPath = baseDir.resolve(repsCsv);
            repRepo.saveToCsv(rPath.toString());
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to save company reps: ").append(e.getMessage()).append("\n");
        }

        try {
            Path stPath = baseDir.resolve(staffCsv);
            staffRepo.saveToCsv(stPath.toString());
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to save staff: ").append(e.getMessage()).append("\n");
        }

        try {
            Path iPath = baseDir.resolve(internshipsCsv);
            internshipRepo.saveToCsv(iPath.toString());
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to save internships: ").append(e.getMessage()).append("\n");
        }

        try {
            Path aPath = baseDir.resolve(applicationsCsv);
            applicationRepo.saveToCsv(aPath.toString());
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to save applications: ").append(e.getMessage()).append("\n");
        }

        try {
            Path wPath = baseDir.resolve(withdrawalsCsv);
            withdrawalRepo.saveToCsv(wPath.toString());
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to save withdrawal requests: ").append(e.getMessage()).append("\n");
        }

        if (hasErrors) {
            throw new IOException("Some data failed to save:\n" + errorMessages.toString());
        }
    }

    /**
     * Loads all system data from CSV files in the base directory.
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Checks if each CSV file exists before attempting to load</li>
     *   <li>Loads repositories in dependency order:
     *     <ul>
     *       <li>Students (no dependencies)</li>
     *       <li>Company representatives (no dependencies)</li>
     *       <li>Staff (no dependencies)</li>
     *       <li>Internships (no dependencies)</li>
     *       <li>Applications (depends on students and internships)</li>
     *       <li>Withdrawals (depends on applications, students, and staff)</li>
     *     </ul>
     *   </li>
     *   <li>Continues attempting all loads even if some fail</li>
     *   <li>Collects all error messages encountered</li>
     *   <li>If any errors occurred, throws IOException with all error details</li>
     * </ol>
     * 
     * <p><strong>Missing Files:</strong>
     * If a file doesn't exist, the system logs a message but doesn't treat it as an error.
     * This allows for partial system initialization with only available data.
     * 
     * <p><strong>Dependency Management:</strong>
     * Some repositories require references to other repositories during loading
     * (e.g., applications need to link to students and internships). The load
     * order ensures these dependencies are available.
     * 
     * @param studentsCsv the filename for student data (e.g., "students.csv")
     * @param repsCsv the filename for company representative data
     * @param staffCsv the filename for staff data
     * @param internshipsCsv the filename for internship data
     * @param applicationsCsv the filename for application data
     * @param withdrawalsCsv the filename for withdrawal request data
     * @throws IOException if any load operation fails, with details of all failures
     */
    public void loadAll(String studentsCsv,
                        String repsCsv,
                        String staffCsv,
                        String internshipsCsv,
                        String applicationsCsv,
                        String withdrawalsCsv) throws IOException {

        boolean hasErrors = false;
        StringBuilder errorMessages = new StringBuilder();

        try {
            Path sPath = baseDir.resolve(studentsCsv);
            if (Files.exists(sPath)) {
                studentRepo.loadFromCsv(sPath.toString());
            } else {
                System.out.println("Students file not found: " + sPath);
            }
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to load students: ").append(e.getMessage()).append("\n");
        }

        try {
            Path rPath = baseDir.resolve(repsCsv);
            if (Files.exists(rPath)) {
                repRepo.loadFromCsv(rPath.toString());
            } else {
                System.out.println("Company reps file not found: " + rPath);
            }
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to load company reps: ").append(e.getMessage()).append("\n");
        }

        try {
            Path stPath = baseDir.resolve(staffCsv);
            if (Files.exists(stPath)) {
                staffRepo.loadFromCsv(stPath.toString());
            } else {
                System.out.println("Staff file not found: " + stPath);
            }
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to load staff: ").append(e.getMessage()).append("\n");
        }

        try {
            Path iPath = baseDir.resolve(internshipsCsv);
            if (Files.exists(iPath)) {
                internshipRepo.loadFromCsv(iPath.toString());
            } else {
                System.out.println("Internships file not found: " + iPath);
            }
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to load internships: ").append(e.getMessage()).append("\n");
        }

        try {
            Path aPath = baseDir.resolve(applicationsCsv);
            if (Files.exists(aPath)) {
                applicationRepo.loadFromCsv(aPath.toString(), studentRepo, internshipRepo);
            } else {
                System.out.println("Applications file not found: " + aPath);
            }
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to load applications: ").append(e.getMessage()).append("\n");
        }

        try {
            Path wPath = baseDir.resolve(withdrawalsCsv);
            if (Files.exists(wPath)) {
                withdrawalRepo.loadFromCsv(wPath.toString(), applicationRepo, studentRepo, staffRepo);
            } else {
                System.out.println("Withdrawal requests file not found: " + wPath);
            }
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to load withdrawal requests: ").append(e.getMessage()).append("\n");
        }

        if (hasErrors) {
            throw new IOException("Some data failed to load:\n" + errorMessages.toString());
        }
    }

    /**
     * Returns the base directory path where CSV files are stored.
     * 
     * <p>Useful for:
     * <ul>
     *   <li>Verifying the data directory location</li>
     *   <li>Building full paths to specific files</li>
     *   <li>Debugging data persistence issues</li>
     * </ul>
     * 
     * @return the base directory path
     */
    public Path getBaseDir() {
        return baseDir;
    }

    /**
     * Checks if all required repositories have been initialized.
     * 
     * <p>Verifies that all six repositories (students, company reps, staff,
     * internships, applications, and withdrawals) are not null. This is
     * important before attempting any data operations.
     * 
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Pre-flight checks before save/load operations</li>
     *   <li>System initialization validation</li>
     *   <li>Debugging configuration issues</li>
     * </ul>
     * 
     * @return true if all repositories are initialized (not null), false otherwise
     */
    public boolean isInitialized() {
        return studentRepo != null && repRepo != null && staffRepo != null && 
               internshipRepo != null && applicationRepo != null && withdrawalRepo != null;
    }
}