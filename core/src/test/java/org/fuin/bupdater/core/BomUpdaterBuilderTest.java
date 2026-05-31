package org.fuin.bupdater.core;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.fuin.bupdater.core.FakeBomResolver.dependency;

/**
 * Test for the {@link BomUpdater.Builder} using a {@link FakeBomResolver}, so the BOM reading and
 * version-map building can be verified without accessing any repository.
 */
class BomUpdaterBuilderTest {

    private static final MavenGA QUARKUS = new MavenGA("io.quarkus", "quarkus-bom");

    private static final MavenGA JACKSON_ANN = new MavenGA("com.fasterxml.jackson.core", "jackson-annotations");

    private static final MavenGA JACKSON_DB = new MavenGA("com.fasterxml.jackson.core", "jackson-databind");

    private static final MavenGA SLF4J = new MavenGA("org.slf4j", "slf4j-api");

    private static final List<String> INCLUDES = List.of(
            "com.fasterxml.jackson.core:^.*$",
            "org.slf4j:^.*$",
            "com.example:^.*$",
            "io.nested:^.*$");

    @Test
    void reads_included_jar_dependencies_and_skips_the_rest() {
        final FakeBomResolver resolver = new FakeBomResolver()
                .registerBom("io.quarkus", "quarkus-bom", "3.24.4",
                        dependency("com.fasterxml.jackson.core", "jackson-annotations", "2.18.2", null, null),
                        dependency("org.slf4j", "slf4j-api", "2.0.17", null, null),
                        dependency("not.included", "other-lib", "1.0.0", null, null),
                        dependency("com.example", "native-lib", "1.0.0", "so", null),
                        dependency("com.example", "with-classifier", "1.0.0", null, "linux"))
                .registerArtifact("com.fasterxml.jackson.core", "jackson-annotations", "2.18.2")
                .registerArtifact("org.slf4j", "slf4j-api", "2.0.17");

        final BomUpdater updater = new BomUpdater.Builder(resolver, INCLUDES)
                .addBom("io.quarkus", "quarkus-bom", "3.24.4")
                .build();

        assertThat(updater.getArtifacts()).containsExactlyInAnyOrder(JACKSON_ANN, SLF4J);
        assertThat(updater.bomVersionsOf(JACKSON_ANN).orElseThrow().get(QUARKUS)).contains(new Version("2.18.2"));
        assertThat(updater.bomVersionsOf(SLF4J).orElseThrow().get(QUARKUS)).contains(new Version("2.0.17"));
        // not included, non-jar and classified dependencies are absent
        assertThat(updater.bomVersionsOf(new MavenGA("not.included", "other-lib"))).isEmpty();
        assertThat(updater.bomVersionsOf(new MavenGA("com.example", "native-lib"))).isEmpty();
        assertThat(updater.bomVersionsOf(new MavenGA("com.example", "with-classifier"))).isEmpty();
    }

    @Test
    void follows_nested_boms_and_attributes_versions_to_the_top_framework() {
        final FakeBomResolver resolver = new FakeBomResolver()
                .registerBom("io.quarkus", "quarkus-bom", "3.24.4",
                        dependency("io.nested", "nested-bom", "9.9.9", "pom", null))
                .registerBom("io.nested", "nested-bom", "9.9.9",
                        dependency("com.fasterxml.jackson.core", "jackson-databind", "2.18.2", null, null))
                .registerArtifact("com.fasterxml.jackson.core", "jackson-databind", "2.18.2");

        final BomUpdater updater = new BomUpdater.Builder(resolver, INCLUDES)
                .addBom("io.quarkus", "quarkus-bom", "3.24.4")
                .build();

        // The transitively found artifact is attributed to the top-level framework BOM (quarkus-bom).
        assertThat(updater.bomVersionsOf(JACKSON_DB).orElseThrow().get(QUARKUS)).contains(new Version("2.18.2"));
    }

    @Test
    void skips_dependencies_that_cannot_be_resolved() {
        final FakeBomResolver resolver = new FakeBomResolver()
                .registerBom("io.quarkus", "quarkus-bom", "3.24.4",
                        dependency("com.fasterxml.jackson.core", "jackson-annotations", "2.18.2", null, null));
        // jackson-annotations artifact is intentionally NOT registered -> existence check fails

        final BomUpdater updater = new BomUpdater.Builder(resolver, INCLUDES)
                .addBom("io.quarkus", "quarkus-bom", "3.24.4")
                .build();

        assertThat(updater.bomVersionsOf(JACKSON_ANN)).isEmpty();
    }

    @Test
    void resolves_concrete_includes_not_present_in_any_bom_from_the_repository() {
        final FakeBomResolver resolver = new FakeBomResolver()
                .registerBom("io.quarkus", "quarkus-bom", "3.24.4",
                        dependency("com.fasterxml.jackson.core", "jackson-annotations", "2.18.2", null, null))
                .registerArtifact("com.fasterxml.jackson.core", "jackson-annotations", "2.18.2")
                .registerLatestStable("com.external", "gson", "3.0.0");

        final List<String> includesWithExternal = List.of(
                "com.fasterxml.jackson.core:^.*$", "com.external:gson");
        final BomUpdater updater = new BomUpdater.Builder(resolver, includesWithExternal)
                .addBom("io.quarkus", "quarkus-bom", "3.24.4")
                .build();

        final MavenGA gson = new MavenGA("com.external", "gson");
        assertThat(updater.bomVersionsOf(gson).orElseThrow().get(BomVersions.EXTERNAL_SOURCE))
                .contains(new Version("3.0.0"));
    }

