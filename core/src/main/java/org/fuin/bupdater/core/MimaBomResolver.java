package org.fuin.bupdater.core;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.extensions.mmr.MavenModelReader;
import eu.maveniverse.maven.mima.extensions.mmr.ModelRequest;
import eu.maveniverse.maven.mima.extensions.mmr.ModelResponse;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link BomResolver} that uses a MIMA {@link Context} (Aether) to access the Maven repositories.
 */
public final class MimaBomResolver implements BomResolver {

    private static final Logger LOG = LoggerFactory.getLogger(MimaBomResolver.class);

    private final Context context;

    /**
     * Constructor with the MIMA context.
     *
     * @param context Context used to access the repositories.
     */
    public MimaBomResolver(Context context) {
        this.context = Objects.requireNonNull(context, "context==null");
    }

    @Override
    public Model resolveModel(MavenGAV gav) {
        Objects.requireNonNull(gav, "gav==null");
        return resolveArtifact(new DefaultArtifact(gav.ga().groupId(), gav.ga().artifactId(),
                null, null, gav.version().toString(), new DefaultArtifactType("pom")));
    }

    @Override
    public Version resolveLatestStableVersion(MavenGA ga) {
        Objects.requireNonNull(ga, "ga==null");
        final Artifact artifact = new DefaultArtifact(ga.groupId(), ga.artifactId(), null, "[0,)");
        final VersionRangeRequest request = new VersionRangeRequest();
        request.setArtifact(artifact);
        request.setRepositories(context.remoteRepositories());
        try {
            final VersionRangeResult result = context.repositorySystem()
                    .resolveVersionRange(context.repositorySystemSession(), request);
            final List<String> versions = result.getVersions().stream().map(Object::toString).toList();
            return findLatestStable(versions)
                    .orElseThrow(() -> new CoreException("No version found on remote repositories for: " + ga));
        } catch (final VersionRangeResolutionException ex) {
            throw new CoreException("Failed to resolve latest version of: " + ga, ex);
        }
    }

    /**
     * Returns the highest stable version from the given version strings. Pure logic without any
     * repository access, so it can be unit-tested directly.
     *
     * @param versionsAscending Version strings ordered from oldest to newest (the order returned by
     *                          the repository system).
     *
     * @return Highest stable version, or empty if none of the versions is stable.
     */
    static Optional<Version> findLatestStable(List<String> versionsAscending) {
        for (int i = versionsAscending.size() - 1; i >= 0; i--) {
            final Version ver = Version.coerce(versionsAscending.get(i));
            if (ver.isStable()) {
                return Optional.of(ver);
            }
        }
        return Optional.empty();
    }

    private Model resolveArtifact(Artifact artifact) {
        final ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(context.remoteRepositories());
        try {
            final ArtifactResult result = context.repositorySystem()
                    .resolveArtifact(context.repositorySystemSession(), request);
            if (!result.isResolved()) {
                if (result.getExceptions() != null && !result.getExceptions().isEmpty()) {
                    result.getExceptions().forEach(ex -> LOG.error("Exception from 'resolveArtifact(..)': ", ex));
                }
                throw new CoreException("Failed to resolve: " + artifact);
            }
            final MavenModelReader mmr = new MavenModelReader(context);
            final ModelResponse response = mmr.readModel(ModelRequest.builder().setArtifact(result.getArtifact()).build());
            LOG.info("Resolved artifact: {}:{}:{}", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
            return response.getEffectiveModel();
        } catch (final ArtifactResolutionException | VersionResolutionException | ArtifactDescriptorException ex) {
            throw new CoreException("Exception resolving: " + artifact, ex);
        }
    }

}
