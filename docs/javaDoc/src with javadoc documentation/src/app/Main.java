package app;

import boundary.*;
import controller.*;
import database.*;

public class Main {
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