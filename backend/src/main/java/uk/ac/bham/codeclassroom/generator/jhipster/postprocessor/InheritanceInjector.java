package uk.ac.bham.codeclassroom.generator.jhipster.postprocessor;

import java.nio.file.Path;
import java.util.List;

/**
 * Coordinates the injection of class extends and JPA annotations into JHipster source files.
 */
public class InheritanceInjector {

    private final EntityTransformer entityTransformer;
    private final DTOTransformer dtoTransformer;
    private final MapperTransformer mapperTransformer;

    /**
     * Default constructor.
     */
    public InheritanceInjector() {
        this.entityTransformer = new EntityTransformer();
        this.dtoTransformer = new DTOTransformer();
        this.mapperTransformer = new MapperTransformer();
    }

    /**
     * Inject inheritance configurations into all JHipster-generated source files.
     *
     * @param context      the transformation context
     * @param changedFiles list to accumulate all transformed paths
     */
    public void inject(TransformationContext context, List<Path> changedFiles) {
        entityTransformer.transform(context, changedFiles);
        dtoTransformer.transform(context, changedFiles);
        mapperTransformer.transform(context, changedFiles);
    }
}
