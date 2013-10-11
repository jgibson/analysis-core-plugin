package hudson.plugins.analysis.util;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.plugins.analysis.core.ParserResult;
import hudson.plugins.analysis.util.model.FileAnnotation;
import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.browser.GitRepositoryBrowser;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCM;
import hudson.tasks.Mailer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;

/**
 * A tool for assigning blame to {@code FileAnnotation}s with git.  It is
 * segregated in this file to avoid NoClassDefFoundErrors if the git plugin
 * isn't installed.
 *
 * @author jgibson
 */
public class GitFileAnnotationBlamer {
    private GitFileAnnotationBlamer() {}

    /**
     * Assigns blame to the results of analysis using the git repository information.
     * <br>
     * If the specified build is not backed by a {@code GitSCM} then this will
     * do nothing.
     *
     * @param build the build upon which to invoke blame.
     * @param listener a listener for the build.
     * @param workspace the workspace for the build.
     * @param analysisResult the results of an analysis.
     * @param logger a logger for the build.
     * @throws IOException if there is an error running the build.
     * @throws InterruptedException if the user cancels the build.
     */
    public static void blameAnnotations(AbstractBuild<?, ?> build, TaskListener listener, File workspace, ParserResult analysisResult, PluginLogger logger) throws IOException, InterruptedException {
        if(analysisResult.getAnnotations().isEmpty()) return;

        SCM scm = build.getProject().getScm();
        if(!(scm instanceof GitSCM)) {
            logger.log("Skipping blame because SCM was not a GitSCM: " + scm);
            return;
        }

        final EnvVars environment = build.getEnvironment(listener);
        final String gitCommit = environment.get("GIT_COMMIT");
        GitSCM gscm = (GitSCM) scm;

        final String gitExe = gscm.getGitExe(build.getBuiltOn(), listener);

        GitClient git = Git.with(listener, environment)
                .in(workspace)
                .using(gitExe)
                .getClient();

        ObjectId headCommit;
        if((gitCommit == null) || "".equals(gitCommit)) {
            // Not sure if this is the right guess, but I couldn't figure out where else the commit id is stored
            logger.log("No GIT_COMMIT environment variable found, using HEAD.");
            headCommit = git.revParse("HEAD");
        } else {
            headCommit = git.revParse(gitCommit);
        }
        if(headCommit == null) {
            logger.log("Could not obtain commit id from: " + gitCommit + " aborting.");
            return;
        }
        final String absoluteWorkspace = workspace.getAbsolutePath();
        HashMap<String, String> nameMap = new HashMap<String, String>();
        for(final FileAnnotation annot : analysisResult.getAnnotations()) {
            if(nameMap.containsKey(annot.getFileName())) {
                continue;
            }
            if(annot.getPrimaryLineNumber() <= 0) {
                continue;
            }
            if(!annot.getFileName().startsWith(absoluteWorkspace)) {
                logger.log("Saw a file outside of the workspace? " + annot.getFileName());
                nameMap.put(annot.getFileName(), "/");
                continue;
            }
            String child = annot.getFileName().substring(absoluteWorkspace.length());
            if(child.startsWith("/") || child.startsWith("\\")) {
                child = child.substring(1);
            }
            nameMap.put(annot.getFileName(), child);
        }

        HashMap<String, BlameResult> blameResults = new HashMap<String, BlameResult>();
        for(final String child : nameMap.values()) {
            if("/".equals(child)) continue;
            BlameCommand blame = new BlameCommand(git.getRepository());
            blame.setFilePath(child);
            blame.setStartCommit(headCommit);
            try {
                BlameResult result = blame.call();
                //BlameGenerator gen = new BlameGenerator(git.getRepository(), child);
                //gen.push("CurrentHead", headCommit);
                //BlameResult blame = gen.computeBlameResult();
                blameResults.put(child, result);
                if(Thread.interrupted()) {
                    throw new InterruptedException("Thread was interrupted while computing blame information.");
                }
            } catch(GitAPIException e) {
                final IOException e2 = new IOException("Error running git blame on " + child + " with revision: " + headCommit);
                e2.initCause(e);
                throw e2;
            }
        }

        for(final FileAnnotation annot : analysisResult.getAnnotations()) {
            if(annot.getPrimaryLineNumber() <= 0) {
                continue;
            }
            String child = nameMap.get(annot.getFileName());
            if("/".equals(child)) {
                continue;
            }
            BlameResult blame = blameResults.get(child);
            if(blame == null) {
                logger.log("No blame result available for: " + annot);
                continue;
            }
            int zeroline = annot.getPrimaryLineNumber() - 1;
            try {
                PersonIdent who = blame.getSourceAuthor(zeroline);
                RevCommit commit = blame.getSourceCommit(zeroline);
                if(who != null) {
                    annot.setCulpritName(who.getName());
                    annot.setCulpritEmail(who.getEmailAddress());
                }
                annot.setCulpritCommitId(commit == null ? null : commit.getName());
            } catch(ArrayIndexOutOfBoundsException e) {
                logger.log("Blame details were out of bounds for line number " + annot.getPrimaryLineNumber() + " in file " + child);
            }
        }
    }

