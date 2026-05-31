package org.fuin.bupdater.core;

import org.apache.maven.model.Model;

/**
 * Resolves Maven artifacts and versions from the (remote) repositories. Implementations encapsulate
 * all repository access so that the BOM harmonization logic can be tested with a fake resolver.
 */
public interface BomResolver {

    /**
     * Resolves the effective POM model of the given artifact.
     *
     * @param gav Group, artifact and version to resolve.
     *
     * @return Effective model.
     *
     * @throws CoreException The artifact could not be resolved.
     */
    Model resolveModel(MavenGAV gav);

    /**
     * Resolves the latest stable version of an artifact available in the repositories.
     *
     * @param ga Group and artifact to resolve the version for.
     *
     * @return Latest stable version.
     *
     * @throws CoreException No version could be resolved.
     */
    Version resolveLatestStableVersion(MavenGA ga);

}
