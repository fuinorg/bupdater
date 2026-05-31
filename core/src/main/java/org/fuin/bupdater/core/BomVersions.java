package org.fuin.bupdater.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Versions of a single artifact as defined by the different framework BOMs, together with the
 * information needed to harmonize them (fixed versions and strategy).
 *
 * @param artifact      Artifact the versions belong to (must not contain a regular expression).
 * @param versions      Version per source BOM (the {@link #EXTERNAL_SOURCE} marker is used when the
 *                      version was resolved from Maven Central instead of a framework BOM).
 * @param fixedVersions Manually fixed versions that override the harmonization result.
 * @param strategy      Strategy used to pick a version when the BOMs disagree.
 */
public record BomVersions(MavenGA artifact,
                          Map<MavenGA, Version> versions,
                          List<MavenGAV> fixedVersions,
                          Strategy strategy) {

    /** Synthetic source marker used when an included artifact is not provided by any BOM. */
    public static final MavenGA EXTERNAL_SOURCE = new MavenGA("external", "external");

    public BomVersions {
        Objects.requireNonNull(artifact, "artifact==null");
        if (artifact.hasRegularExpressions()) {
            throw new IllegalStateException("Artifact must not have a regular expression, but was: " + artifact);
        }
        Objects.requireNonNull(versions, "versions==null");
        if (versions.keySet().stream().anyMatch(v -> v.equals(artifact))) {
            throw new IllegalStateException("Versions must not have any regular expression, but was: " + versions);
        }
        Objects.requireNonNull(fixedVersions, "fixedVersions==null");
        Objects.requireNonNull(strategy, "strategy==null");
    }

    public BomVersions(MavenGA artifact) {
        this(artifact, new HashMap<>(), new ArrayList<>(), Strategy.HIGHEST);
    }

    public Optional<Version> get(final MavenGA artifact) {
        return Optional.ofNullable(versions.get(artifact));
    }

    public Optional<Version> harmonized() {
        Optional<MavenGAV> fixedVersion = findFixedVersion(fixedVersions, artifact);
        return fixedVersion.map(MavenGAV::version).or(() -> strategy.harmonized(versions.values()));
    }

    public boolean isFailed() {
        return findFixedVersion(fixedVersions, artifact).isEmpty();
    }

    private Optional<MavenGAV> findFixedVersion(List<MavenGAV> fixedVersions, MavenGA artifact) {
        return fixedVersions.stream().filter(v -> v.ga().similar(artifact)).findFirst();
    }

    public String getMajorDiffText() {
        if (!isMajorDiff()) {
            return "";
        }
        return artifact + "=>" + versions;
    }

    public boolean isMajorDiff() {
        Version prev = null;
        for (Version version : versions.values()) {
            if (prev == null) {
                prev = version;
            } else {
                if (version.semver() != null && prev.semver() != null) {
                    if (version.semver().major() != prev.semver().major()) {
                        return true;
                    }
                } else {
                    if (version.id().compareTo(prev.id()) > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String getAnyDiffText() {
        if (!isAnyDiff()) {
            return "";
        }
        return artifact + " => " + harmonized().orElseThrow() + " " + versions;
    }

    public boolean isAnyDiff() {
        Version prev = null;
        for (final Version version : versions.values()) {
            if (prev == null) {
                prev = version;
            } else {
                if (!version.equals(prev)) {
                    return true;
                }
            }
        }
        return false;
    }

}
