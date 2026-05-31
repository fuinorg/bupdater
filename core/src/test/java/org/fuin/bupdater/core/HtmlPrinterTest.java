package org.fuin.bupdater.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the {@link HtmlPrinter} class.
 */
class HtmlPrinterTest {

    private static final MavenGA JACKSON = new MavenGA("com.fasterxml.jackson.core", "jackson-annotations");

    private static final MavenGAV QUARKUS = MavenGAV.of("io.quarkus:quarkus-bom:3.24.4");

    private static final MavenGAV SPRING = MavenGAV.of("org.springframework.boot:spring-boot-dependencies:3.5.3");

    private static BomVersions row(MavenGA artifact, Map<MavenGA, Version> versions) {
        return new BomVersions(artifact, versions, List.of(), Strategy.HIGHEST);
    }

    private static Map<MavenGA, Version> versions(String quarkus, String spring) {
        final Map<MavenGA, Version> map = new LinkedHashMap<>();
        map.put(QUARKUS.ga(), new Version(quarkus));
        map.put(SPRING.ga(), new Version(spring));
        return map;
    }

    @Test
    void to_html_renders_a_column_header_per_framework() {
        final String html = new HtmlPrinter().toHtml(List.of(QUARKUS, SPRING), List.of());

        assertThat(html)
                .contains("<th>quarkus-bom 3.24.4</th>")
                .contains("<th>spring-boot-dependencies 3.5.3</th>")
                .contains("<th>Group</th>")
                .contains("<th>Harmonized</th>");
    }

    @Test
    void to_html_renders_a_row_with_group_artifact_and_each_framework_version() {
        final BomVersions row = row(JACKSON, versions("2.18.2", "2.18.3"));

        final String html = new HtmlPrinter().toHtml(List.of(QUARKUS, SPRING), List.of(row));

        assertThat(html)
                .contains("<td>com.fasterxml.jackson.core</td>")
                .contains("<td>jackson-annotations</td>")
                .contains("<td>2.18.2</td>")
                .contains("<td>2.18.3</td>")
                // harmonized (HIGHEST) without warning
                .contains(">2.18.3</td>")
                .doesNotContain("class=\"warning\"");
    }

    @Test
    void to_html_shows_a_dash_when_a_framework_does_not_define_the_version() {
        final Map<MavenGA, Version> onlyQuarkus = new LinkedHashMap<>();
        onlyQuarkus.put(QUARKUS.ga(), new Version("2.18.2"));

        final String html = new HtmlPrinter().toHtml(List.of(QUARKUS, SPRING), List.of(row(JACKSON, onlyQuarkus)));

        // The Spring column has no value -> rendered as "-"
        assertThat(html).contains("<td>-</td>");
    }

    @Test
    void to_html_marks_major_differences_as_a_warning() {
        final BomVersions row = row(JACKSON, versions("10.20.1", "11.3.4"));

        final String html = new HtmlPrinter().toHtml(List.of(QUARKUS, SPRING), List.of(row));

        assertThat(html)
                .contains("class=\"warning\"")
                .contains("(!)");
    }

    @Test
    void to_html_is_a_complete_document() {
        final String html = new HtmlPrinter().toHtml(List.of(QUARKUS), List.of());

        assertThat(html).startsWith("<html>");
        assertThat(html).contains("</table>").contains("</body>").contains("</html>");
    }

}
