package org.fuin.bupdater.core;

import eu.maveniverse.maven.mima.context.Context;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Holds the harmonized versions of the configured artifacts across several framework BOMs and
 * provides queries on the differences. Use the {@link Builder} to create an instance. Repository
 * access is delegated to a {@link BomResolver}, and writing the result back into a POM is delegated
 * to a {@link PomDependenciesWriter}.
 */
public final class BomUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(BomUpdater.class);

    private final List<MavenGAV> frameworks;

    private final Map<MavenGA, Map<MavenGA, Version>> map;

    private final List<MavenGAV> fixedVersions;

    private final List<MavenGA> includes;

    private final Strategy strategy;

    private BomUpdater(List<MavenGAV> frameworks,
                       Map<MavenGA, Map<MavenGA, Version>> map,
                       List<MavenGAV> fixedVersions,
                       List<MavenGA> includes,
                       Strategy strategy) {
        this.frameworks = Objects.requireNonNull(frameworks, "frameworks==null");
        this.map = Objects.requireNonNull(map, "map==null");
        this.fixedVersions = Objects.requireNonNull(fixedVersions, "fixedVersions==null");
        this.includes = Objects.requireNonNull(includes, "includes==null");
        this.strategy = Objects.requireNonNull(strategy, "strategy==null");

        LOG.info("Frameworks: {}", frameworks);
        LOG.info("Fixed versions: {}", fixedVersions);
        LOG.info("Included versions: {}", includes);
        LOG.info("Strategy: {}", strategy);
    }

    public List<MavenGAV> getFrameworks() {
        final List<MavenGAV> sorted = new ArrayList<>(frameworks);
        Collections.sort(sorted);
        return sorted;
    }

    public Stream<MavenGA> getArtifacts() {
        final List<MavenGA> artifacts = new ArrayList<>(map.keySet());
        Collections.sort(artifacts);
        return artifacts.stream().filter(this::included);
    }

    public Stream<BomVersions> getMajorDiffs() {
        return getBomVersions().filter(BomVersions::isMajorDiff);
    }

    public Stream<BomVersions> getAnyDiffs() {
        return getBomVersions().filter(BomVersions::isAnyDiff);
    }

    public String getMajorAsString() {
        return asString(getMajorDiffs().toList(), BomVersions::getMajorDiffText);
    }

    public String getAnyAsString() {
        return asString(getAnyDiffs().toList(), BomVersions::getAnyDiffText);
    }

    public boolean isFailed() {
        return getMajorDiffs()
                .anyMatch(v -> included(v.artifact()) && v.isFailed());
    }

    public Stream<BomVersions> getFailed() {
        return getMajorDiffs().filter(v -> included(v.artifact()) && v.isFailed());
    }

    public String getFailedAsString() {
        return asString(getFailed().toList(), BomVersions::getMajorDiffText);
    }

    public Stream<BomVersions> getBomVersions() {
        return getArtifacts().map(this::bomVersionsOf)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public Optional<BomVersions> bomVersionsOf(MavenGA artifact) {
        final Map<MavenGA, Version> versions = map.get(artifact);
        if (versions == null) {
            return Optional.empty();
        }
        return Optional.of(new BomVersions(artifact, versions, fixedVersions, strategy));
    }

    public void updateDependencies(File pomFile) {
        LOG.info("Updating: {}", pomFile.getAbsolutePath());
        new PomDependenciesWriter().write(pomFile, frameworks, getBomVersions().toList());
    }

    private boolean included(MavenGA artifact) {
        return includes.stream().anyMatch(include -> include.similar(artifact));
    }

    private static String asString(List<BomVersions> diffs, Function<BomVersions, String> func) {
        final StringBuilder builder = new StringBuilder();
        for (final BomVersions bom : diffs) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(func.apply(bom));
        }
        return builder.toString();
    }

    /**
     * Builds a {@link BomUpdater} by reading framework BOMs through a {@link BomResolver} and
     * collecting the versions of the included artifacts.
     */
    public static class Builder {

        private final BomResolver resolver;

        private final Set<MavenGAV> frameworks;

        private final Map<MavenGA, Map<MavenGA, Version>> map;

        private final List<MavenGAV> fixedVersions;

        private final List<MavenGA> includes;

        private Strategy strategy;

        private int bomCount;

        /**
         * Constructor using a MIMA {@link Context} to access the repositories.
         *
         * @param context  Context used to resolve the artifacts.
         * @param includes Artifacts to include (may be {@literal null} for none).
         */
        public Builder(Context context, @Nullable List<String> includes) {
            this(new MimaBomResolver(context), includes);
        }

        /**
         * Constructor using an explicit {@link BomResolver} (mainly for testing).
         *
         * @param resolver Resolver used to access the artifacts.
         * @param includes Artifacts to include (may be {@literal null} for none).
         */
        public Builder(BomResolver resolver, @Nullable List<String> includes) {
            this.resolver = Objects.requireNonNull(resolver, "resolver==null");
            this.frameworks = new HashSet<>();
            this.map = new HashMap<>();
            this.fixedVersions = new ArrayList<>();
            if (includes == null) {
                this.includes = List.of();
            } else {
                this.includes = includes.stream().map(MavenGA::of).toList();
            }
            this.strategy = Strategy.HIGHEST;
            this.bomCount = 0;
        }

        public Builder addBom(String groupId, String artifactId, String version) {
            Objects.requireNonNull(groupId, "groupId==null");
            Objects.requireNonNull(artifactId, "artifactId==null");
            Objects.requireNonNull(version, "id==null");
            bomCount++;
            final MavenGA bom = new MavenGA(groupId, artifactId);
            frameworks.add(new MavenGAV(bom, Objects.requireNonNull(Version.coerce(version))));
            LOG.info("Adding Bom: {}:{}", bom, version);
            read(bom, groupId, artifactId, version);
            return this;
        }

        public Builder addBoms(Model model) {
            Objects.requireNonNull(model, "model==null");
            if (model.getDependencyManagement() != null
                    && model.getDependencyManagement().getDependencies() != null) {
                model.getDependencyManagement().getDependencies()
                        .forEach(dep -> addBom(dep.getGroupId(), dep.getArtifactId(), dep.getVersion()));
            }
            return this;
        }

        public Builder addBom(String framework) {
            Objects.requireNonNull(framework, "framework==null");
            final MavenGAV gav = MavenGAV.of(framework);
            addBom(gav.ga().groupId(), gav.ga().artifactId(), gav.version().toString());
            return this;
        }

        public Builder addBoms(List<String> frameworks) {
            Objects.requireNonNull(frameworks, "frameworks==null");
            frameworks.forEach(this::addBom);
            return this;
        }

        public Builder strategy(Strategy strategy) {
            this.strategy = Objects.requireNonNull(strategy, "strategy==null)");
            return this;
        }

        public Builder addFixedVersion(String version) {
            Objects.requireNonNull(version, "id==null");
            return addFixedVersion(MavenGAV.of(version));
        }

        public Builder addFixedVersion(MavenGAV version) {
            Objects.requireNonNull(version, "id==null");
            this.fixedVersions.add(version);
            return this;
        }

        public Builder addFixedVersions(@Nullable List<String> fixedVersions) {
            if (fixedVersions != null) {
                fixedVersions.forEach(this::addFixedVersion);
            }
            return this;
        }

        public BomUpdater build() {
            if (bomCount == 0) {
                throw new IllegalStateException("No BOMs added!");
            }
            for (final MavenGA include : includes) {
                if (!include.hasRegularExpressions() && !map.containsKey(include)) {
                    final Version latest = resolver.resolveLatestStableVersion(include);
                    LOG.info("Resolved latest stable version for {} (not in any BOM): {}", include, latest);
                    map.computeIfAbsent(include, k -> new HashMap<>())
                            .put(BomVersions.EXTERNAL_SOURCE, latest);
                }
            }
            final List<MavenGAV> fw = new ArrayList<>(frameworks);
            Collections.sort(fw);
            return new BomUpdater(fw, map, fixedVersions, includes, strategy);
        }

        private void read(MavenGA bom, String groupId, String artifactId, String version) throws CoreException {
            final Model bomModel = resolver.resolveModel(
                    new MavenGAV(groupId, artifactId, Objects.requireNonNull(Version.coerce(version))));
            bomModel.getDependencyManagement().getDependencies().stream()
                    .filter(dep -> includes.stream()
                            .anyMatch(include -> include.similar(new MavenGA(dep.getGroupId(), dep.getArtifactId()))))
                    .forEach(dep -> add(bom, groupId, version, dep));
        }

        private void add(MavenGA bom, String bomGroup, String bomVersion, Dependency dep) throws CoreException {
            if (dep.getArtifactId().endsWith("-bom") || dep.getArtifactId().endsWith("_bom")) {
                read(bom, dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
            } else {
                final String type = dep.getType() == null ? "jar" : dep.getType();
                if (!"jar".equals(type)) {
                    LOG.debug("Skipping non-jar dependency: {}:{}:{} (classifier={}, type={})",
                            dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getClassifier(), type);
                    return;
                }
                if (dep.getClassifier() != null) {
                    LOG.debug("Skipping dependency with classifier: {}:{}:{} (classifier={}, type={})",
                            dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getClassifier(), type);
                    return;
                }
                final String groupId = dep.getGroupId().startsWith("${") ? bomGroup : dep.getGroupId();
                final String version = dep.getVersion().startsWith("${") ? bomVersion : dep.getVersion();
                try {
                    resolver.resolveModel(new MavenGAV(dep.getGroupId(), dep.getArtifactId(),
                            Objects.requireNonNull(Version.coerce(dep.getVersion()))));
                    map.computeIfAbsent(new MavenGA(groupId, dep.getArtifactId()), k -> new HashMap<>())
                            .put(bom, Version.coerce(version));
                } catch (final CoreException ex) {
                    LOG.error("Failed to resolve: " + dep.getGroupId() + ":" + dep.getArtifactId()
                            + ":" + dep.getVersion(), ex);
                }
            }
        }

    }

}
