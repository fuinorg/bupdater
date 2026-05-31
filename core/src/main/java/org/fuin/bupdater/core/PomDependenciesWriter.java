package org.fuin.bupdater.core;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rewrites the managed dependencies of a POM file between the
 * {@value #BEGIN_DEPENDENCIES} and {@value #END_DEPENDENCIES} markers. The text outside of the
 * markers is kept unchanged.
 */
public final class PomDependenciesWriter {

    static final String BEGIN_DEPENDENCIES = "<!-- BEGIN_BUPDATER_DEPENDENCIES -->";

    static final String END_DEPENDENCIES = "<!-- END_BUPDATER_DEPENDENCIES -->";

    /**
     * Reads the given POM file, replaces the managed dependencies between the markers and writes the
     * result back to the same file.
     *
     * @param pomFile     POM file to update (read and written).
     * @param frameworks  Frameworks used to align the dependencies (rendered as a comment).
     * @param bomVersions Harmonized dependencies to write.
     */
    public void write(File pomFile, List<MavenGAV> frameworks, List<BomVersions> bomVersions) {
        final List<String> lines = readLines(pomFile);
        final String content = rewrite(pomFile.getAbsolutePath(), lines, frameworks, bomVersions);
        try (Writer writer = new FileWriter(pomFile)) {
            writer.write(content);
        } catch (IOException ex) {
            throw new CoreException("Failed to write: " + pomFile, ex);
        }
    }

    /**
     * Produces the new file content by replacing everything between the markers. This method does no
     * file I/O and is therefore easy to test.
     *
     * @param source      Description of the source (used in error messages only).
     * @param lines       Original lines of the POM.
     * @param frameworks  Frameworks used to align the dependencies (rendered as a comment).
     * @param bomVersions Harmonized dependencies to write.
     *
     * @return New file content including line separators.
     */
    String rewrite(String source, List<String> lines, List<MavenGAV> frameworks, List<BomVersions> bomVersions) {
        final String spaces = determineSpaces(source, lines);
        final List<String> linesBefore = filterBefore(source, lines);
        final List<String> linesAfter = filterAfter(source, lines);
        final String nl = System.lineSeparator();

        final StringBuilder sb = new StringBuilder();
        for (final String line : linesBefore) {
            sb.append(line).append(nl);
        }
        sb.append(nl);
        sb.append(spaces).append("<!-- Frameworks used to align the dependencies:").append(nl);
        for (final MavenGAV framework : frameworks) {
            sb.append(spaces).append(framework).append(nl);
        }
        sb.append(spaces).append("-->").append(nl);
        for (final BomVersions dep : bomVersions) {
            sb.append(nl);
            sb.append(spaces).append("<!-- ").append(asString(dep.versions())).append(" -->").append(nl);
            sb.append(spaces).append("<dependency>").append(nl);
            sb.append(spaces).append("    <groupId>").append(dep.artifact().groupId()).append("</groupId>").append(nl);
            sb.append(spaces).append("    <artifactId>").append(dep.artifact().artifactId()).append("</artifactId>").append(nl);
            sb.append(spaces).append("    <version>").append(dep.harmonized().orElseThrow()).append("</version>").append(nl);
            sb.append(spaces).append("</dependency>").append(nl);
        }
        sb.append(nl);
        for (final String line : linesAfter) {
            sb.append(line).append(nl);
        }
        return sb.toString();
    }

    static String asString(Map<MavenGA, Version> map) {
        if (map.size() == 1 && map.containsKey(BomVersions.EXTERNAL_SOURCE)) {
            return "Not in framework BOM";
        }
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<MavenGA, Version> entry : map.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(entry.getKey().artifactId()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private static List<String> readLines(File pomFile) {
        final List<String> lines = new ArrayList<>();
        try (LineNumberReader reader = new LineNumberReader(new FileReader(pomFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException ex) {
            throw new CoreException("Failed to read: " + pomFile, ex);
        }
        return lines;
    }

    private static String determineSpaces(final String source, final List<String> lines) {
        final String str = lines.stream()
                .filter(line -> line.contains(BEGIN_DEPENDENCIES))
                .findFirst()
                .orElseThrow(() -> new CoreException("Failed to find '" + BEGIN_DEPENDENCIES + "' in: " + source));
        final int c = str.indexOf(BEGIN_DEPENDENCIES);
        return " ".repeat(c);
    }

    private static List<String> filterBefore(final String source, final List<String> lines) {
        final List<String> before = new ArrayList<>();
        boolean found = false;
        for (final String line : lines) {
            before.add(line);
            if (line.contains(BEGIN_DEPENDENCIES)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new CoreException("Failed to find '" + BEGIN_DEPENDENCIES + "' in: " + source);
        }
        return before;
    }

    private static List<String> filterAfter(final String source, final List<String> lines) {
        final List<String> after = new ArrayList<>();
        boolean found = false;
        for (final String line : lines) {
            if (line.contains(END_DEPENDENCIES)) {
                found = true;
            }
            if (found) {
                after.add(line);
            }
        }
        if (!found) {
            throw new CoreException("Failed to find '" + END_DEPENDENCIES + "' in: " + source);
        }
        return after;
    }

}
