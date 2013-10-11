package hudson.plugins.analysis.core;

import hudson.FilePath.FileCallable;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.analysis.util.GitFileAnnotationBlamer;
import hudson.plugins.analysis.util.PluginLogger;
import hudson.plugins.analysis.util.StringPluginLogger;
import hudson.remoting.VirtualChannel;
import hudson.scm.NullSCM;
import hudson.scm.SCM;

import java.io.File;
import java.io.IOException;

/**
 * A tool for assigning blame to the specific warnings based on SCM metadata.
 * <br>
 * This isn't static because if we do support additional SCMs then we'd
 * probably want to refashion this to be created by a factory.
 * @author jgibson
 */
public class BlameAssigner {
    /**
     * Assigns blame to the {@code FileAnnotation}s in the specified
     * {@code ParserResult} using git.
     *
     * @param build the owning build.
     * @param parserResult the results of analysis.
     * @param pluginId the name of the plugin (used for log messages).
     * @param pluginLogger used for log messsages.
     * @param listener a listener for git operations.
     * @throws IOException if there is an error during blame analysis.
     * @throws InterruptedException if the user cancels the operation.
     */
    public void assignBlame(final AbstractBuild<?, ?> build, final ParserResult parserResult,
            final String pluginId, final PluginLogger pluginLogger, final TaskListener listener)
            throws IOException, InterruptedException {
        pluginLogger.log("Trying to assign blame for annotations.");

        SCM scm = build.getProject().getScm();
        final AbstractBuild<?, ?> scmBuild;
        if ((scm == null) || (scm instanceof NullSCM)) {
            scmBuild = build.getRootBuild();
        }
        else {
            scmBuild = build;
        }

        pluginLogger.logLines(build.getWorkspace().act(new FileCallable<String>() {
            private static final long serialVersionUID = 1L;
            public String invoke(final File workspace, final VirtualChannel vc) throws IOException, InterruptedException {
                final StringPluginLogger logger = new StringPluginLogger(pluginId);
                try {
                    GitFileAnnotationBlamer.blameAnnotations(scmBuild, listener, workspace, parserResult, logger);
                }
                catch (NoClassDefFoundError e) {
                    logger.log("Could not blame annotations because git plugin wasn't installed.");
                }
                return logger.toString();
            }
        }));
    }
}
