package app;

import boundary.*;
import controller.*;
import database.*;

/**
 * Main application entry point for the Internship Management System.
 * <p>
 * This is the launcher used when running the console application; it wires
 * repositories, controllers and boundary (UI) components, loads CSV data,
 * starts the main menu and ensures data is saved on exit.
 * <p>
 * The application follows a layered architecture:
 * <ul>
 *   <li><strong>Database Layer:</strong> Repository classes handling CSV data persistence</li>
 *   <li><strong>Controller Layer:</strong> Business logic controllers enforcing rules and workflows</li>
 *   <li><strong>Boundary Layer:</strong> User interface components for different user roles</li>
 * </ul>
 * 
 * <strong>Key Features:</strong>
 * <ul>
 *   <li>Automatic data loading from CSV files on startup</li>
 *   <li>Role-based authentication and authorization</li>
 *   <li>Comprehensive internship management for companies</li>
 *   <li>Student application and placement system</li>
 *   <li>Career Center staff oversight and approval workflows</li>
 *   <li>Automatic data saving on application exit</li>
 * </ul>
 * 
 * <strong>Data Flow:</strong>
 * <ol>
 *   <li>Loads all data from CSV files in `src/data/` directory</li>
 *   <li>Initializes controllers with repository dependencies</li>
 *   <li>Starts authentication and main menu system</li>
 *   <li>Persists all changes to CSV files on application termination</li>
 * </ol>
 * 
 */
public class Main {
    
    /**
     * Application entry point for the Internship Management System.
     * <p>
     * This method performs the following operations:
     * <ol>
     *   <li>Initializes all data repositories</li>
     *   <li>Loads initial data from CSV files in the `src/data/` directory</li>
     *   <li>Creates and wires all business controllers with their dependencies</li>
     *   <li>Initializes user interface boundaries for all user roles</li>
     *   <li>Sets up data persistence controller for automatic saving</li>
     *   <li>Starts the main application menu system</li>
     *   <li>Ensures all data is saved to CSV files on application exit</li>
     * </ol>
     * 
     * <strong>Error Handling:</strong>
     * <ul>
     *   <li>Application errors during startup are caught and displayed</li>
     *   <li>Data saving errors during shutdown are caught and reported</li>
     *   <li>The application attempts to save data even if startup fails partially</li>
     * </ul>
     * 
     * <strong>Data Files:</strong>
     * The application expects the following CSV files in the `src/data/` directory:
     * <ul>
     *   <li>{@code students.csv} - Student accounts and profiles</li>
     *   <li>{@code companyreps.csv} - Company representative registrations</li>
     *   <li>{@code staffs.csv} - Career Center staff accounts</li>
     *   <li>{@code internships.csv} - Internship postings and details</li>
     *   <li>{@code applications.csv} - Student internship applications</li>
     *   <li>{@code withdrawals.csv} - Application withdrawal requests</li>
     * </ul>
     * 
     * If any data file is missing, the application will start with empty data
     * for that entity type and create the file on exit.
     *
     * @param args command-line arguments (currently ignored)
     * 
     * @throws SecurityException if the application lacks file system permissions
     *         for reading/writing data files
     *         
     * @see <a href="../../data/students.csv">Students Data Format</a>
     * @see <a href="../../data/companyreps.csv">Company Representatives Data Format</a>
     * @see <a href="../../data/staffs.csv">Staff Data Format</a>
     * @see <a href="../../data/internships.csv">Internships Data Format</a>
     * @see <a href="../../data/applications.csv">Applications Data Format</a>
     * @see <a href="../../data/withdrawals.csv">Withdrawals Data Format</a>
     */
    public static void main(String[] args) {
        // Use consistent relative paths
        String dataDir = "src/data/";
        
        // Initialize repositories
        var students = new StudentRepository();
        var reps = new CompanyRepRepository();
        var staff = new CareerCenterStaffRepository();
        var internships = new InternshipRepository();
        var applications = new ApplicationRepository();
        var withdrawals = new WithdrawalRequestRepository();

        // DataSaveController must be declared here to be accessible in finally
        DataSaveController datasave = null;
        
        try {            
            // --- Load initial CSVs with consistent file names ---
            students.loadFromCsv(dataDir + "students.csv");
            reps.loadFromCsv(dataDir + "companyreps.csv");
            staff.loadFromCsv(dataDir + "staffs.csv");
            internships.loadFromCsv(dataDir + "internships.csv");
            applications.loadFromCsv(dataDir + "applications.csv", students, internships);
            withdrawals.loadFromCsv(dataDir + "withdrawals.csv", applications, students, staff);

            // --- Authentication controller ---
            var auth = new AuthController(students, reps, staff);

            // --- Business controllers ---
            var studentCtl = new StudentController(students, internships, applications, withdrawals);
            var repCtl = new CompanyRepController(reps, internships, applications);
            var staffCtl = new CareerCenterStaffController(reps, internships, applications, withdrawals, staff);
            var regCtl = new CompanyRepRegistrationController(reps);

            // --- Boundaries ---
            var login = new LoginBoundary(students, reps, staff, auth);
            var stuUI = new StudentBoundary(studentCtl, auth);
            var repUI = new CompanyRepBoundary(repCtl, auth);
            var stfUI = new CareerCenterStaffBoundary(staffCtl, auth);
            var regUI = new CompanyRepRegistrationBoundary(regCtl);

            // --- Data save controller ---
            datasave = new DataSaveController(students, reps, staff, internships, applications, withdrawals, java.nio.file.Paths.get(dataDir));

            // --- Main application loop ---
            System.out.println();
            new MainMenuBoundary(login, stuUI, repUI, stfUI, regUI).run();
            
        } catch (Exception e) {
            System.err.println("Application error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // --- Save all data on exit ---
            if (datasave != null) {
                try {
                    datasave.saveAll(
                        "students.csv",           // Consistent with load
                        "companyreps.csv",        // Consistent with load  
                        "staffs.csv",             // Consistent with load
                        "internships.csv",        // Consistent with load
                        "applications.csv",       // Consistent with load
                        "withdrawals.csv"         // Consistent with load
                    );
                } catch (Exception e) {
                    System.err.println("Error saving data: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("DataSaveController not initialized, unable to save");
            }
        }
    }
}