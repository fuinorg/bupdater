package org.fuin.bupdater.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for the {@link MavenGAV} class.
 */
class MavenGAVTest {

    @Test
    void record_components_are_exposed_via_accessors() {
        final MavenGA ga = new MavenGA("org.example", "lib");
        final Version version = new Version("1.2.3");
        final MavenGAV gav = new MavenGAV(ga, version);

        assertThat(gav.ga()).isSameAs(ga);
        assertThat(gav.version()).isEqualTo(version);
    }

    @Test
    void convenience_constructor_builds_ga_from_group_and_artifact() {
        final Version version = new Version("1.2.3");
        final MavenGAV gav = new MavenGAV("org.example", "lib", version);

        assertThat(gav.ga()).isEqualTo(new MavenGA("org.example", "lib"));
        assertThat(gav.version()).isEqualTo(version);
    }

    @Test
    void of_parses_group_artifact_and_version() {
        final MavenGAV gav = MavenGAV.of("org.example:lib:1.2.3");

        assertThat(gav.ga()).isEqualTo(new MavenGA("org.example", "lib"));
        assertThat(gav.version()).isEqualTo(new Version("1.2.3"));
    }

    @Test
    void of_delegates_group_and_artifact_parsing_to_MavenGA() {
        final MavenGAV gav = MavenGAV.of("^org\\.example$:^lib-.*$:1.0.0");

        assertThat(gav.ga().groupId()).isEqualTo("^org\\.example$");
        assertThat(gav.ga().groupRegExpr()).isTrue();
        assertThat(gav.ga().artifactId()).isEqualTo("^lib-.*$");
        assertThat(gav.ga().artifactRegExpr()).isTrue();
        assertThat(gav.version()).isEqualTo(new Version("1.0.0"));
    }

    @Test
    void of_rejects_string_with_only_one_segment() {
        assertThatThrownBy(() -> MavenGAV.of("org.example"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Maven GAV")
                .hasMessageContaining("org.example");
    }

    @Test
    void of_rejects_string_with_only_two_segments() {
        assertThatThrownBy(() -> MavenGAV.of("org.example:lib"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Maven GAV");
    }

    @Test
    void of_rejects_string_with_more_than_three_segments() {
        assertThatThrownBy(() -> MavenGAV.of("org.example:lib:1.0.0:extra"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Maven GAV");
    }

    @Test
    void of_rejects_empty_string() {
        assertThatThrownBy(() -> MavenGAV.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Maven GAV");
    }

    @Test
    void equals_and_hashCode_use_ga_and_version() {
        final Version v1 = new Version("1.0.0");
        final Version v2 = new Version("2.0.0");
        final MavenGAV a = new MavenGAV(new MavenGA("org.example", "lib"), v1);
        final MavenGAV same = new MavenGAV(new MavenGA("org.example", "lib"), v1);
        final MavenGAV otherVersion = new MavenGAV(new MavenGA("org.example", "lib"), v2);
        final MavenGAV otherGa = new MavenGAV(new MavenGA("org.example", "other"), v1);

        assertThat(a).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(a).isNotEqualTo(otherVersion);
        assertThat(a).isNotEqualTo(otherGa);
    }

}
