package org.fuin.bupdater.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for the {@link MavenGA} class.
 */
class MavenGATest {

    @Test
    void convenience_constructor_sets_no_regular_expression_flags() {
        final MavenGA ga = new MavenGA("org.example", "lib");

        assertThat(ga.groupId()).isEqualTo("org.example");
        assertThat(ga.artifactId()).isEqualTo("lib");
        assertThat(ga.groupRegExpr()).isFalse();
        assertThat(ga.artifactRegExpr()).isFalse();
    }

    @Test
    void convenience_constructor_rejects_regular_expression_in_group_id() {
        assertThatThrownBy(() -> new MavenGA("^org\\.example$", "lib"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Regular expression not allowed");
    }

    @Test
    void convenience_constructor_rejects_regular_expression_in_artifact_id() {
        assertThatThrownBy(() -> new MavenGA("org.example", "^lib.*$"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Regular expression not allowed");
    }

    @Test
    void convenience_constructor_rejects_partial_regular_expression() {
        assertThatThrownBy(() -> new MavenGA("^org.example", "lib"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MavenGA("org.example", "lib$"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void to_string_returns_group_id_colon_artifact_id() {
        assertThat(new MavenGA("org.example", "lib")).hasToString("org.example:lib");
    }

    @Test
    void compare_to_orders_by_group_then_artifact() {
        final MavenGA a = new MavenGA("org.a", "lib");
        final MavenGA a_ = new MavenGA("org.a", "lib");
        final MavenGA b = new MavenGA("org.b", "lib");
        final MavenGA aZ = new MavenGA("org.a", "zlib");

        assertThat(a.compareTo(b)).isNegative();
        assertThat(b.compareTo(a)).isPositive();
        assertThat(a.compareTo(a)).isZero();
        assertThat(a.compareTo(a_)).isZero();
        assertThat(a.compareTo(aZ)).isNegative();
        assertThat(aZ.compareTo(a)).isPositive();
    }

    @Test
    void of_parses_plain_group_and_artifact() {
        final MavenGA ga = MavenGA.of("org.example:lib");

        assertThat(ga.groupId()).isEqualTo("org.example");
        assertThat(ga.artifactId()).isEqualTo("lib");
        assertThat(ga.groupRegExpr()).isFalse();
        assertThat(ga.artifactRegExpr()).isFalse();
    }

    @Test
    void of_parses_regular_expression_group_id() {
        final MavenGA ga = MavenGA.of("^org\\.example$:lib");

        assertThat(ga.groupId()).isEqualTo("^org\\.example$");
        assertThat(ga.groupRegExpr()).isTrue();
        assertThat(ga.artifactId()).isEqualTo("lib");
        assertThat(ga.artifactRegExpr()).isFalse();
    }

    @Test
    void of_parses_regular_expression_artifact_id() {
        final MavenGA ga = MavenGA.of("org.example:^lib-.*$");

        assertThat(ga.groupId()).isEqualTo("org.example");
        assertThat(ga.groupRegExpr()).isFalse();
        assertThat(ga.artifactId()).isEqualTo("^lib-.*$");
        assertThat(ga.artifactRegExpr()).isTrue();
    }

    @Test
    void of_uses_first_colon_as_separator() {
        final MavenGA ga = MavenGA.of("org.example:lib:extra");

        assertThat(ga.groupId()).isEqualTo("org.example");
        assertThat(ga.artifactId()).isEqualTo("lib:extra");
    }

    @Test
    void of_rejects_string_without_colon() {
        assertThatThrownBy(() -> MavenGA.of("org.example"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected format 'groupId:artifactId'");
    }

    @Test
    void of_rejects_empty_group_id() {
        assertThatThrownBy(() -> MavenGA.of(":lib"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected format 'groupId:artifactId'");
    }

    @Test
    void of_rejects_empty_artifact_id() {
        assertThatThrownBy(() -> MavenGA.of("org.example:"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected format 'artifactId'");
    }

    @Test
    void of_rejects_partial_regular_expression_in_group_id() {
        assertThatThrownBy(() -> MavenGA.of("^org.example:lib"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected regular expression format '^EXPR$'");
    }

    @Test
    void of_rejects_partial_regular_expression_in_artifact_id() {
        assertThatThrownBy(() -> MavenGA.of("org.example:lib$"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected regular expression format '^EXPR$'");
    }

    @Test
    void of_rejects_invalid_regular_expression() {
        assertThatThrownBy(() -> MavenGA.of("^org.example:^lib[$"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Expected regular expression");
    }

    @Test
    void similar_matches_when_group_and_artifact_are_equal() {
        final MavenGA pattern = new MavenGA("org.example", "lib");
        final MavenGA candidate = new MavenGA("org.example", "lib");

        assertThat(pattern.similar(candidate)).isTrue();
    }

    @Test
    void similar_does_not_match_when_group_id_differs() {
        final MavenGA pattern = new MavenGA("org.example", "lib");
        final MavenGA candidate = new MavenGA("org.other", "lib");

        assertThat(pattern.similar(candidate)).isFalse();
    }

    @Test
    void similar_does_not_match_when_artifact_id_differs() {
        final MavenGA pattern = new MavenGA("org.example", "lib");
        final MavenGA candidate = new MavenGA("org.example", "other");

        assertThat(pattern.similar(candidate)).isFalse();
    }

    @Test
    void similar_matches_group_regular_expression() {
        final MavenGA pattern = MavenGA.of("^org\\.example(\\..*)?$:lib");

        assertThat(pattern.similar(new MavenGA("org.example", "lib"))).isTrue();
        assertThat(pattern.similar(new MavenGA("org.example.sub", "lib"))).isTrue();
        assertThat(pattern.similar(new MavenGA("org.other", "lib"))).isFalse();
    }

    @Test
    void similar_matches_artifact_regular_expression() {
        final MavenGA pattern = MavenGA.of("org.example:^lib-.*$");

        assertThat(pattern.similar(new MavenGA("org.example", "lib-core"))).isTrue();
        assertThat(pattern.similar(new MavenGA("org.example", "lib-api"))).isTrue();
        assertThat(pattern.similar(new MavenGA("org.example", "other"))).isFalse();
    }

    @Test
    void similar_matches_when_both_group_and_artifact_are_regular_expressions() {
        final MavenGA pattern = MavenGA.of("^org\\..*$:^lib-.*$");

        assertThat(pattern.similar(new MavenGA("org.example", "lib-core"))).isTrue();
        assertThat(pattern.similar(new MavenGA("com.example", "lib-core"))).isFalse();
        assertThat(pattern.similar(new MavenGA("org.example", "other"))).isFalse();
    }

    @Test
    void similar_rejects_argument_that_contains_a_regular_expression() {
        final MavenGA pattern = new MavenGA("org.example", "lib");
        final MavenGA regExprArg = MavenGA.of("^org\\..*$:lib");

        assertThatThrownBy(() -> pattern.similar(regExprArg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed to contain regular expressions");
    }

}
