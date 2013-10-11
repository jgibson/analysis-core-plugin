package hudson.plugins.analysis.views;

import hudson.model.AbstractBuild;
import hudson.model.User;
import hudson.plugins.analysis.util.GitFileAnnotationBlamer;
import hudson.plugins.analysis.util.model.CulpritAnnotationContainer;
import hudson.scm.NullSCM;
import hudson.scm.SCM;

/**
 * Details for a particular person.
 *
 * @author jgibson
 */
public class CulpritDetail extends AbstractAnnotationsDetail {
    /** Unique identifier of this class. */
    private static final long serialVersionUID = -5907296989102083012L;

    /** The culprit to show the details for. */
    private final String culpritName;
    private final String culpritEmail;

    private transient boolean userAttempted;
    private transient User user;

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
        super(owner, detailFactory, culpritContainer.getAnnotations(), defaultEncoding, header, culpritContainer.getHierarchy());
        this.culpritName = culpritContainer.getFullName();
        this.culpritEmail = culpritContainer.getEmail();
    }

    /** {@inheritDoc} */
    public String getDisplayName() {
        return "".equals(culpritName) ? "Unknown users" : culpritName;
    }

    public User getUser() {
        if(userAttempted) {
            return user;
        }
        userAttempted = true;
        if("".equals(culpritName)) {
            return null;
        }
        SCM scm = getOwner().getProject().getScm();
        if((scm == null) || (scm instanceof NullSCM)) {
            scm = getOwner().getProject().getRootProject().getScm();
        }
        try {
            user = GitFileAnnotationBlamer.findOrCreateUser(culpritName, culpritEmail, scm);
        } catch(NoClassDefFoundError e) {
            // Git wasn't installed, ignore
        }

        return user;
    }
}
