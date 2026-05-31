package org.fuin.bupdater.core;

import org.fuin.utils4j.Utils4J;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes the list of dependencies from the frameworks to an HTML file. The HTML generation
 * ({@link #toHtml(List, List)}) is separated from any repository access and file I/O so it can be
 * tested with plain data.
 */
public class HtmlPrinter {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlPrinter.class);

    /**
     * Writes the dependencies of an updater to an HTML file.
     *
     * @param updater    Updater providing the frameworks and harmonized versions.
     * @param type       Selects whether all dependencies or only the failed ones are written.
     * @param targetFile File to write the HTML to.
     */
    public void write(BomUpdater updater, PrintType type, File targetFile) {
        final List<MavenGAV> frameworks = updater.getFrameworks();
        final List<BomVersions> rows =
                (type == PrintType.ALL ? updater.getBomVersions() : updater.getFailed()).toList();
        write(targetFile, frameworks, rows);
    }

    /**
     * Writes the given dependency rows to an HTML file.
     *
     * @param targetFile File to write the HTML to.
     * @param frameworks Frameworks shown as columns.
     * @param rows       Dependency rows to render.
     */
    public void write(File targetFile, List<MavenGAV> frameworks, List<BomVersions> rows) {
        LOG.info("Writing HTML file to {}", targetFile.getAbsolutePath());
        try (final FileWriter fw = new FileWriter(targetFile)) {
            fw.write(toHtml(frameworks, rows));
        } catch (final IOException ex) {
            throw new CoreException("Failed to write: " + targetFile, ex);
        }
    }

    /**
     * Renders the dependency table as an HTML document. Pure function without any I/O.
     *
     * @param frameworks Frameworks shown as columns.
     * @param rows       Dependency rows to render.
     *
     * @return Complete HTML document.
     */
    String toHtml(List<MavenGAV> frameworks, List<BomVersions> rows) {
        final String tdNthChilds = createTdNthChilds(frameworks);
        final String thFrameworks = createThFrameworks(frameworks);

        final StringBuilder sb = new StringBuilder();
        sb.append(Utils4J.replaceVars("""
                <html>
                <head>
                <title>Managed Dependencies</title>
                <style>
                table, th, td {
                   border: 1px solid black;
                   border-collapse: collapse;
                }
                th, td {
                  padding: 1em;
                }
                table th:nth-child(1), table th:nth-child(2)  {
                  text-align: left;
                }
                table ${tdNthChilds} {
                  text-align: center;
                }
                .warning {
                  color: red;
                }
                </style>
                </head>
                <body>
                <h1>Managed Dependencies</h1>
                <table>
                  <tr><th>Group</th><th>Artifact</th>${thFrameworks}<th>Harmonized</th></tr>
                """, Map.of("thFrameworks", thFrameworks, "tdNthChilds", tdNthChilds)));

        for (final BomVersions bomVersions : rows) {
            final Map<String, String> vars = new HashMap<>();
            vars.put("group", bomVersions.artifact().groupId());
            vars.put("artifact", bomVersions.artifact().artifactId());
            vars.put("tdFrameworks", createTdFrameworks(frameworks, bomVersions));
            vars.put("expected", bomVersions.harmonized().map(Version::toString).orElse("-"));
            vars.put("expected-warn-class", warnClass(bomVersions));
            vars.put("expected-warn-icon", warnIcon(bomVersions));
            sb.append(Utils4J.replaceVars("""
                    <tr>
                    <td>${group}</td><td>${artifact}</td>${tdFrameworks}<td${expected-warn-class}>${expected}${expected-warn-icon}</td>
                    </tr>
                    """, vars));
        }

        sb.append("""
                </table>
                </body>
                </html>
                """);
        return sb.toString();
    }

    private static String createTdFrameworks(List<MavenGAV> frameworks, BomVersions bomVersions) {
        final StringBuilder sb = new StringBuilder();
        for (final MavenGAV mavenGAV : frameworks) {
            final String version = bomVersions.get(mavenGAV.ga()).map(Version::toString).orElse("-");
            sb.append("<td>").append(version).append("</td>");
        }
        return sb.toString();
    }

    private static String createTdNthChilds(List<MavenGAV> frameworks) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < frameworks.size(); i++) {
            sb.append("td:nth-child(").append(i + 3).append(")").append(", ");
        }
        sb.append("td:nth-child(").append(frameworks.size() + 3).append(")");
        return sb.toString();
    }

    private static String createThFrameworks(List<MavenGAV> frameworks) {
        final StringBuilder sb = new StringBuilder();
        for (final MavenGAV mavenGAV : frameworks) {
            sb.append("<th>")
                    .append(mavenGAV.ga().artifactId())
                    .append(" ")
                    .append(mavenGAV.version())
                    .append("</th>");
        }
        return sb.toString();
    }

    private static String warnIcon(BomVersions bomVersions) {
        if (bomVersions.isMajorDiff()) {
            return " (!)";
        }
        return "";
    }

    private static String warnClass(BomVersions bomVersions) {
        if (bomVersions.isMajorDiff()) {
            return " class=\"warning\"";
        }
        return "";
    }

}
