package dev.lukebemish.modgitpublisher;

import org.eclipse.jgit.api.Git;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

public class TagVersionTask extends DefaultTask {
    @TaskAction
    public void release() {
        try (Git git = Git.open(getProject().getRootDir())) {
            var extension = getProject().getExtensions().getByType(ModGitPublisherExtension.class);
            CommitAnalyzer.SemVerVersion oldVersion = CommitAnalyzer.getSemVerVersion(git,CommitAnalyzer.getNewestVersionedTag(git));
            List<CommitMessage> messages = CommitAnalyzer.getCommitMessages(git);
            CommitAnalyzer.SemVerVersion newVersion = CommitMessage.computeBumpedVersion(messages, oldVersion, extension.getMinecraftVersion().get());
            git.tag().setName(newVersion.asTag()).setForceUpdate(true).call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
