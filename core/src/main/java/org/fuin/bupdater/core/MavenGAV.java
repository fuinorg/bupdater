package org.fuin.bupdater.core;

/**
 * Combination of Maven GA and id.
 *
 * @param ga      Maven GA.
 * @param version Maven id.
 */
public record MavenGAV(MavenGA ga, Version version) implements Comparable<MavenGAV> {

    /**
     * Convenience constructor with all strings.
     *
     * @param groupId    The Maven 'groupId' or a regular expression to handle several groups.
     * @param artifactId The Maven 'artifactId' or a regular expression to handle several artifacts of a group.
     * @param version    Maven id.
     */
    public MavenGAV(String groupId, String artifactId, Version version) {
        this(new MavenGA(groupId, artifactId), version);
    }

    @Override
    public int compareTo(MavenGAV o) {
        int c = ga.compareTo(o.ga);
        if (c != 0) {
            return c;
        }
        return version.compareTo(o.version);
    }

    @Override
    public String toString() {
        return ga + ":" + version;
    }

    /**
     * Parses a Maven GAV.
     *
     * @param str String with format 'groupId:artifactId:id', where the 'groupId' and 'artifactId'
     *            may be a regular expression starting with a '^' and end with a '$'.
     * @return Maven groupId:artifactId:version
     */
    public static MavenGAV of(String str) {
        String[] split = str.split(":");
        if (split.length != 3) {
            throw new IllegalArgumentException("Invalid Maven GAV: '" + str + "'");
        }
        final Version version = Version.coerce(split[2]);
        return new MavenGAV(MavenGA.of(split[0] + ":" + split[1]), version);
    }

}
