package hudson.plugins.analysis.views;

import hudson.model.AbstractBuild;
import hudson.plugins.analysis.util.model.CulpritAnnotationContainer;

/**
 * Details for a particular person.
 *
 * @author jgibson
 */
public class CulpritDetail extends AbstractAnnotationsDetail {
    /** Unique identifier of this class. */
    private static final long serialVersionUID = -5907296989102083012L;

    /** The package to show the details for. */
    private final String culpritName;

    /**
     * Creates a new instance of <code>ModuleDetail</code>.
     *
     * @param owner
     *            current build as owner of this action.
     * @param detailFactory
     *            factory to create detail objects with
     * @param file
     *            the file to show the details for
     * @param defaultEncoding
     *            the default encoding to be used when reading and parsing files
     * @param header
     *            header to be shown on detail page
     */
    public CulpritDetail(final AbstractBuild<?, ?> owner, final DetailFactory detailFactory, final CulpritAnnotationContainer culpritContainer, final String defaultEncoding, final String header) {
        super(owner, detailFactory, culpritContainer.getAnnotations(), defaultEncoding, header, Hierarchy.USER);
        this.culpritName = culpritContainer.getName();
    }

    /**
     * Returns the header for the detail screen.
     *
     * @return the header
     */
    @Override
    public String getHeader() {
        return getName() + " - " + culpritName;
    }

    /** {@inheritDoc} */
    public String getDisplayName() {
        return culpritName;
    }
}
