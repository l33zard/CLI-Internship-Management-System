package entity;

/**
 * Enumeration of internship difficulty levels determining student eligibility.
 * 
 * <p>Internship levels are used to match opportunities with student year of study,
 * ensuring students only apply for positions appropriate to their experience level.</p>
 * 
 * <h2>Eligibility Rules</h2>
 * <ul>
 *   <li><b>Year 1-2 Students:</b> BASIC level only</li>
 *   <li><b>Year 3-4 Students:</b> All levels (BASIC, INTERMEDIATE, ADVANCED)</li>
 * </ul>
 * 
 * <h2>Level Descriptions</h2>
 * <ul>
 *   <li><b>{@link #BASIC}:</b> Entry-level positions suitable for all students</li>
 *   <li><b>{@link #INTERMEDIATE}:</b> Positions requiring some background knowledge</li>
 *   <li><b>{@link #ADVANCED}:</b> Challenging positions for experienced students</li>
 * </ul>
 * 
 * @author SC2002 Assignment Team
 * @version 1.0
 * @since 1.0
 * @see Student#isEligibleFor(InternshipLevel)
 * @see Internship#getLevel()
 */
public enum InternshipLevel {
	/**
	 * Basic level internships suitable for all students including Year 1-2.
	 * 
	 * <p>These are typically entry-level positions that:</p>
	 * <ul>
	 *   <li>Require minimal prior experience</li>
	 *   <li>Provide foundational learning opportunities</li>
	 *   <li>Focus on basic skills development</li>
	 *   <li>Are accessible to students early in their studies</li>
	 * </ul>
	 */
	BASIC,
	
	/**
	 * Intermediate level internships for Year 3-4 students.
	 * 
	 * <p>These positions typically:</p>
	 * <ul>
	 *   <li>Require some background knowledge in the field</li>
	 *   <li>Expect familiarity with relevant tools/technologies</li>
	 *   <li>Involve more independent work</li>
	 *   <li>Build on foundational coursework</li>
	 * </ul>
	 */
	INTERMEDIATE,
	
	/**
	 * Advanced level internships for experienced Year 3-4 students.
	 * 
	 * <p>These are challenging positions that:</p>
	 * <ul>
	 *   <li>Require significant background knowledge</li>
	 *   <li>May expect prior internship or project experience</li>
	 *   <li>Involve complex problem-solving</li>
	 *   <li>Offer opportunities for specialized skill development</li>
	 * </ul>
	 */
	ADVANCED;
}
