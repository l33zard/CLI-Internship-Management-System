// src/database/CrudRepository.java
package database;

import java.util.*;

/**
 * Generic repository interface for CRUD (Create, Read, Update, Delete) operations.
 * 
 * <p>This interface defines the standard contract for all repository implementations
 * in the internship management system. It provides a consistent API for data access
 * operations across different entity types.
 * 
 * <p><strong>Design Pattern:</strong>
 * This follows the Repository pattern, which abstracts data access logic and provides
 * a collection-like interface for domain objects. This enables:
 * <ul>
 *   <li>Decoupling business logic from data access details</li>
 *   <li>Easy testing through mock implementations</li>
 *   <li>Flexibility to change underlying storage mechanisms</li>
 *   <li>Consistent API across all entities</li>
 * </ul>
 * 
 * <p><strong>Type Parameters:</strong>
 * <ul>
 *   <li><strong>T:</strong> The entity type (e.g., Student, Internship, Application)</li>
 *   <li><strong>ID:</strong> The type of the entity's identifier (typically String)</li>
 * </ul>
 * 
 * <p><strong>Implementation Notes:</strong>
 * <ul>
 *   <li>All implementations in this system use in-memory storage (Maps)</li>
 *   <li>IDs must be stable and unique for each entity</li>
 *   <li>save() performs upsert (insert or update)</li>
 *   <li>Methods return Optional for single results to handle missing entities gracefully</li>
 * </ul>
 * 
 * @param <T> the entity type managed by this repository
 * @param <ID> the type of the entity's unique identifier
 * 
 * @see InMemoryCrudRepository
 * @see StudentRepository
 * @see InternshipRepository
 * @see ApplicationRepository
 */
public interface CrudRepository<T, ID> {
    
    /**
     * Retrieves an entity by its unique identifier.
     * 
     * <p>Returns an Optional containing the entity if found, or an empty
     * Optional if no entity with the given ID exists.
     * 
     * @param id the unique identifier of the entity to retrieve
     * @return an Optional containing the entity if found, empty otherwise
     */
    Optional<T> findById(ID id);
    
    /**
     * Retrieves all entities in the repository.
     * 
     * <p>Returns an immutable or defensive copy of all entities to prevent
     * external modification of the repository's internal state.
     * 
     * @return a list containing all entities (never null, may be empty)
     */
    List<T> findAll();

    /**
     * Saves an entity to the repository (insert or update).
     * 
     * <p><strong>Upsert Behavior:</strong>
     * <ul>
     *   <li>If an entity with the same ID exists, it is replaced (update)</li>
     *   <li>If no entity with the ID exists, it is added (insert)</li>
     * </ul>
     * 
     * <p>The entity's ID must not be null when saved.
     * 
     * @param entity the entity to save (must not be null, ID must not be null)
     * @return the saved entity
     * @throws NullPointerException if entity or entity ID is null
     */
    T save(T entity);
    
    /**
     * Saves multiple entities to the repository.
     * 
     * <p>Convenience method for bulk operations. Each entity is saved
     * using the same upsert logic as {@link #save(Object)}.
     * 
     * @param entities the entities to save (must not be null)
     * @return a list of all saved entities in the order they were processed
     */
    List<T> saveAll(Iterable<T> entities);

    /**
     * Deletes an entity by its unique identifier.
     * 
     * <p>If no entity with the given ID exists, this method has no effect
     * (does not throw an exception).
     * 
     * @param id the unique identifier of the entity to delete
     */
    void deleteById(ID id);
    
    /**
     * Deletes multiple entities by their identifiers.
     * 
     * <p>Convenience method for bulk deletion. For each ID, if an entity
     * exists, it is removed; otherwise, that ID is silently ignored.
     * 
     * @param ids the collection of identifiers for entities to delete
     */
    void deleteAllById(Iterable<ID> ids);
    
    /**
     * Deletes all entities from the repository.
     * 
     * <p><strong>Warning:</strong> This operation removes all data and cannot
     * be undone. Use with caution, typically only for testing or reset operations.
     */
    void deleteAll();

    /**
     * Checks whether an entity with the given identifier exists.
     * 
     * <p>Useful for validation before attempting to retrieve or modify an entity.
     * 
     * @param id the unique identifier to check
     * @return true if an entity with this ID exists, false otherwise
     */
    boolean existsById(ID id);
    
    /**
     * Returns the total number of entities in the repository.
     * 
     * <p>Useful for statistics, pagination calculations, or validation.
     * 
     * @return the count of all entities (never negative)
     */
    long count();
}