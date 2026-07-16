package uk.ac.bham.codeclassroom.mapper;

import org.springframework.stereotype.Component;
import uk.ac.bham.codeclassroom.dto.StudentDTO;
import uk.ac.bham.codeclassroom.model.Student;

/**
 * Mapper for the entity {@link Student} and its DTO {@link StudentDTO}.
 */
@Component
public class StudentMapper {

    /**
     * Maps an entity to its DTO representation.
     */
    public StudentDTO toDto(Student entity) {
        if (entity == null) {
            return null;
        }
        // TODO: Implement field mappings
        StudentDTO dto = new StudentDTO();
        dto.setId(entity.getId());
        return dto;
    }

    /**
     * Maps a DTO to its entity representation.
     */
    public Student toEntity(StudentDTO dto) {
        if (dto == null) {
            return null;
        }
        // TODO: Implement field mappings
        Student entity = new Student();
        entity.setId(dto.getId());
        return entity;
    }
}
