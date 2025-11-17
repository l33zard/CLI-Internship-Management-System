package entity;

/**
 * Abstract base class for all user types in the Internship Management System.
 * 
 * <p>User serves as the parent class for the three distinct user roles:</p>
 * <ul>
 *   <li>{@link Student} - Students searching for and applying to internships</li>
 *   <li>{@link CompanyRep} - Company representatives posting internship opportunities</li>
 *   <li>{@link CareerCenterStaff} - Staff managing approvals and system oversight</li>
 * </ul>
 * 
 * <h2>Common Properties</h2>
 * <p>All users share these fundamental attributes:</p>
 * <ul>
 *   <li><b>userId:</b> Unique identifier for the user</li>
 *   <li><b>name:</b> Full name of the user</li>
 *   <li><b>password:</b> Authentication credential</li>
 * </ul>
 * 
 * <h2>User ID Formats</h2>
 * <ul>
 *   <li><b>Students:</b> Format UXXXXXXXY (e.g., U2345123F)</li>
 *   <li><b>Company Reps:</b> Email address (used as unique identifier)</li>
 *   <li><b>Staff:</b> NTU staff account username</li>
 * </ul>
 * 
 * <h2>Password Management</h2>
 * <p>This class provides basic password functionality:</p>
 * <ul>
 *   <li>Password storage (plain text in current implementation)</li>
 *   <li>Password validation</li>
 *   <li>Password change with verification</li>
 * </ul>
 * 
 * <p><b>Security Note:</b> This implementation uses plain text passwords
 * suitable for educational purposes. Production systems should use:</p>
 * <ul>
 *   <li>Password hashing (e.g., BCrypt, Argon2)</li>
 *   <li>Salt generation for each password</li>
 *   <li>Secure password policies (length, complexity)</li>
 * </ul>
 * 
 * @author SC2002 Assignment Team
 * @version 1.0
 * @since 1.0
 * @see Student
 * @see CompanyRep
 * @see CareerCenterStaff
 */
public abstract class User {
    /**
     * Unique identifier for this user.
     * 
     * <p>Format varies by user type:</p>
     * <ul>
     *   <li>Students: UXXXXXXXY format</li>
     *   <li>Company Reps: Email address</li>
     *   <li>Staff: NTU username</li>
     * </ul>
     */
    protected String userId;
    
    /**
     * Full name of the user.
     */
    protected String name;
    
    /**
     * User's password for authentication.
     * 
     * <p><b>Note:</b> Stored as plain text in current implementation.
     * Production systems should use hashed passwords.</p>
     */
    protected String password;

    /**
     * Constructs a new User with the specified ID and name.
     * 
     * <p>This constructor is called by subclasses to initialize common
     * user properties. Password is initially null and should be set
     * through {@link #setPassword(String)}.</p>
     * 
     * @param userId unique identifier for the user
     * @param name full name of the user
     */
    public User(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    /**
     * Returns the user's unique identifier.
     * 
     * @return the user ID
     */
    public String getUserId() { return userId; }
    
    /**
     * Returns the user's full name.
     * 
     * @return the name
     */
    public String getName() { return name; }
    
    /**
     * Returns the user's password.
     * 
     * <p><b>Security Warning:</b> This method returns the password in plain text.
     * Use cautiously and never log or display passwords.</p>
     * 
     * @return the password, or null if not set
     */
    public String getPassword() { return password; }
    
    /**
     * Sets the user's password.
     * 
     * <p>Validates that the password is not null or empty before setting.
     * Whitespace is trimmed from the password.</p>
     * 
     * @param password the new password to set
     * @throws IllegalArgumentException if password is null or empty after trimming
     */
    public void setPassword(String password) { 
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        this.password = password.trim(); 
    }
    
    /**
     * Checks if the provided password matches the user's current password.
     * 
     * <p>Performs exact string comparison. Returns false if current password
     * is null (user has no password set).</p>
     * 
     * @param attempt the password to verify
     * @return {@code true} if passwords match, {@code false} otherwise
     */
    public boolean checkPassword(String attempt) {
        return password != null && password.equals(attempt);
    }
    
    /**
     * Changes the user's password after verifying the current password.
     * 
     * <p>This method enforces password change security by:</p>
     * <ul>
     *   <li>Verifying the current password before allowing change</li>
     *   <li>Ensuring new password is not null or empty</li>
     *   <li>Preventing use of same password (must be different)</li>
     * </ul>
     * 
     * <h3>Business Rules</h3>
     * <ul>
     *   <li>Current password must be correct</li>
     *   <li>New password cannot be empty</li>
     *   <li>New password must differ from current password</li>
     * </ul>
     * 
     * @param currentPassword the user's current password for verification
     * @param newPassword the new password to set
     * @throws IllegalArgumentException if current password is incorrect,
     *         new password is null/empty, or new password is same as current
     */
	public void changePassword(String currentPassword, String newPassword) {
		if (!this.password.equals(currentPassword)) {
		    throw new IllegalArgumentException("Current password is incorrect.");
		}
		if (newPassword == null || newPassword.isEmpty()) {
		    throw new IllegalArgumentException("New password cannot be empty.");
		}
		if (newPassword.equals(this.password)) {
		    throw new IllegalArgumentException("New password cannot be the same as the old one.");
		}
		this.password = newPassword;
	}
}