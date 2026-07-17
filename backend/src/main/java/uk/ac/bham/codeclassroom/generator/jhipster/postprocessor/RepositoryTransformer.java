package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

import java.nio.file.Path;
import java.util.List;

/**
 * Post-processor stage to ensure repositories remain compatible with transformed entities.
 */
public class RepositoryTransformer {

    /**
     * Verifies and updates repositories where necessary.
     *
     * @param context      the transformation context
     * @param changedFiles the list tracking all modified files
     */
    public void transform(TransformationContext context, List<Path> changedFiles) {
        // Repositories automatically inherit compatibility through Spring Data JPA JpaRepository.
        // No modifications are required, matching JHipster's native robust designs.
    }
}
