package org.fuin.bupdater.core;

import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.StringTokenizer;

/**
 * Represents a semantic version with non-standard values "build" and "extra".
 * This is used by several Maven artifacts like Oracle and others.
 *
 * @param major Major number.
 * @param minor Minor number.
 * @param patch Patch number.
 * @param build Build number.
 * @param extra Extra number.
 * @param preRelease Optional pre-release like "SNAPSHOT".
 */
public record Semver(int major,
                     int minor,
                     int patch,
                     int build,
                     int extra,
                     @Nullable String preRelease) implements Comparable<Semver> {

    @Override
    public int compareTo(Semver o) {
        int c = Integer.compare(major, o.major);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(minor, o.minor);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(patch, o.patch);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(build, o.build);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(extra, o.extra);
        if (c != 0) {
            return c;
        }
        if (preRelease == null) {
            if  (o.preRelease == null) {
                return 0;
            }
            return 1;
        }
        if (o.preRelease == null) {
            return -1;
        }
        return preRelease.compareTo(o.preRelease);
    }

    /**
     * Determines if this is a stable version.
     *
     * @return {@literal true} in case there is no pre-release part.
     */
    public boolean isStable() {
        return preRelease == null;
    }

    public static Optional<Semver> coerce(String s) {
        int c = s.indexOf('-');
        final String str;
        String preRelease;
        if (c == -1) {
            preRelease = null;
            str = s;
        } else  {
            preRelease = s.substring(c + 1);
            str = s.substring(0, c);
        }

        final StringTokenizer tokenizer = new StringTokenizer(str, ".");
        final int count = tokenizer.countTokens();
        if (count < 1 || count > 5) {
            return Optional.empty();
        }
        try {
            final int major = Integer.parseInt(tokenizer.nextToken());
            final int minor = count >= 2 ? Integer.parseInt(tokenizer.nextToken()) : 0;
            final int patch = count >= 3 ? Integer.parseInt(tokenizer.nextToken()) : 0;
            int build = 0;
            if (count >= 4) {
                final String buildToken = tokenizer.nextToken();
                final Integer buildNumber = parseBuild(buildToken);
                if (buildNumber == null) {
                    // Non-numeric qualifier like "Beta1" or "Alpha1" is a pre-release
                    if (preRelease == null) {
                        preRelease = buildToken;
                    }
                } else {
                    build = buildNumber;
                }
            }
            final int extra = count == 5 ? Integer.parseInt(tokenizer.nextToken()) : 0;
            return Optional.of(new Semver(major, minor, patch, build, extra, preRelease));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Parses the build segment of a version.
     *
     * @param str Segment to parse.
     *
     * @return Build number, {@literal 0} for a stable keyword ("FINAL"/"RELEASE"), or
     *         {@literal null} if the segment is a non-numeric qualifier (pre-release).
     */
    @Nullable
    private static Integer parseBuild(String str) {
        if (str.equalsIgnoreCase("FINAL")
                || str.equalsIgnoreCase("RELEASE") ) {
            return 0;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
