package org.fuin.bupdater.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for the {@link PomDependenciesWriter} class.
 */
class PomDependenciesWriterTest {

    private static final MavenGA JACKSON = new MavenGA("com.fasterxml.jackson.core", "jackson-annotations");

    private static final MavenGA QUARKUS = new MavenGA("io.quarkus", "quarkus-bom");

    private static List<String> pomLines(String indent) {
        return List.of(
                "<project>",
                "    <dependencyManagement>",
                "        <dependencies>",
                indent + PomDependenciesWriter.BEGIN_DEPENDENCIES,
                indent + "<dependency>OLD CONTENT TO BE REPLACED</dependency>",
                indent + PomDependenciesWriter.END_DEPENDENCIES,
                "        </dependencies>",
                "    </dependencyManagement>",
                "</project>");
    }

    private static BomVersions dependency(String version) {
        final Map<MavenGA, Version> versions = new LinkedHashMap<>();
        versions.put(QUARKUS, new Version(version));
        return new BomVersions(JACKSON, versions, List.of(), Strategy.HIGHEST);
    }

    @Test
    void rewrite_replaces_only_the_content_between_the_markers() {
        final String result = new PomDependenciesWriter().rewrite("test", pomLines("            "),
                List.of(MavenGAV.of("io.quarkus:quarkus-bom:3.24.4")),
                List.of(dependency("2.18.3")));

        assertThat(result)
                .contains("<project>")
                .contains("        </dependencies>")
                .contains("</project>")
                .doesNotContain("OLD CONTENT TO BE REPLACED")
                .contains("<groupId>com.fasterxml.jackson.core</groupId>")
                .contains("<artifactId>jackson-annotations</artifactId>")
                .contains("<version>2.18.3</version>")
                .contains("Frameworks used to align the dependencies")
                .contains("io.quarkus:quarkus-bom:3.24.4");
    }

    @Test
    void rewrite_keeps_the_indentation_of_the_begin_marker() {
        final String result = new PomDependenciesWriter().rewrite("test", pomLines("            "),
                List.of(MavenGAV.of("io.quarkus:quarkus-bom:3.24.4")),
                List.of(dependency("2.18.3")));

        // The begin marker is indented with 12 spaces, so the generated dependency uses the same indent.
        assertThat(result).contains("            <dependency>");
    }

    @Test
    void rewrite_renders_external_only_versions_with_a_hint() {
        final Map<MavenGA, Version> versions = new LinkedHashMap<>();
        versions.put(BomVersions.EXTERNAL_SOURCE, new Version("9.9.9"));
        final BomVersions external = new BomVersions(JACKSON, versions, List.of(), Strategy.HIGHEST);

        final String result = new PomDependenciesWriter().rewrite("test", pomLines("    "),
                List.of(), List.of(external));

        assertThat(result).contains("<!-- Not in framework BOM -->");
    }

    @Test
    void rewrite_fails_when_the_begin_marker_is_missing() {
        final List<String> lines = List.of("<project>", PomDependenciesWriter.END_DEPENDENCIES, "</project>");

        assertThatThrownBy(() -> new PomDependenciesWriter().rewrite("missing-begin", lines, List.of(),
                List.of(dependency("2.18.3"))))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining(PomDependenciesWriter.BEGIN_DEPENDENCIES)
                .hasMessageContaining("missing-begin");
    }

    @Test
    void rewrite_fails_when_the_end_marker_is_missing() {
        final List<String> lines = List.of("<project>", PomDependenciesWriter.BEGIN_DEPENDENCIES, "</project>");

        assertThatThrownBy(() -> new PomDependenciesWriter().rewrite("missing-end", lines, List.of(),
                List.of(dependency("2.18.3"))))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining(PomDependenciesWriter.END_DEPENDENCIES);
    }

    @Test
    void write_round_trips_through_a_file(@TempDir Path dir) throws IOException {
        final File pom = dir.resolve("pom.xml").toFile();
        Files.write(pom.toPath(), String.join(System.lineSeparator(), pomLines("            "))
                .getBytes(StandardCharsets.UTF_8));

        new PomDependenciesWriter().write(pom, List.of(MavenGAV.of("io.quarkus:quarkus-bom:3.24.4")),
                List.of(dependency("2.18.3")));

        final String written = Files.readString(pom.toPath(), StandardCharsets.UTF_8);
        assertThat(written)
                .doesNotContain("OLD CONTENT TO BE REPLACED")
                .contains("<version>2.18.3</version>")
                .contains(PomDependenciesWriter.BEGIN_DEPENDENCIES)
                .contains(PomDependenciesWriter.END_DEPENDENCIES);
    }

}
