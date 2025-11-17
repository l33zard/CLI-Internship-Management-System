package entity;

/**
 * Represents the experience level or complexity of an internship position.
 * 
 * <p>This enum defines the progression of internship levels that students can apply for,
 * typically corresponding to their academic year or prior experience level. The levels
 * help match students with appropriate internship opportunities based on their skills
 * and educational background.
 *
 * <p><b>Level Definitions:</b>
 * <ul>
 *   <li><b>BASIC</b> - Entry-level internships suitable for students with minimal experience,
 *       typically first or second year students</li>
 *   <li><b>INTERMEDIATE</b> - Mid-level internships for students with some relevant coursework
 *       or prior basic internship experience</li>
 *   <li><b>ADVANCED</b> - Advanced internships requiring significant relevant experience
 *       or specialized skills, typically for final year students</li>
 * </ul>
 *
 */
public enum InternshipLevel {
    
    /**
     * Entry-level internship position suitable for beginners.
     * Typically requires minimal prior experience and focuses on learning fundamental skills.
     * Appropriate for first or second year undergraduate students.
     */
    BASIC,
    
    /**
     * Intermediate internship position requiring some relevant background.
     * Suitable for students who have completed core coursework or have basic internship experience.
     * Appropriate for second or third year undergraduate students.
     */
    INTERMEDIATE,
    
    /**
     * Advanced internship position requiring significant experience or specialized skills.
     * Typically involves complex projects and greater responsibility.
     * Appropriate for final year undergraduate or graduate students.
     */
    ADVANCED;
}