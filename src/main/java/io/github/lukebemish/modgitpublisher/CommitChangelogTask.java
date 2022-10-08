package io.github.lukebemish.modgitpublisher;

import org.eclipse.jgit.api.Git;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class CommitChangelogTask extends DefaultTask {
    @TaskAction
    public void commitChangelog() {
        var extension = getProject().getExtensions().getByType(ModGitPublisherExtension.class);
        try (Git git = Git.open(getProject().getRootDir())) {
            var path = extension.getChangelogFile().get().getAsFile().toPath();
            var uncommitted = git.status().call().getUncommittedChanges();
            String toCommit = uncommitted.stream().filter(it->getProject().getRootProject().file(it).toPath().equals(path)).findFirst().orElse(null);
            if (toCommit != null) {
                git.add().addFilepattern(toCommit).call();
                git.commit().setMessage("CHORE: Automated changelog update").call();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
