package org.fuin.bupdater.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.bupdater.core.FakeBomResolver.dependency;

/**
 * Test for the diff and decision API of {@link BomUpdater} (the part the Maven plugin and the
 * command line app rely on to decide success/failure and what to report).
 */
class BomUpdaterTest {

    private static final MavenGA JACKSON = new MavenGA("com.fasterxml.jackson.core", "jackson-annotations");

    private static final MavenGA FLYWAY = new MavenGA("org.flywaydb", "flyway-core");

    private static final List<String> INCLUDES =
            List.of("com.fasterxml.jackson.core:^.*$", "org.flywaydb:^.*$");

    /**
     * Two frameworks where jackson differs only by patch (minor diff) and flyway differs by major
     * version (a conflict).
     */
    private static FakeBomResolver conflictingFrameworks() {
        return new FakeBomResolver()
                .registerBom("io.quarkus", "quarkus-bom", "3.24.4",
                        dependency("com.fasterxml.jackson.core", "jackson-annotations", "2.18.2", null, null),
                        dependency("org.flywaydb", "flyway-core", "11.3.4", null, null))
                .registerBom("org.springframework.boot", "spring-boot-dependencies", "3.5.3",
                        dependency("com.fasterxml.jackson.core", "jackson-annotations", "2.18.3", null, null),
                        dependency("org.flywaydb", "flyway-core", "10.20.1", null, null))
                .registerArtifact("com.fasterxml.jackson.core", "jackson-annotations", "2.18.2")
                .registerArtifact("com.fasterxml.jackson.core", "jackson-annotations", "2.18.3")
                .registerArtifact("org.flywaydb", "flyway-core", "11.3.4")
                .registerArtifact("org.flywaydb", "flyway-core", "10.20.1");
    }

    private static BomUpdater updater(FakeBomResolver resolver, String... fixedVersions) {
        return new BomUpdater.Builder(resolver, INCLUDES)
                .addFixedVersions(List.of(fixedVersions))
                .addBom("io.quarkus", "quarkus-bom", "3.24.4")
                .addBom("org.springframework.boot", "spring-boot-dependencies", "3.5.3")
                .build();
    }

    @Test
    void is_failed_is_true_for_an_unresolved_major_conflict() {
        final BomUpdater updater = updater(conflictingFrameworks());

        assertThat(updater.isFailed()).isTrue();
    }

    @Test
    void get_failed_contains_only_the_unresolved_major_conflict() {
        final BomUpdater updater = updater(conflictingFrameworks());

        assertThat(updater.getFailed().map(BomVersions::artifact)).containsExactly(FLYWAY);
        assertThat(updater.getFailedAsString()).contains(FLYWAY.toString());
    }

    @Test
    void major_diffs_contain_the_conflict_but_not_the_minor_diff() {
        final BomUpdater updater = updater(conflictingFrameworks());

        assertThat(updater.getMajorDiffs().map(BomVersions::artifact)).containsExactly(FLYWAY);
    }

    @Test
    void any_diffs_contain_both_the_minor_and_the_major_difference() {
        final BomUpdater updater = updater(conflictingFrameworks());

        assertThat(updater.getAnyDiffs().map(BomVersions::artifact))
                .containsExactlyInAnyOrder(JACKSON, FLYWAY);
        assertThat(updater.getAnyAsString()).contains(JACKSON.toString()).contains(FLYWAY.toString());
    }

    @Test
    void a_matching_fixed_version_resolves_the_conflict() {
        final BomUpdater updater = updater(conflictingFrameworks(), "org.flywaydb:flyway-core:11.3.4");

        assertThat(updater.isFailed()).isFalse();
        assertThat(updater.getFailed()).isEmpty();
        assertThat(updater.bomVersionsOf(FLYWAY).orElseThrow().harmonized()).contains(new Version("11.3.4"));
    }

    @Test
    void frameworks_are_returned_sorted() {
        final BomUpdater updater = updater(conflictingFrameworks());

        assertThat(updater.getFrameworks()).containsExactly(
                MavenGAV.of("io.quarkus:quarkus-bom:3.24.4"),
                MavenGAV.of("org.springframework.boot:spring-boot-dependencies:3.5.3"));
    }

    @Test
    void no_differences_means_not_failed_and_empty_diffs() {
        final FakeBomResolver resolver = new FakeBomResolver()
                .registerBom("io.quarkus", "quarkus-bom", "3.24.4",
                        dependency("com.fasterxml.jackson.core", "jackson-annotations", "2.18.3", null, null))
                .registerBom("org.springframework.boot", "spring-boot-dependencies", "3.5.3",
                        dependency("com.fasterxml.jackson.core", "jackson-annotations", "2.18.3", null, null))
                .registerArtifact("com.fasterxml.jackson.core", "jackson-annotations", "2.18.3");

        final BomUpdater updater = updater(resolver);

        assertThat(updater.isFailed()).isFalse();
        assertThat(updater.getAnyDiffs()).isEmpty();
        assertThat(updater.getMajorDiffs()).isEmpty();
        assertThat(updater.getAnyAsString()).isEmpty();
    }

}
