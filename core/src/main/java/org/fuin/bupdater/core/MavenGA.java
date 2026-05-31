package org.fuin.bupdater.core;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Represents a combination of a Maven 'groupId' and 'artifactId',
 * which may be regular expressions (starting with '^' and ending with '$').
 *
 * @param groupId         The Maven 'groupId' or a regular expression to handle several groups.
 * @param groupRegExpr    The 'groupId' is a regular expression.
 * @param artifactId      The Maven 'artifactId' or a regular expression to handle several artifacts of a group.
 * @param artifactRegExpr The 'artifactId' is a regular expression.
 */
public record MavenGA(String groupId,
                      boolean groupRegExpr,
                      String artifactId,
                      boolean artifactRegExpr) implements Comparable<MavenGA> {

    /**
     * Convenience constructor with GA without regular expressions.
     *
     * @param groupId    The Maven 'groupId' (NOT a regular expression).
     * @param artifactId The Maven 'artifactId' (NOT a regular expression).
     */
    public MavenGA(final String groupId, final String artifactId) {
        this(noRegExpr(groupId), false, noRegExpr(artifactId), false);
    }

    @Override
    public int compareTo(MavenGA o) {
        int cmp = groupId.compareTo(o.groupId);
        if (cmp != 0) {
            return cmp;
        }
        return artifactId.compareTo(o.artifactId);
    }

    /**
     * Determines if this instance has a regular expression.
     *
     * @return {@literal true} if either 'groupId' or 'artifactId' is a regular expression.
     */
    public boolean hasRegularExpressions() {
        return groupRegExpr || artifactRegExpr;
    }

    /**
     * Determines if this instance represents the given one.
     * It can either be the equal or the regular expression
     *
     * @param mavenGA A Maven groupId/artifactId without regular expressions.
     * @return {@literal true} in case this GA captures the given one.
     */
    public boolean similar(MavenGA mavenGA) {
        if (mavenGA.groupRegExpr || mavenGA.artifactRegExpr) {
            throw new IllegalArgumentException("The parameter is not allowed to contain regular expressions, but was: " + mavenGA);
        }
        if (groupRegExpr) {
            if (!mavenGA.groupId.matches(groupId)) {
                return false;
            }
        } else {
            if (!groupId.equals(mavenGA.groupId)) {
                return false;
            }
        }
        if (artifactRegExpr) {
            return mavenGA.artifactId.matches(artifactId);
        }
        return artifactId.equals(mavenGA.artifactId);
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId;
    }

    /**
     * Parses a string into an instance of this class.
     *
     * @param str The string to parse. Expected format: 'artifactId:groupId',
     *            where the two parts may be a regular expression starting with a '^' and end with a '$'.
     * @return New instance.
     */
    public static MavenGA of(String str) {
        final int p = str.indexOf(':');
        if (p < 0) {
            throw new IllegalArgumentException("Expected format 'groupId:artifactId', but was: '" + str + "'");
        }
        if (str.indexOf(p + 1) != -1) {
            throw new IllegalArgumentException("Expected format 'groupId:artifactId', but was: '" + str + "'");
        }
        final String groupId = str.substring(0, p);
        if (groupId.isEmpty()) {
            throw new IllegalArgumentException("Expected format 'groupId:artifactId', but was: '" + str + "'");
        }
        final String artifactId = str.substring(p + 1);
        if (artifactId.isEmpty()) {
            throw new IllegalArgumentException("Expected format 'artifactId', but was: '" + str + "'");
        }
        return new MavenGA(groupId, regExpr(groupId), artifactId, regExpr(artifactId));
    }

    private static boolean regExpr(final String str) {
        if (str.startsWith("^") && str.endsWith("$")) {
            try {
                Pattern.compile(str);
            } catch (PatternSyntaxException ex) {
                throw new IllegalArgumentException("Invalid regular expression '" + str + "': " + ex.getDescription());
            }
            return true;
        }
        if (str.startsWith("^") || str.endsWith("$")) {
            throw new IllegalArgumentException("Expected regular expression format '^EXPR$', but was: " + str);
        }
        return false;
    }

    private static String noRegExpr(final String str) {
        if (str.startsWith("^") || str.endsWith("$")) {
            throw new IllegalArgumentException("Regular expression not allowed, but was: " + str);
        }
        return str;
    }

}
