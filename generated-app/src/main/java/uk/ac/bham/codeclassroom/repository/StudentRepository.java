package uk.ac.bham.codeclassroom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.ac.bham.codeclassroom.model.Student;

/**
 * Spring Data JPA Repository for the {@link Student} entity.
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
}
