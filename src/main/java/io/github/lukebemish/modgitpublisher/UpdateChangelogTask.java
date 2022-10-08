package io.github.lukebemish.modgitpublisher;

import org.eclipse.jgit.api.Git;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Files;

public class UpdateChangelogTask extends DefaultTask {
    @TaskAction
    public void updateChangelog() {
        var extension = getProject().getExtensions().getByType(ModGitPublisherExtension.class);

        try (Git git = Git.open(getProject().getRootDir())) {
            var messages = CommitAnalyzer.getCommitMessages(git);
            if (messages.size() == 0)
                return;
            var newestTaggedVersion = CommitAnalyzer.getSemVerVersion(git, CommitAnalyzer.getNewestVersionedTag(git));
            var path = extension.getChangelogFile().get().getAsFile().toPath();
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            String existingChangelog = new String(Files.readAllBytes(path)).replaceAll("^(\\s*[\\n\\r]+\\s*)*", "");
            String firstLine = existingChangelog.split("\\r?\\n")[0];
            if (firstLine.startsWith("## ")) {
                var remaining = firstLine.substring(3);
                if (remaining.startsWith(extension.getProjectName().get()+" ")) {
                    remaining = remaining.substring(extension.getProjectName().get().length()+1);
                    var fullVersion = remaining.trim()+"-"+extension.getMinecraftVersion().get();
                    CommitAnalyzer.SemVerVersion topVersion = CommitAnalyzer.getSemVerVersion(fullVersion);
                    if (topVersion != null) {
                        if (topVersion.isNewerThan(newestTaggedVersion)) {
                            existingChangelog = existingChangelog.substring(firstLine.length());
                            int index = existingChangelog.indexOf("\n## ")+1;
                            if (index == 0)
                                existingChangelog = "";
                            else
                                existingChangelog = existingChangelog.substring(index);
                        }
                    }
                }
            }
            String newChangelog = CommitMessage.getChangelog(messages);
            String header = "## "+extension.getProjectName().get()+" "+getProject().getVersion()+"\n\n";
            Files.write(path, (header + newChangelog + "\n" + existingChangelog).getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
