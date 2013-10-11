package hudson.plugins.analysis.util.model;

/**
 * A container for culprit annotations.
 *
 * @author jgibson
 */
public class CulpritAnnotationContainer extends AnnotationContainer {
    private static final long serialVersionUID = 5504146567211894175L;

    private final String fullName;
    private final String email;

    /**
     * Creates a new instance of {@link CulpritAnnotationContainer}.
     *
     * @param culpritName the full name of the culprit for this container.
     * @param culpritEmail the email of the culprit for this container.
     * @param hierarchy the scope of this culprit container.  Should be one of
     *  the {@code USER_} values.
     */
    public CulpritAnnotationContainer(final String culpritName, final String culpritEmail, final Hierarchy hierarchy) {
        super(culpritName + culpritEmail, hierarchy);
        this.fullName = culpritName;
        this.email = culpritEmail;
    }

    /**
     * Get the full name of the culprit.
     *
     * @return the full name of the culprit.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Get the email of the cuprit.
     *
     * @return the email of culprit.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Get a readable name for this container.
     *
     * @return a readable name for this container.
     */
    public String getDisplayName() {
        return "".equals(getFullName()) ? "Unknown users" : getFullName();
    }

    /**
     * Rebuilds the priorities mapping.
     *
     * @return the created object
     */
    private Object readResolve() {
        // NOTSURE if this is necessary because the superclass calls this
        rebuildMappings();
        return this;
    }
}
