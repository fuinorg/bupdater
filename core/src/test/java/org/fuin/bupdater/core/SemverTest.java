package org.fuin.bupdater.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the {@link Semver} class.
 */
class SemverTest {

    @Test
    void coerce_parses_single_major_segment() {
        assertThat(Semver.coerce("5")).contains(new Semver(5, 0, 0, 0, 0, null));
    }

    @Test
    void coerce_parses_major_and_minor() {
        assertThat(Semver.coerce("1.2")).contains(new Semver(1, 2, 0, 0, 0, null));
    }

    @Test
    void coerce_parses_major_minor_and_patch() {
        assertThat(Semver.coerce("1.2.3")).contains(new Semver(1, 2, 3, 0, 0, null));
    }

    @Test
    void coerce_parses_major_minor_patch_and_build() {
        assertThat(Semver.coerce("1.2.3.4")).contains(new Semver(1, 2, 3, 4, 0, null));
    }

    @Test
    void coerce_parses_all_five_numeric_segments() {
        assertThat(Semver.coerce("1.2.3.4.5")).contains(new Semver(1, 2, 3, 4, 5, null));
    }

    @Test
    void coerce_extracts_pre_release_suffix() {
        assertThat(Semver.coerce("1.2.3-SNAPSHOT")).contains(new Semver(1, 2, 3, 0, 0, "SNAPSHOT"));
    }

    @Test
    void coerce_keeps_everything_after_the_first_dash_as_pre_release() {
        assertThat(Semver.coerce("1.2.3-beta-1")).contains(new Semver(1, 2, 3, 0, 0, "beta-1"));
    }

    @Test
    void coerce_combines_pre_release_with_a_short_numeric_part() {
        assertThat(Semver.coerce("1-SNAPSHOT")).contains(new Semver(1, 0, 0, 0, 0, "SNAPSHOT"));
    }

    @Test
    void coerce_treats_final_build_keyword_as_zero() {
        assertThat(Semver.coerce("1.2.3.FINAL")).contains(new Semver(1, 2, 3, 0, 0, null));
    }

    @Test
    void coerce_treats_release_build_keyword_as_zero() {
        assertThat(Semver.coerce("1.2.3.RELEASE")).contains(new Semver(1, 2, 3, 0, 0, null));
    }

    @Test
    void coerce_treats_build_keyword_case_insensitively() {
        assertThat(Semver.coerce("1.2.3.final")).contains(new Semver(1, 2, 3, 0, 0, null));
    }

    @Test
    void coerce_returns_empty_for_non_numeric_segment() {
        assertThat(Semver.coerce("abc")).isEmpty();
    }

    @Test
    void coerce_returns_empty_for_non_numeric_minor() {
        assertThat(Semver.coerce("1.x")).isEmpty();
    }

    @Test
    void coerce_treats_a_non_numeric_build_segment_as_pre_release() {
        assertThat(Semver.coerce("7.0.0.Beta1")).contains(new Semver(7, 0, 0, 0, 0, "Beta1"));
        assertThat(Semver.coerce("7.0.0.Alpha1")).contains(new Semver(7, 0, 0, 0, 0, "Alpha1"));
        assertThat(Semver.coerce("7.0.0.Alpha2")).contains(new Semver(7, 0, 0, 0, 0, "Alpha2"));
    }

    @Test
    void coerce_keeps_a_dash_pre_release_over_a_qualifier_build_segment() {
        assertThat(Semver.coerce("1.2.3.GA")).contains(new Semver(1, 2, 3, 0, 0, "GA"));
    }

    @Test
    void qualifier_versions_are_not_stable() {
        assertThat(Semver.coerce("7.0.0.Beta1")).get().extracting(Semver::isStable).isEqualTo(false);
    }

    @Test
    void compare_to_orders_qualifier_pre_releases_below_the_final_release() {
        final Semver alpha1 = Semver.coerce("7.0.0.Alpha1").orElseThrow();
        final Semver alpha2 = Semver.coerce("7.0.0.Alpha2").orElseThrow();
        final Semver beta1 = Semver.coerce("7.0.0.Beta1").orElseThrow();
        final Semver release = Semver.coerce("7.0.0").orElseThrow();

        assertThat(alpha1).isLessThan(alpha2);
        assertThat(alpha2).isLessThan(beta1);
        assertThat(beta1).isLessThan(release);
    }

    @Test
    void coerce_returns_empty_for_empty_string() {
        assertThat(Semver.coerce("")).isEmpty();
    }

    @Test
    void coerce_returns_empty_for_too_many_segments() {
        assertThat(Semver.coerce("1.2.3.4.5.6")).isEmpty();
    }

    @Test
    void is_stable_is_true_without_a_pre_release() {
        assertThat(new Semver(1, 2, 3, 0, 0, null).isStable()).isTrue();
    }

    @Test
    void is_stable_is_false_with_a_pre_release() {
        assertThat(new Semver(1, 2, 3, 0, 0, "SNAPSHOT").isStable()).isFalse();
    }

    @Test
    void compare_to_orders_by_each_numeric_segment_in_turn() {
        assertThat(new Semver(2, 0, 0, 0, 0, null))
                .isGreaterThan(new Semver(1, 9, 9, 9, 9, null));
        assertThat(new Semver(1, 2, 0, 0, 0, null))
                .isGreaterThan(new Semver(1, 1, 9, 9, 9, null));
        assertThat(new Semver(1, 1, 2, 0, 0, null))
                .isGreaterThan(new Semver(1, 1, 1, 9, 9, null));
        assertThat(new Semver(1, 1, 1, 2, 0, null))
                .isGreaterThan(new Semver(1, 1, 1, 1, 9, null));
        assertThat(new Semver(1, 1, 1, 1, 2, null))
                .isGreaterThan(new Semver(1, 1, 1, 1, 1, null));
    }

    @Test
    void compare_to_treats_a_release_as_higher_than_a_pre_release() {
        final Semver release = new Semver(1, 0, 0, 0, 0, null);
        final Semver preRelease = new Semver(1, 0, 0, 0, 0, "SNAPSHOT");

        assertThat(release.compareTo(preRelease)).isPositive();
        assertThat(preRelease.compareTo(release)).isNegative();
    }

    @Test
    void compare_to_orders_pre_releases_lexicographically() {
        final Semver alpha = new Semver(1, 0, 0, 0, 0, "alpha");
        final Semver beta = new Semver(1, 0, 0, 0, 0, "beta");

        assertThat(alpha.compareTo(beta)).isNegative();
        assertThat(beta.compareTo(alpha)).isPositive();
    }

    @Test
    void compare_to_returns_zero_for_equal_semvers() {
        assertThat(new Semver(1, 2, 3, 4, 5, "x").compareTo(new Semver(1, 2, 3, 4, 5, "x"))).isZero();
        assertThat(new Semver(1, 2, 3, 0, 0, null).compareTo(new Semver(1, 2, 3, 0, 0, null))).isZero();
    }

}
