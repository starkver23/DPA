package uk.ac.bham.codeclassroom.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.bham.codeclassroom.dto.StudentDTO;
import uk.ac.bham.codeclassroom.mapper.StudentMapper;
import uk.ac.bham.codeclassroom.repository.StudentRepository;
import uk.ac.bham.codeclassroom.service.StudentService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link uk.ac.bham.codeclassroom.model.Student}.
 */
@Service
@Transactional
public class StudentServiceImpl implements StudentService {

    private final StudentRepository repository;
    private final StudentMapper mapper;

    /**
     * Constructor injection.
     */
    public StudentServiceImpl(StudentRepository repository, StudentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public StudentDTO save(StudentDTO dto) {
        // Stub implementation
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentDTO> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StudentDTO> findOne(Long id) {
        return repository.findById(id)
                .map(mapper::toDto);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
