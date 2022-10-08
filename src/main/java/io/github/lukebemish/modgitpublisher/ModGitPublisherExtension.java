package io.github.lukebemish.modgitpublisher;

import org.eclipse.jgit.api.Git;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

import java.util.List;

public abstract class ModGitPublisherExtension {
    abstract Property<String> getProjectName();
    abstract Property<String> getMinecraftVersion();
    abstract RegularFileProperty getChangelogFile();

    protected final Project project;

    public ModGitPublisherExtension(final Project project) {
        getChangelogFile().set(project.file("CHANGELOG.md"));
        this.project = project;
    }

    public String getChangelog() {
        try (Git git = Git.open(project.getRootDir())) {
            List<CommitMessage> messages = CommitAnalyzer.getCommitMessages(git);
            return "## " + getProjectName().get() + " " + project.getVersion()+"\n\n" + CommitMessage.getChangelog(messages);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getVersion() {
        try (Git git = Git.open(project.getRootDir())) {
            CommitAnalyzer.SemVerVersion oldVersion = CommitAnalyzer.getSemVerVersion(git,CommitAnalyzer.getNewestVersionedTag(git));
            List<CommitMessage> messages = CommitAnalyzer.getCommitMessages(git);
            CommitAnalyzer.SemVerVersion newVersion = CommitMessage.computeBumpedVersion(messages, oldVersion);
            return newVersion.asGradleVersion();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}