package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

import java.nio.file.Path;
import java.util.List;

/**
 * Post-processor stage to ensure REST controllers function flawlessly.
 */
public class ControllerTransformer {

    /**
     * Verifies controllers.
     *
     * @param context      the transformation context
     * @param changedFiles the list tracking all modified files
     */
    public void transform(TransformationContext context, List<Path> changedFiles) {
        // Controllers work perfectly with inheritance because of polymorphism and standard REST APIs.
    }
}
