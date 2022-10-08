package io.github.lukebemish.modgitpublisher;

import org.eclipse.jgit.api.Git;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.List;

public class ModGitPublisherPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        try (Git git = Git.open(project.getRootDir())) {
            var extension = project.getExtensions().create("modGitPublisher", ModGitPublisherExtension.class, project);
            CommitAnalyzer.SemVerVersion oldVersion = CommitAnalyzer.getSemVerVersion(git,CommitAnalyzer.getNewestVersionedTag(git));
            List<CommitMessage> messages = CommitAnalyzer.getCommitMessages(git);
            CommitAnalyzer.SemVerVersion newVersion = CommitMessage.computeBumpedVersion(messages, oldVersion, extension.getMinecraftVersion().get());
            project.setVersion(newVersion.asGradleVersion());
        } catch (Exception ignored) {

        }

        project.getTasks().register("updateChangelog", UpdateChangelogTask.class);
        project.getTasks().register("commitChangelog", CommitChangelogTask.class);
        project.getTasks().getByName("commitChangelog").dependsOn("updateChangelog");
        project.getTasks().register("tagRelease", TagVersionTask.class);
        project.getTasks().getByName("tagRelease").dependsOn("commitChangelog");
    }
}
