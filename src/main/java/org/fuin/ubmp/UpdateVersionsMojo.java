package org.fuin.ubmp;

import com.vdurmont.semver4j.Semver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.version.Version;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

@Mojo(name = "update",
        defaultPhase = LifecyclePhase.INITIALIZE,
        requiresProject = true,
        threadSafe = true)
public class UpdateVersionsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    RemoteRepositoryManager remoteRepositoryManager;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> projectRepos;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(property = "strategy", required = false, defaultValue = "HIGHEST")
    Strategy strategy;

    @Parameter(property = "frameworks", required = true)
    List<String> frameworks;

    @Parameter(property = "fixed-versions", required = true)
    List<String> fixedVersions;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // frameworks.stream().map(Arti::parse).map()

    }

    private Optional<String> stable(List<Semver> versions) {
        return versions.stream().filter(Semver::isStable).max(Semver::compareTo).map(Semver::toString);
    }

    private Optional<String> latest(List<Semver> versions) {
        return versions.stream().max(Semver::compareTo).map(Semver::toString);
    }

    private Artifact resolveArtifact(Arti arti) throws MojoExecutionException {
        final ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new DefaultArtifact(arti.groupId(), arti.artifactId(), null, null, arti.version()));
        request.setRepositories(projectRepos);
        try {
            ArtifactResult result = repositorySystem.resolveArtifact(repoSession, request);
            if (!result.isResolved()) {
                if (result.getExceptions() != null && !result.getExceptions().isEmpty()) {
                    result.getExceptions().forEach(ex -> getLog().error("Exception from 'resolveArtifact(..)': ", ex));
                }
                throw new MojoExecutionException("Failed to resolve: " + arti);
            }

        } catch (final ArtifactResolutionException ex) {
            throw new MojoExecutionException("Exception resolving: " + arti, ex);
        }

    }

    private List<Semver> getVersions(Arti arti) throws MojoExecutionException {
        final VersionRangeRequest rangeRequest = new VersionRangeRequest();
        final Artifact artifact = arti.toAllVersionsArtifact();
        rangeRequest.setArtifact(artifact);
        rangeRequest.setRepositories(projectRepos);
        try {
            final List<Version> versions = repositorySystem.resolveVersionRange(repoSession, rangeRequest).getVersions();
            final List<Semver> semvers = new ArrayList<>(versions.stream().map(version -> new Semver(version.toString(), Semver.SemverType.LOOSE)).toList());
            Collections.sort(semvers);
            return semvers;
        } catch (VersionRangeResolutionException ex) {
            throw new MojoExecutionException("Failed to resolve available versions for: " + arti, ex);
        }
    }

    private record Arti(String groupId, String artifactId, String version) {

        public Artifact toAllVersionsArtifact() {
            return new DefaultArtifact(groupId + ":" + artifactId + ":(0,]");
        }

        public static Arti parse(String str) {
            final StringTokenizer tok = new StringTokenizer(str, ":");
            if (tok.countTokens() != 2) {
                throw new IllegalArgumentException("Invalid artifact format: " + str);
            }
            final String groupId = tok.nextToken();
            final String artifactId = tok.nextToken();
            final String version = tok.nextToken();
            return new Arti(groupId, artifactId, version);
        }

    }

}
