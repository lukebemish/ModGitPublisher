package io.github.lukebemish.modgitpublisher;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Function;

public record CommitMessage(CommitType type, String message, boolean breaking) {
    public enum CommitType {
        FEATURE(CommitAnalyzer.SemVerVersion::bumpMinor),
        FIX(CommitAnalyzer.SemVerVersion::bumpPatch),
        MAINTENANCE(CommitAnalyzer.SemVerVersion::bumpPatch),
        OTHER(CommitAnalyzer.SemVerVersion::bumpPatch);

        public final Function<CommitAnalyzer.SemVerVersion, CommitAnalyzer.SemVerVersion> bump;

        CommitType(Function<CommitAnalyzer.SemVerVersion, CommitAnalyzer.SemVerVersion> bump) {
            this.bump = bump;
        }

        public CommitType largerChange(CommitType other) {
            if (this == FEATURE)
                return this;
            if (other == FEATURE)
                return other;
            return this;
        }
    }

    @Nullable
    public static CommitMessage fromString(String message) {
        if (message == null || message.isBlank())
            return null;
        String[] parts = message.split(":", 2);
        if (parts.length != 2)
            return new CommitMessage(CommitType.OTHER, message.trim(), false);
        if (parts[0].isBlank())
            return new CommitMessage(CommitType.OTHER, parts[1].trim(), false);
        if (!parts[0].matches("[a-zA-Z!]+"))
            return new CommitMessage(CommitType.OTHER, message.trim(), false);
        return switch (parts[0].toUpperCase(Locale.ROOT)) {
            case "FEAT", "FEATURE" -> new CommitMessage(CommitType.FEATURE, parts[1].trim(), false);
            case "FEAT!", "FEATURE!" -> new CommitMessage(CommitType.FEATURE, parts[1].trim(), true);
            case "FIX" -> new CommitMessage(CommitType.FIX, parts[1].trim(), false);
            case "FIX!" -> new CommitMessage(CommitType.FIX, parts[1].trim(), true);
            case "CHORE", "IGNORED" -> null;
            case "REFACTOR", "DOCS", "STYLE" -> new CommitMessage(CommitType.MAINTENANCE, parts[1].trim(), false);
            case "REFACTOR!", "DOCS!", "STYLE!" -> new CommitMessage(CommitType.MAINTENANCE, parts[1].trim(), true);
            default -> new CommitMessage(CommitType.OTHER, parts[1].trim(), parts[0].endsWith("!"));
        };
    }

    public static CommitAnalyzer.SemVerVersion computeBumpedVersion(Collection<CommitMessage> messages, CommitAnalyzer.SemVerVersion original) {
        if (messages.isEmpty())
            return original;
        if (messages.stream().anyMatch(CommitMessage::breaking))
            return original.bumpMajor();
        CommitType type = messages.stream().map(CommitMessage::type).reduce(CommitType.OTHER, CommitType::largerChange);
        return type.bump.apply(original);
    }

    public static CommitAnalyzer.SemVerVersion computeBumpedVersion(Collection<CommitMessage> messages, CommitAnalyzer.SemVerVersion original, String minecraftVersion) {
        return computeBumpedVersion(messages, original).withMinecraftVersion(minecraftVersion);
    }

    public static String getChangelog(List<CommitMessage> messages) {
        var flippedMessages = new ArrayList<>(messages);
        Collections.reverse(flippedMessages);
        StringBuilder builder = new StringBuilder();
        List<CommitMessage> features = flippedMessages.stream().filter(message -> message.type() == CommitType.FEATURE).toList();
        List<CommitMessage> fixes = flippedMessages.stream().filter(message -> message.type() == CommitType.FIX).toList();
        List<CommitMessage> other = flippedMessages.stream().filter(message -> message.type() == CommitType.OTHER || message.type() == CommitType.MAINTENANCE).toList();
        if (!features.isEmpty()) {
            builder.append("### Features").append("\n");
            for (CommitMessage message : features) {
                builder.append("- ");
                if (message.breaking())
                    builder.append("**BREAKING** ");
                builder.append(message.message()).append("\n");
            }
        }
        if (!fixes.isEmpty()) {
            builder.append("### Fixes").append("\n");
            for (CommitMessage message : fixes) {
                builder.append("- ");
                if (message.breaking())
                    builder.append("**BREAKING** ");
                builder.append(message.message()).append("\n");
            }
        }
        if (!other.isEmpty()) {
            builder.append("### Other").append("\n");
            for (CommitMessage message : other) {
                builder.append("- ");
                if (message.breaking())
                    builder.append("**BREAKING** ");
                builder.append(message.message()).append("\n");
            }
        }
        return builder.toString();
    }
}