    @Test
    void build_fails_when_no_bom_was_added() {
        final BomUpdater.Builder builder = new BomUpdater.Builder(new FakeBomResolver(), INCLUDES);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No BOMs added");
    }

    // --- Builder public API surface ---------------------------------------------------------------

    @Test
    void add_bom_from_coordinate_string_registers_the_framework() {
        final FakeBomResolver resolver = new FakeBomResolver()
                .registerBom("io.quarkus", "quarkus-bom", "3.24.4");

        final BomUpdater updater = new BomUpdater.Builder(resolver, INCLUDES)
                .addBom("io.quarkus:quarkus-bom:3.24.4")
                .build();

        assertThat(updater.getFrameworks()).containsExactly(MavenGAV.of("io.quarkus:quarkus-bom:3.24.4"));
    }

    @Test
    void add_boms_from_a_list_of_coordinate_strings_registers_all_frameworks() {
        final FakeBomResolver resolver = new FakeBomResolver()
                .registerBom("io.quarkus", "quarkus-bom", "3.24.4")
                .registerBom("org.springframework.boot", "spring-boot-dependencies", "3.5.3");

        final BomUpdater updater = new BomUpdater.Builder(resolver, INCLUDES)
                .addBoms(List.of("io.quarkus:quarkus-bom:3.24.4",
                        "org.springframework.boot:spring-boot-dependencies:3.5.3"))
                .build();

        assertThat(updater.getFrameworks()).containsExactly(
                MavenGAV.of("io.quarkus:quarkus-bom:3.24.4"),
                MavenGAV.of("org.springframework.boot:spring-boot-dependencies:3.5.3"));
    }

    @Test
    void add_boms_from_a_model_reads_each_managed_dependency_as_a_framework() {
        final FakeBomResolver resolver = new FakeBomResolver()
                .registerBom("io.quarkus", "quarkus-bom", "3.24.4");
        final Model model = new Model();
        final DependencyManagement dm = new DependencyManagement();
        dm.addDependency(dependency("io.quarkus", "quarkus-bom", "3.24.4", "pom", null));
        model.setDependencyManagement(dm);

        final BomUpdater updater = new BomUpdater.Builder(resolver, INCLUDES)
                .addBoms(model)
                .build();

        assertThat(updater.getFrameworks()).containsExactly(MavenGAV.of("io.quarkus:quarkus-bom:3.24.4"));
    }

    @Test
    void add_boms_from_a_model_without_dependency_management_adds_nothing() {
        final BomUpdater.Builder builder = new BomUpdater.Builder(new FakeBomResolver(), INCLUDES)
                .addBoms(new Model());

        // No BOM was added through the empty model.
        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No BOMs added");
    }

    @Test
    void lowest_strategy_harmonizes_to_the_lowest_version() {
        final FakeBomResolver resolver = twoFrameworksWithJackson();

        final BomUpdater updater = new BomUpdater.Builder(resolver, List.of("com.fasterxml.jackson.core:^.*$"))
                .strategy(Strategy.LOWEST)
                .addBom("io.quarkus", "quarkus-bom", "3.24.4")
                .addBom("org.springframework.boot", "spring-boot-dependencies", "3.5.3")
                .build();

        assertThat(updater.bomVersionsOf(JACKSON_ANN).orElseThrow().harmonized()).contains(new Version("2.18.2"));
    }

    @Test
    void fixed_version_from_string_overrides_the_harmonized_result() {
        final FakeBomResolver resolver = twoFrameworksWithJackson();

        final BomUpdater updater = new BomUpdater.Builder(resolver, List.of("com.fasterxml.jackson.core:^.*$"))
                .addFixedVersion("com.fasterxml.jackson.core:jackson-annotations:2.18.9")
                .addBom("io.quarkus", "quarkus-bom", "3.24.4")
                .addBom("org.springframework.boot", "spring-boot-dependencies", "3.5.3")
                .build();

        assertThat(updater.bomVersionsOf(JACKSON_ANN).orElseThrow().harmonized()).contains(new Version("2.18.9"));
    }

    @Test
    void add_fixed_versions_ignores_null() {
        final FakeBomResolver resolver = new FakeBomResolver().registerBom("io.quarkus", "quarkus-bom", "3.24.4");

        final BomUpdater updater = new BomUpdater.Builder(resolver, INCLUDES)
                .addFixedVersions(null)
                .addBom("io.quarkus", "quarkus-bom", "3.24.4")
                .build();

        assertThat(updater.getFrameworks()).containsExactly(MavenGAV.of("io.quarkus:quarkus-bom:3.24.4"));
    }

    private static FakeBomResolver twoFrameworksWithJackson() {
        return new FakeBomResolver()
                .registerBom("io.quarkus", "quarkus-bom", "3.24.4",
                        dependency("com.fasterxml.jackson.core", "jackson-annotations", "2.18.2", null, null))
                .registerBom("org.springframework.boot", "spring-boot-dependencies", "3.5.3",
                        dependency("com.fasterxml.jackson.core", "jackson-annotations", "2.18.3", null, null))
                .registerArtifact("com.fasterxml.jackson.core", "jackson-annotations", "2.18.2")
                .registerArtifact("com.fasterxml.jackson.core", "jackson-annotations", "2.18.3");
    }

}
