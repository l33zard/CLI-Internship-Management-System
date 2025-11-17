// src/database/InMemoryCrudRepository.java
package database;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Generic in-memory implementation of the CrudRepository interface.
 * 
 * <p>This class provides a thread-safe, in-memory storage solution for entities
 * using a ConcurrentHashMap. It serves as a base implementation for repositories
 * that need simple CRUD operations without domain-specific query methods.
 * 
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li><strong>Thread-Safety:</strong> Uses ConcurrentHashMap for concurrent access</li>
 *   <li><strong>Generic Design:</strong> Works with any entity type and ID type</li>
 *   <li><strong>Flexible ID Extraction:</strong> Accepts a custom function to extract IDs from entities</li>
 *   <li><strong>Fast Access:</strong> O(1) lookups, inserts, and deletes</li>
 * </ul>
 * 
 * <p><strong>Design Considerations:</strong>
 * <ul>
 *   <li>Entities must have stable IDs that don't change after creation</li>
 *   <li>All data is stored in memory and lost when the application stops</li>
 *   <li>Suitable for development, testing, and small-scale applications</li>
 *   <li>For production systems with large datasets, consider persistent storage</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Create a repository for Student entities with String IDs
 * Function<Student, String> idExtractor = Student::getUserId;
 * CrudRepository<Student, String> studentRepo = 
 *     new InMemoryCrudRepository<>(idExtractor);
 * 
 * // Save a student
 * Student student = new Student("U1234567A", "John Doe", "CS", 3, "john@example.com");
 * studentRepo.save(student);
 * 
 * // Find by ID
 * Optional<Student> found = studentRepo.findById("U1234567A");
 * }</pre>
 * 
 * @param <T> the entity type managed by this repository
 * @param <ID> the type of the entity's unique identifier
 * 
 * @see CrudRepository
 */
public class InMemoryCrudRepository<T, ID> implements CrudRepository<T, ID> {
    
    /**
     * The internal concurrent map storing entities by their IDs.
     * 
     * <p>ConcurrentHashMap is used instead of HashMap to provide thread-safety
     * without requiring external synchronization. This allows multiple threads
     * to safely read and write to the repository concurrently.
     */
    protected final Map<ID, T> store = new ConcurrentHashMap<>();
    
    /**
     * Function to extract the ID from an entity.
     * 
     * <p>This allows the repository to work with any entity type without
     * requiring entities to implement a specific interface. The function
     * is typically a method reference like {@code Student::getUserId}.
     */
    private final Function<T, ID> idExtractor;

    /**
     * Constructs a new InMemoryCrudRepository with the specified ID extraction function.
     * 
     * <p>The ID extractor must return a non-null, stable ID for each entity.
     * 
     * @param idExtractor function to extract the ID from an entity (must not be null)
     * @throws NullPointerException if idExtractor is null
     */
    public InMemoryCrudRepository(Function<T, ID> idExtractor) {
        this.idExtractor = Objects.requireNonNull(idExtractor, "idExtractor");
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Performance:</strong> O(1) time complexity.
     */
    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Note:</strong> Returns an immutable copy of the values
     * to prevent external modification of the repository's internal state.
     * 
     * <p><strong>Performance:</strong> O(n) time complexity where n is the number of entities.
     */
    @Override
    public List<T> findAll() {
        return List.copyOf(store.values());
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Workflow:</strong>
     * <ol>
     *   <li>Extracts the ID from the entity using the idExtractor function</li>
     *   <li>Validates that the ID is not null</li>
     *   <li>Stores the entity in the map (replacing any existing entity with the same ID)</li>
     * </ol>
     * 
     * <p><strong>Performance:</strong> O(1) time complexity.
     * 
     * @throws NullPointerException if the extracted entity ID is null
     */
    @Override
    public T save(T entity) {
        ID id = Objects.requireNonNull(idExtractor.apply(entity), "entity id must not be null");
        store.put(id, entity);
        return entity;
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Performance:</strong> O(n) time complexity where n is the number of entities to save.
     */
    @Override
    public List<T> saveAll(Iterable<T> entities) {
        List<T> list = new ArrayList<>();
        for (T e : entities) list.add(save(e));
        return list;
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Performance:</strong> O(1) time complexity.
     * 
     * <p>If the ID doesn't exist, this method has no effect (no exception thrown).
     */
    @Override
    public void deleteById(ID id) {
        store.remove(id);
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Performance:</strong> O(n) time complexity where n is the number of IDs to delete.
     */
    @Override
    public void deleteAllById(Iterable<ID> ids) {
        for (ID id : ids) store.remove(id);
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Warning:</strong> This clears all data from the repository.
     * This operation cannot be undone.
     * 
     * <p><strong>Performance:</strong> O(1) time complexity.
     */
    @Override
    public void deleteAll() {
        store.clear();
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Performance:</strong> O(1) time complexity.
     */
    @Override
    public boolean existsById(ID id) {
        return store.containsKey(id);
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Performance:</strong> O(1) time complexity.
     */
    @Override
    public long count() {
        return store.size();
    }
}