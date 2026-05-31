package org.fuin.bupdater.core;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory {@link BomResolver} for tests. BOM models and resolvable artifacts are registered up
 * front; anything not registered makes {@link #resolveModel(MavenGAV)} throw a {@link CoreException}.
 */
final class FakeBomResolver implements BomResolver {

    private final Map<String, Model> models = new HashMap<>();

    private final Map<MavenGA, Version> latestStable = new HashMap<>();

    FakeBomResolver registerBom(String groupId, String artifactId, String version, Dependency... deps) {
        final Model model = new Model();
        final DependencyManagement dm = new DependencyManagement();
        for (final Dependency dep : deps) {
            dm.addDependency(dep);
        }
        model.setDependencyManagement(dm);
        models.put(key(groupId, artifactId, version), model);
        return this;
    }

    FakeBomResolver registerArtifact(String groupId, String artifactId, String version) {
        models.put(key(groupId, artifactId, version), new Model());
        return this;
    }

    FakeBomResolver registerLatestStable(String groupId, String artifactId, String version) {
        latestStable.put(new MavenGA(groupId, artifactId), new Version(version));
        return this;
    }

    @Override
    public Model resolveModel(MavenGAV gav) {
        final String key = key(gav.ga().groupId(), gav.ga().artifactId(), gav.version().toString());
        final Model model = models.get(key);
        if (model == null) {
            throw new CoreException("Not registered: " + key);
        }
        return model;
    }

    @Override
    public Version resolveLatestStableVersion(MavenGA ga) {
        final Version version = latestStable.get(ga);
        if (version == null) {
            throw new CoreException("No latest stable registered for: " + ga);
        }
        return version;
    }

    static Dependency dependency(String groupId, String artifactId, String version,
                                 @Nullable String type, @Nullable String classifier) {
        final Dependency dep = new Dependency();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setVersion(version);
        if (type != null) {
            dep.setType(type);
        }
        if (classifier != null) {
            dep.setClassifier(classifier);
        }
        return dep;
    }

    private static String key(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version;
    }

}
