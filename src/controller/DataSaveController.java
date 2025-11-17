package controller;

import database.*;
import java.io.IOException;
import java.nio.file.*;

/**
 * Controller responsible for loading and saving repository CSV files from/to
 * the configured base data directory.
 * <p>
 * This controller provides centralized data persistence management for all
 * system entities, handling both save and load operations for:
 * <ul>
 *   <li>Student records</li>
 *   <li>Company representative accounts</li>
 *   <li>Career center staff accounts</li>
 *   <li>Internship postings</li>
 *   <li>Internship applications</li>
 *   <li>Withdrawal requests</li>
 * </ul>
 * 
 * The controller uses a configurable base directory for CSV file storage and
 * automatically creates the directory structure if it doesn't exist. Operations
 * continue even if individual repository operations fail, with comprehensive
 * error reporting.
 * 
 */
public class DataSaveController extends BaseController {

    private final Path baseDir;

    /**
     * Creates a DataSaveController using the default data directory `src/data`.
     * <p>
     * The default directory will be created if it doesn't exist. If directory
     * creation fails, a warning will be printed but initialization continues.
     *
     * @param students the student repository instance
     * @param reps the company representative repository instance
     * @param staff the career center staff repository instance
     * @param internships the internship repository instance
     * @param applications the internship application repository instance
     * @param withdrawals the withdrawal request repository instance
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
     * Creates a DataSaveController with an explicit base directory for CSV files.
     * <p>
     * The specified directory will be created if it doesn't exist. If directory
     * creation fails, a warning will be printed but initialization continues.
     * If the provided baseDir is null, the default directory `src/data` will be used.
     *
     * @param students the student repository instance
     * @param reps the company representative repository instance
     * @param staff the career center staff repository instance
     * @param internships the internship repository instance
     * @param applications the internship application repository instance
     * @param withdrawals the withdrawal request repository instance
     * @param baseDir the base directory path for CSV file storage, or null to use default
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
     * Saves all repositories to their respective CSV files under the base directory.
     * <p>
     * Each repository is saved to a separate CSV file. The method attempts to save
     * all repositories even if some operations fail, and collects all error messages
     * into a single exception if any failures occur.
     *
     * @param studentsCsv the filename for student data (e.g., "students.csv")
     * @param repsCsv the filename for company representative data (e.g., "company_reps.csv")
     * @param staffCsv the filename for staff data (e.g., "staff.csv")
     * @param internshipsCsv the filename for internship data (e.g., "internships.csv")
     * @param applicationsCsv the filename for application data (e.g., "applications.csv")
     * @param withdrawalsCsv the filename for withdrawal request data (e.g., "withdrawals.csv")
     * @throws IOException if any save operation fails, containing consolidated error
     *         messages from all failed operations
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
     * Loads all repositories from their respective CSV files in the base directory.
     * <p>
     * Each repository is loaded from a separate CSV file. If a file doesn't exist,
     * a message is printed but loading continues for other files. The method attempts
     * to load all repositories even if some operations fail, and collects all error
     * messages into a single exception if any failures occur.
     * <p>
     * Some repositories require dependencies for loading (e.g., applications need
     * student and internship references), so repositories should be loaded in the
     * correct order.
     *
     * @param studentsCsv the filename for student data (e.g., "students.csv")
     * @param repsCsv the filename for company representative data (e.g., "company_reps.csv")
     * @param staffCsv the filename for staff data (e.g., "staff.csv")
     * @param internshipsCsv the filename for internship data (e.g., "internships.csv")
     * @param applicationsCsv the filename for application data (e.g., "applications.csv")
     * @param withdrawalsCsv the filename for withdrawal request data (e.g., "withdrawals.csv")
     * @throws IOException if any load operation fails, containing consolidated error
     *         messages from all failed operations
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
     * Returns the base directory path used for CSV file storage.
     *
     * @return the base directory path where CSV files are stored
     */
    public Path getBaseDir() {
        return baseDir;
    }

    /**
     * Checks if all repositories have been properly initialized.
     * <p>
     * This method verifies that all repository instances are non-null,
     * indicating that the controller is ready for data operations.
     *
     * @return true if all repositories are initialized, false otherwise
     */
    public boolean isInitialized() {
        return studentRepo != null && repRepo != null && staffRepo != null && 
               internshipRepo != null && applicationRepo != null && withdrawalRepo != null;
    }
}