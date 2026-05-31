package org.fuin.bupdater.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the pure {@link MimaBomResolver#findLatestStable(List)} logic. The repository access of
 * {@link MimaBomResolver} itself is covered by the plugin integration tests.
 */
class MimaBomResolverTest {

    @Test
    void returns_the_highest_version_when_all_are_stable() {
        assertThat(MimaBomResolver.findLatestStable(List.of("1.0.0", "1.1.0", "2.0.0")))
                .contains(new Version("2.0.0"));
    }

    @Test
    void skips_a_trailing_pre_release() {
        assertThat(MimaBomResolver.findLatestStable(List.of("1.0.0", "2.0.0", "2.1.0-SNAPSHOT")))
                .contains(new Version("2.0.0"));
    }

    @Test
    void skips_several_trailing_pre_releases() {
        assertThat(MimaBomResolver.findLatestStable(List.of("1.0.0", "2.0.0-RC1", "2.0.0-SNAPSHOT")))
                .contains(new Version("1.0.0"));
    }

    @Test
    void is_empty_when_all_versions_are_pre_releases() {
        assertThat(MimaBomResolver.findLatestStable(List.of("1.0.0-SNAPSHOT", "2.0.0-SNAPSHOT"))).isEmpty();
    }

    @Test
    void is_empty_for_no_versions() {
        assertThat(MimaBomResolver.findLatestStable(List.of())).isEmpty();
    }

}
