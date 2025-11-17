// src/database/CrudRepository.java
package database;

import java.util.*;

/**
 * Minimal CRUD (Create, Read, Update, Delete) repository contract used by in-memory repositories
 * throughout the project.
 *
 * <p>This interface defines the basic persistence operations that all entity repositories
 * should implement. It follows the repository pattern to abstract data access and provide
 * a consistent API across different entity types.
 *
 * <p><b>Implementation Notes:</b>
 * <ul>
 *   <li>The {@code save} operation performs an upsert (insert or update)</li>
 *   <li>{@code findAll()} should return entities in deterministic order when supported by the implementation</li>
 *   <li>Implementations may throw {@code NullPointerException} for null parameters unless otherwise specified</li>
 *   <li>This interface does not specify transaction behavior; implementations define their own guarantees</li>
 * </ul>
 * 
 *
 * @param <T> the type of entity managed by this repository
 * @param <ID> the type of the entity's identifier
 * 
 */
public interface CrudRepository<T, ID> {
    
    /**
     * Finds an entity by its unique identifier.
     *
     * @param id the identifier of the entity to find; must not be null
     * @return an {@code Optional} containing the found entity, or empty if no entity exists with the given id
     * @throws NullPointerException if the id is null
     */
    Optional<T> findById(ID id);
    
    /**
     * Returns all entities managed by the repository.
     *
     * <p>Implementations should return entities in a deterministic order when possible
     * (e.g., insertion order, natural ordering, or sorted by identifier).
     *
     * @return a list of all entities; never null but may be empty
     */
    List<T> findAll();

    /**
     * Saves (inserts or updates) an entity in the repository.
     *
     * <p>This operation performs an upsert:
     * <ul>
     *   <li>If the entity does not exist, it is inserted</li>
     *   <li>If the entity already exists, it is updated</li>
     * </ul>
     * The returned instance may be different from the input parameter (e.g., with generated identifiers
     * or updated timestamps).
     *
     * @param entity the entity to save; must not be null
     * @return the saved entity; may be different from the input parameter
     * @throws NullPointerException if the entity is null
     */
    T save(T entity);                 // upsert; return the stored entity
    
    /**
     * Saves all entities in the provided iterable.
     *
     * <p>This is a batch operation that may be more efficient than multiple individual
     * {@code save} calls for some implementations.
     *
     * @param entities the entities to save; must not be null and must not contain null elements
     * @return a list of the saved entities in the same iteration order
     * @throws NullPointerException if the iterable is null or contains null elements
     */
    List<T> saveAll(Iterable<T> entities);

    /**
     * Deletes the entity with the given identifier.
     *
     * <p>If no entity exists with the given id, the method returns silently without error.
     *
     * @param id the identifier of the entity to delete; must not be null
     * @throws NullPointerException if the id is null
     */
    void deleteById(ID id);
    
    /**
     * Deletes all entities with the given identifiers.
     *
     * <p>If any identifier does not correspond to an existing entity, it is silently ignored.
     * The operation continues processing remaining identifiers.
     *
     * @param ids the identifiers of entities to delete; must not be null and must not contain null elements
     * @throws NullPointerException if the iterable is null or contains null elements
     */
    void deleteAllById(Iterable<ID> ids);
    
    /**
     * Deletes all entities managed by the repository.
     *
     * <p>After this operation, the repository will be empty.
     */
    void deleteAll();

    /**
     * Checks whether an entity with the given identifier exists.
     *
     * @param id the identifier to check; must not be null
     * @return true if an entity with the given id exists, false otherwise
     * @throws NullPointerException if the id is null
     */
    boolean existsById(ID id);
    
    /**
     * Returns the number of entities in the repository.
     *
     * @return the total count of entities
     */
    long count();
}