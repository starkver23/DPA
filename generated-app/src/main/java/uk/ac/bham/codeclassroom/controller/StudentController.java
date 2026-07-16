package uk.ac.bham.codeclassroom.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.bham.codeclassroom.dto.StudentDTO;
import uk.ac.bham.codeclassroom.service.StudentService;

import java.util.List;

/**
 * REST controller for managing {@link uk.ac.bham.codeclassroom.model.Student}.
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService service;

    /**
     * Constructor injection.
     */
    public StudentController(StudentService service) {
        this.service = service;
    }

    /**
     * POST /api/students : Create a new entity.
     */
    @PostMapping
    public ResponseEntity<StudentDTO> create(@RequestBody StudentDTO dto) {
        return ResponseEntity.ok(service.save(dto));
    }

    /**
     * GET /api/students : Get all entities.
     */
    @GetMapping
    public ResponseEntity<List<StudentDTO>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    /**
     * GET /api/students/:id : Get entity by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<StudentDTO> get(@PathVariable Long id) {
        return service.findOne(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/students/:id : Delete entity by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
