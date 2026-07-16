package uk.ac.bham.codeclassroom.dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.Collection;

/**
 * A DTO for the {@link uk.ac.bham.codeclassroom.model.Student} entity.
 */
public class StudentDTO {

    private Long id;

    private String name;

    public StudentDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
