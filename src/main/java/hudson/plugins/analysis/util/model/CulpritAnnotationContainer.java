package hudson.plugins.analysis.util.model;

import java.util.Collection;

/**
 * A container for culprit annotations.
 *
 * @author jgibson
 */
public class CulpritAnnotationContainer extends AnnotationContainer {
    private static final long serialVersionUID = 5504146567211894175L;

    /**
     * Creates a new instance of {@link CulpritAnnotationContainer}.
     *
     * @param name
     *            the name of this container
     * @param annotations
     *            the annotations to be stored
     */
    public CulpritAnnotationContainer(final String culpritName) {
        this(culpritName, null);
    }

    /**
     * Creates a new instance of {@link CulpritAnnotationContainer}.
     *
     * @param name
     *            the name of this container
     * @param annotations
     *            the annotations to be stored
     */
    public CulpritAnnotationContainer(final String culpritName, final Collection<FileAnnotation> annotations) {
        super(culpritName, Hierarchy.USER);
        if(annotations != null) {
            addAnnotations(annotations);
        }
    }

    /**
     * Rebuilds the priorities mapping.
     *
     * @return the created object
     */
    private Object readResolve() {
        // NOTSURE if this is necessary because the superclass calls this
        setHierarchy(Hierarchy.USER);
        rebuildMappings();
        return this;
    }
}
