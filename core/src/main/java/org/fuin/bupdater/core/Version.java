package org.fuin.bupdater.core;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Combines the "raw" Maven version as string and a semantic version if the string can be converted into one.
 * CAUTION: If some versions use semantic versioning and some do not, ordering might be kind of random.
 *
 * @param id Raw maven version.
 * @param semver Semantic version if ID is one.
 */
public record Version(String id,
                      @Nullable Semver semver) implements Comparable<Version> {

    public Version {
        Objects.requireNonNull(id, "id==null");
    }

    public Version(String str) {
        this(str, Semver.coerce(str).orElse(null));
    }

    public boolean isStable() {
        if (semver == null) {
            // We don't know... Assume YES.
            return true;
        }
        return semver.isStable();
    }

    @Override
    public int compareTo(Version other) {
        if (semver == null || other.semver == null) {
            return id.compareTo(other.id);
        }
        return semver.compareTo(other.semver);
    }

    @Override
    public String toString() {
        return id;
    }

    public static Version coerce(String s) {
        return Semver.coerce(s)
                .map(value -> new Version(s, value))
                .orElseGet(() -> new Version(s, null));
    }

}
