package org.fuin.bupdater.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for the {@link BomVersions} class.
 */
class BomVersionsTest {

    private static final MavenGA ARTIFACT = new MavenGA("com.fasterxml.jackson.core", "jackson-annotations");

    private static final MavenGA QUARKUS = new MavenGA("io.quarkus", "quarkus-bom");

    private static final MavenGA SPRING = new MavenGA("org.springframework.boot", "spring-boot-dependencies");

    private static Map<MavenGA, Version> versions(String quarkus, String spring) {
        final Map<MavenGA, Version> map = new LinkedHashMap<>();
        map.put(QUARKUS, new Version(quarkus));
        map.put(SPRING, new Version(spring));
        return map;
    }

    private static BomVersions bom(Map<MavenGA, Version> versions, Strategy strategy, MavenGAV... fixed) {
        return new BomVersions(ARTIFACT, versions, List.of(fixed), strategy);
    }

    @Test
    void harmonized_picks_the_highest_version_for_the_highest_strategy() {
        final BomVersions bom = bom(versions("2.18.2", "2.18.3"), Strategy.HIGHEST);

        assertThat(bom.harmonized()).contains(new Version("2.18.3"));
    }

    @Test
    void harmonized_picks_the_lowest_version_for_the_lowest_strategy() {
        final BomVersions bom = bom(versions("2.18.2", "2.18.3"), Strategy.LOWEST);

        assertThat(bom.harmonized()).contains(new Version("2.18.2"));
    }

    @Test
    void harmonized_prefers_a_matching_fixed_version_over_the_strategy() {
        final BomVersions bom = bom(versions("2.18.2", "2.18.3"), Strategy.HIGHEST,
                MavenGAV.of("com.fasterxml.jackson.core:jackson-annotations:2.18.5"));

        assertThat(bom.harmonized()).contains(new Version("2.18.5"));
    }

    @Test
    void is_failed_is_true_when_no_fixed_version_matches() {
        final BomVersions bom = bom(versions("10.20.1", "11.3.4"), Strategy.HIGHEST);

        assertThat(bom.isFailed()).isTrue();
    }

    @Test
    void is_failed_is_false_when_a_fixed_version_matches() {
        final BomVersions bom = bom(versions("10.20.1", "11.3.4"), Strategy.HIGHEST,
                MavenGAV.of("com.fasterxml.jackson.core:jackson-annotations:10.20.1"));

        assertThat(bom.isFailed()).isFalse();
    }

    @Test
    void is_major_diff_is_true_when_majors_differ() {
        assertThat(bom(versions("10.20.1", "11.3.4"), Strategy.HIGHEST).isMajorDiff()).isTrue();
    }

    @Test
    void is_major_diff_is_false_when_only_minor_or_patch_differ() {
        assertThat(bom(versions("2.18.2", "2.18.3"), Strategy.HIGHEST).isMajorDiff()).isFalse();
    }

    @Test
    void is_any_diff_is_true_when_versions_differ() {
        assertThat(bom(versions("2.18.2", "2.18.3"), Strategy.HIGHEST).isAnyDiff()).isTrue();
    }

    @Test
    void is_any_diff_is_false_when_all_versions_are_equal() {
        assertThat(bom(versions("2.18.3", "2.18.3"), Strategy.HIGHEST).isAnyDiff()).isFalse();
    }

    @Test
    void get_returns_the_version_of_a_specific_source_bom() {
        final BomVersions bom = bom(versions("2.18.2", "2.18.3"), Strategy.HIGHEST);

        assertThat(bom.get(QUARKUS)).contains(new Version("2.18.2"));
        assertThat(bom.get(SPRING)).contains(new Version("2.18.3"));
        assertThat(bom.get(new MavenGA("org.unknown", "unknown"))).isEmpty();
    }

    @Test
    void major_diff_text_describes_the_conflict() {
        final BomVersions bom = bom(versions("10.20.1", "11.3.4"), Strategy.HIGHEST);

        assertThat(bom.getMajorDiffText()).contains(ARTIFACT.toString());
    }

    @Test
    void diff_texts_are_empty_when_there_is_no_diff() {
        final BomVersions bom = bom(versions("2.18.3", "2.18.3"), Strategy.HIGHEST);

        assertThat(bom.getMajorDiffText()).isEmpty();
        assertThat(bom.getAnyDiffText()).isEmpty();
    }

    @Test
    void empty_convenience_constructor_has_no_versions_and_highest_strategy() {
        final BomVersions bom = new BomVersions(ARTIFACT);

        assertThat(bom.versions()).isEmpty();
        assertThat(bom.fixedVersions()).isEmpty();
        assertThat(bom.strategy()).isEqualTo(Strategy.HIGHEST);
        assertThat(bom.harmonized()).isEmpty();
    }

    @Test
    void is_major_diff_uses_id_comparison_when_versions_are_not_semver() {
        // Spring Cloud style release-train names do not coerce to a semantic version.
        assertThat(new Version("Camden").semver()).isNull();
        final Map<MavenGA, Version> map = new LinkedHashMap<>();
        map.put(QUARKUS, new Version("Camden"));
        map.put(SPRING, new Version("Dalston"));

        assertThat(new BomVersions(ARTIFACT, map, List.of(), Strategy.HIGHEST).isMajorDiff()).isTrue();
    }

    @Test
    void is_major_diff_is_false_for_equal_non_semver_versions() {
        final Map<MavenGA, Version> map = new LinkedHashMap<>();
        map.put(QUARKUS, new Version("Dalston"));
        map.put(SPRING, new Version("Dalston"));

        assertThat(new BomVersions(ARTIFACT, map, List.of(), Strategy.HIGHEST).isMajorDiff()).isFalse();
    }

    @Test
    void is_any_diff_detects_a_difference_between_non_semver_versions() {
        final Map<MavenGA, Version> map = new LinkedHashMap<>();
        map.put(QUARKUS, new Version("Camden"));
        map.put(SPRING, new Version("Dalston"));

        assertThat(new BomVersions(ARTIFACT, map, List.of(), Strategy.HIGHEST).isAnyDiff()).isTrue();
    }

    @Test
    void constructor_rejects_an_artifact_with_a_regular_expression() {
        final MavenGA regex = MavenGA.of("com.example:^.*$");

        assertThatThrownBy(() -> new BomVersions(regex))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("regular expression");
    }

    @Test
    void constructor_rejects_versions_keyed_by_the_artifact_itself() {
        final Map<MavenGA, Version> map = new LinkedHashMap<>();
        map.put(ARTIFACT, new Version("1.0.0"));

        assertThatThrownBy(() -> new BomVersions(ARTIFACT, map, List.of(), Strategy.HIGHEST))
                .isInstanceOf(IllegalStateException.class);
    }

}
