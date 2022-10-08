package io.github.lukebemish.modgitpublisher;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CommitAnalyzer {
    public static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)-([0-9.]+)");

    public record SemVerVersion(int major, int minor, int patch, String minecraft) {
        String asTag() {
            return String.format("%d.%d.%d-%s", major, minor, patch, minecraft);
        }

        String asGradleVersion() {
            return String.format("%d.%d.%d", major, minor, patch);
        }

        SemVerVersion bumpMinor() {
            return new SemVerVersion(major, minor + 1, 0, minecraft);
        }

        SemVerVersion bumpMajor() {
            return new SemVerVersion(major + 1, 0, 0, minecraft);
        }

        SemVerVersion bumpPatch() {
            return new SemVerVersion(major, minor, patch + 1, minecraft);
        }

        SemVerVersion withMinecraftVersion(String minecraft) {
            return new SemVerVersion(major, minor, patch, minecraft);
        }

        boolean isNewerThan(SemVerVersion other) {
            if (major > other.major)
                return true;
            if (minor > other.minor)
                return true;
            return patch > other.patch;
        }
    }

    @Nullable
    public static SemVerVersion getSemVerVersion(@Nullable String version) {
        if (version == null)
            return null;
        var matcher = VERSION.matcher(version);
        if (!matcher.matches())
            return null;
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        String minecraft = matcher.group(4);
        return new SemVerVersion(major, minor, patch, minecraft);
    }

    public static SemVerVersion getSemVerVersion(Git git, ObjectId objectId) throws MissingObjectException, GitAPIException {
        var revCommit = git.nameRev().add(objectId).addPrefix("refs/tags/").call();
        return getSemVerVersion(revCommit.get(objectId));
    }

    public static ObjectId getNewestVersionedTag(Git git) throws GitAPIException, MissingObjectException {
        for(RevCommit commit :git.log().call()) {
            Map<ObjectId,String> names = git.nameRev().add(commit).addPrefix( "refs/tags/" ).call();
            for (Map.Entry<ObjectId, String> entry : names.entrySet()) {
                if (getSemVerVersion(entry.getValue()) != null) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    public static List<CommitMessage> getCommitMessages(Git git) throws GitAPIException, MissingObjectException {
        ObjectId tag = getNewestVersionedTag(git);
        List<CommitMessage> messages = new ArrayList<>();
        for (RevCommit commit : git.log().call()) {
            Map<ObjectId,String> names = git.nameRev().add(commit).addPrefix( "refs/tags/" ).call();
            if (names.containsKey(tag))
                break;
            CommitMessage message = CommitMessage.fromString(commit.getFullMessage().split("\n")[0]);
            if (message != null)
                messages.add(message);
        }
        return messages;
    }
}
