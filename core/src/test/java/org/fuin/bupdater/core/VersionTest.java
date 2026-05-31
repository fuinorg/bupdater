package org.fuin.bupdater.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the {@link Version} class.
 */
class VersionTest {

    @Test
    void string_constructor_keeps_id_and_coerces_semver() {
        final Version version = new Version("1.2.3");

        assertThat(version.id()).isEqualTo("1.2.3");
        assertThat(version.semver()).isEqualTo(new Semver(1, 2, 3, 0, 0, null));
    }

    @Test
    void string_constructor_keeps_pre_release_part() {
        final Version version = new Version("1.2.3-SNAPSHOT");

        assertThat(version.id()).isEqualTo("1.2.3-SNAPSHOT");
        assertThat(version.semver()).isEqualTo(new Semver(1, 2, 3, 0, 0, "SNAPSHOT"));
    }

    @Test
    void string_constructor_sets_null_semver_for_non_numeric_id() {
        final Version version = new Version("abc");

        assertThat(version.id()).isEqualTo("abc");
        assertThat(version.semver()).isNull();
    }

    @Test
    void is_stable_returns_true_when_semver_is_missing() {
        assertThat(new Version("abc").isStable()).isTrue();
    }

    @Test
    void is_stable_returns_true_for_release_semver() {
        assertThat(new Version("1.2.3").isStable()).isTrue();
    }

    @Test
    void is_stable_returns_false_for_pre_release_semver() {
        assertThat(new Version("1.2.3-SNAPSHOT").isStable()).isFalse();
    }

    @Test
    void compare_to_uses_semantic_order_when_both_have_semver() {
        // String order would put "1.10.0" before "1.2.0"; semantic order must not.
        final Version lower = new Version("1.2.0");
        final Version higher = new Version("1.10.0");

        assertThat(lower.compareTo(higher)).isNegative();
        assertThat(higher.compareTo(lower)).isPositive();
        assertThat(lower.compareTo(new Version("1.2.0"))).isZero();
    }

    @Test
    void compare_to_orders_pre_release_below_release() {
        final Version snapshot = new Version("1.0.0-SNAPSHOT");
        final Version release = new Version("1.0.0");

        assertThat(snapshot.compareTo(release)).isNegative();
        assertThat(release.compareTo(snapshot)).isPositive();
    }

    @Test
    void compare_to_falls_back_to_id_when_a_semver_is_missing() {
        final Version textA = new Version("abc");
        final Version textB = new Version("abd");

        assertThat(textA.compareTo(textB)).isNegative();
        assertThat(textB.compareTo(textA)).isPositive();
        assertThat(textA.compareTo(new Version("abc"))).isZero();
    }

    @Test
    void compare_to_falls_back_to_id_when_only_one_semver_is_missing() {
        final Version text = new Version("abc");
        final Version numeric = new Version("1.0.0");

        // Compared as plain ids: 'a' (97) is greater than '1' (49).
        assertThat(text.compareTo(numeric)).isPositive();
        assertThat(numeric.compareTo(text)).isNegative();
    }

    @Test
    void compare_to_uses_supplied_semver_independent_of_id() {
        final Version a = new Version("z", new Semver(1, 0, 0, 0, 0, null));
        final Version b = new Version("a", new Semver(2, 0, 0, 0, 0, null));

        assertThat(a.compareTo(b)).isNegative();
    }

    @Test
    void to_string_returns_the_id() {
        assertThat(new Version("1.0.0")).hasToString("1.0.0");
        assertThat(new Version("not-a-version")).hasToString("not-a-version");
    }

    @Test
    void coerce_creates_semver_for_parseable_id() {
        final Version version = Version.coerce("4.0.6");

        assertThat(version.id()).isEqualTo("4.0.6");
        assertThat(version.semver()).isEqualTo(new Semver(4, 0, 6, 0, 0, null));
    }

    @Test
    void coerce_sets_null_semver_for_non_parseable_id() {
        final Version version = Version.coerce("abc");

        assertThat(version.id()).isEqualTo("abc");
        assertThat(version.semver()).isNull();
    }

    @Test
    void records_with_same_id_and_semver_are_equal() {
        assertThat(new Version("1.2.3")).isEqualTo(new Version("1.2.3"));
        assertThat(new Version("1.2.3")).hasSameHashCodeAs(new Version("1.2.3"));
    }

}
