package entity;

/**
 * Base user entity shared by students, company representatives, and career center staff.
 * 
 * <p>This abstract class provides fundamental user identity and authentication capabilities
 * that are common across all user types in the system. Concrete user types extend this class
 * to add domain-specific fields, behaviors, and business rules.
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li>Manage user identity and display name</li>
 *   <li>Handle password storage and validation</li>
 *   <li>Provide secure password change functionality</li>
 *   <li>Serve as base class for domain-specific user types</li>
 * </ul>
 *
 */
public abstract class User {
    protected String userId;
    protected String name;
    protected String password;

    /**
     * Constructs a user with the specified identifier and display name.
     *
     * @param userId the unique external identifier (non-null)
     * @param name the display name for the user
     * @throws IllegalArgumentException if userId or name is null or empty
     */
    public User(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    /**
     * Returns the unique user identifier.
     *
     * @return the user ID
     */
    public String getUserId() { return userId; }
    
    /**
     * Returns the user's display name.
     *
     * @return the display name
     */
    public String getName() { return name; }
    
    /**
     * Returns the stored password hash or value.
     * Note: In a production system, this would typically return a password hash rather than plain text.
     *
     * @return the stored password (may be null if not set)
     */
    public String getPassword() { return password; }
    
    /**
     * Sets a new password for this user.
     * The password is trimmed and validated to ensure it's not empty.
     *
     * @param password the new password to set
     * @throws IllegalArgumentException if the password is null or empty after trimming
     */
    public void setPassword(String password) { 
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        this.password = password.trim(); 
    }
    
    /**
     * Verifies whether a password attempt matches the stored password.
     * Performs a case-sensitive exact match comparison.
     *
     * @param attempt the password attempt to verify
     * @return true if the attempt matches the stored password, false otherwise
     */
    public boolean checkPassword(String attempt) {
        return password != null && password.equals(attempt);
    }
    
    /**
     * Changes the user's password after verifying the current password.
     * Provides secure password change functionality with validation:
     * <ul>
     *   <li>Verifies current password matches</li>
     *   <li>Ensures new password is not empty</li>
     *   <li>Prevents setting the same password as the current one</li>
     * </ul>
     *
     * @param currentPassword the user's current password for verification
     * @param newPassword the desired new password
     * @throws IllegalArgumentException if current password is incorrect, new password is empty,
     *                                  or new password is the same as current password
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

//    /**
//     * Returns a string representation of the user.
//     * Format: "Display Name (UserID)"
//     *
//     * @return formatted string representation
//     */
//    @Override 
//    public String toString() { 
//        return String.format("%s (%s)", name, userId); 
//    }
}