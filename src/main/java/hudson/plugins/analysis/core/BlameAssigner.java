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
 * This must be invoked on the workspace of the build.
 *
 * @author jgibson
 */
public class BlameAssigner {
    public BlameAssigner() {
    }

    public void assignBlame(AbstractBuild<?, ?> build, final ParserResult parserResult,
            final String pluginId, PluginLogger pluginLogger, final TaskListener listener)
            throws IOException, InterruptedException {
        pluginLogger.log("Trying to assign blame for annotations.");

        SCM scm = build.getProject().getScm();
        if((scm == null) || (scm instanceof NullSCM)) {
            build = build.getRootBuild();
        }

        final AbstractBuild<?, ?> scmBuild = build;
        pluginLogger.logLines(build.getWorkspace().act(new FileCallable<String>() {
            private static final long serialVersionUID = 1L;
            public String invoke(File workspace, VirtualChannel vc) throws IOException, InterruptedException {
                final StringPluginLogger logger = new StringPluginLogger(pluginId);
                try {
                    GitFileAnnotationBlamer.blameAnnotations(scmBuild, listener, workspace, parserResult, logger);
                } catch(NoClassDefFoundError e) {
                    logger.log("Could not blame annotations because git plugin wasn't installed.");
                }
                return logger.toString();
            }
        }));
    }
}
