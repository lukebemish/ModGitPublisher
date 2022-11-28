package dev.lukebemish.modgitpublisher;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

@SuppressWarnings("unused")
public class ModGitPublisherPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().create("modGitPublisher", ModGitPublisherExtension.class, project);

        project.getTasks().register("updateChangelog", UpdateChangelogTask.class);
        project.getTasks().register("commitChangelog", CommitChangelogTask.class);
        project.getTasks().getByName("commitChangelog").dependsOn("updateChangelog");
        project.getTasks().register("tagRelease", TagVersionTask.class);
        project.getTasks().getByName("tagRelease").dependsOn("commitChangelog");
    }
}
