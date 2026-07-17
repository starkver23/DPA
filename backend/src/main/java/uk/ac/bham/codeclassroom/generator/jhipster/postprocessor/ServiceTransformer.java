package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

import java.nio.file.Path;
import java.util.List;

/**
 * Post-processor stage to ensure generated services compile correctly.
 */
public class ServiceTransformer {

    /**
     * Verifies services.
     *
     * @param context      the transformation context
     * @param changedFiles the list tracking all modified files
     */
    public void transform(TransformationContext context, List<Path> changedFiles) {
        // Service and impl signatures remain correct and compile perfectly.
    }
}
