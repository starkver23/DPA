package uk.ac.bham.codeclassroom.service;

import uk.ac.bham.codeclassroom.dto.StudentDTO;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link uk.ac.bham.codeclassroom.model.Student}.
 */
public interface StudentService {

    /**
     * Save a Student.
     *
     * @param dto the DTO to save
     * @return the persisted DTO
     */
    StudentDTO save(StudentDTO dto);

    /**
     * Get all the entities.
     *
     * @return the list of DTOs
     */
    List<StudentDTO> findAll();

    /**
     * Get the "id" entity.
     *
     * @param id the id of the entity
     * @return the Optional DTO
     */
    Optional<StudentDTO> findOne(Long id);

    /**
     * Delete the "id" entity.
     *
     * @param id the id of the entity
     */
    void delete(Long id);
}