    /**
     * Returns user of the change set.  Stolen from hudson.plugins.git.GitChangeSet.
     *
     * @param csAuthor user name.
     * @param csAuthorEmail user email.
     * @param createAccountBasedOnEmail true if create new user based on committer's email.
     * @return {@link User}
     */
    public static User findOrCreateUser(String fullName, String email, SCM scm) {
        if(!(scm instanceof GitSCM)) {
            return null;
        }

        GitSCM gscm = (GitSCM) scm;
        boolean createAccountBasedOnEmail = gscm.isCreateAccountBasedOnEmail();

        User user;
        if (createAccountBasedOnEmail) {
            user = User.get(email, false);

            if (user == null) {
                try {
                    user = User.get(email, true);
                    user.setFullName(fullName);
                    user.addProperty(new Mailer.UserProperty(email));
                    user.save();
                } catch (IOException e) {
                    // add logging statement?
                }
            }
        } else {
            user = User.get(fullName, false);

            if (user == null)
                user = User.get(email.split("@")[0], true);
        }
        // set email address for user if none is already available
        // Let's not do this because git plugin 1.2.0 doesn't.
//        if (fixEmpty(csAuthorEmail) != null && !isMailerPropertySet(user)) {
//            try {
//                user.addProperty(new Mailer.UserProperty(csAuthorEmail));
//            } catch (IOException e) {
//                // ignore error
//            }
//        }
        return user;
    }

    public static Map<String, URL> computeUrlsForCommitIds(SCM scm, Set<String> commitIds) {
        if(!(scm instanceof GitSCM)) {
            return null;
        }
        if(commitIds.isEmpty()) {
            return null;
        }

        GitSCM gscm = (GitSCM) scm;
        GitRepositoryBrowser browser = gscm.getBrowser();
        if(browser == null) {
            RepositoryBrowser<?> ebrowser = gscm.getEffectiveBrowser();
            if(ebrowser instanceof GitRepositoryBrowser) {
                browser = (GitRepositoryBrowser) ebrowser;
            } else {
                return null;
            }
        }

        // This is a dirty hack because the only way to create changesets is to do it by parsing git log messages
        // Because what we're doing is fairly dangerous (creating minimal commit messages) just give up if there is an error
        try {
            HashMap<String, URL> result = new HashMap<String, URL>((int) (commitIds.size() * 1.5f));
            for(final String commitId : commitIds) {
                GitChangeSet cs = new GitChangeSet(Collections.singletonList("commit " + commitId), true);
                if(cs.getId() != null) {
                    result.put(commitId, browser.getChangeSetLink(cs));
                }
            }

            return result;
        } catch(Exception e) {
            // TODO: log?
            return null;
        }
    }
}
