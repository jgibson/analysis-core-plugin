package hudson.plugins.analysis.util.model;

/**
 * A container for culprit annotations.
 *
 * @author jgibson
 */
public class CulpritAnnotationContainer extends AnnotationContainer {
    private static final long serialVersionUID = 5504146567211894175L;

    private String fullName;
    private String email;

    /**
     * Creates a new instance of {@link CulpritAnnotationContainer}.
     *
     * @param name
     *            the name of this container
     * @param annotations
     *            the annotations to be stored
     */
    public CulpritAnnotationContainer(final String culpritName, final String culpritEmail) {
        super(culpritName + culpritEmail, Hierarchy.USER);
        this.fullName = culpritName;
        this.email = culpritEmail;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
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
