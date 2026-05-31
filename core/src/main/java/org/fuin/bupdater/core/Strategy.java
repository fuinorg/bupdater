package org.fuin.bupdater.core;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/**
 * Strategy for updating versions.
 */
public enum Strategy {

    /** Always take the lowest compatible id. */
    LOWEST(Strategy::lowest),

    /** Always take the highest compatible id. */
    HIGHEST(Strategy::highest);

    private final Function<Collection<Version>, Optional<Version>> func;

    Strategy(Function<Collection<Version>, Optional<Version>> func) {
        this.func = func;
    }

    public Optional<Version> harmonized(Collection<Version> versions) {
        return func.apply(versions);
    }

    private static Optional<Version> highest(Collection<Version> versions)  {
        if (versions.isEmpty()) {
            return Optional.empty();
        }
        return versions.stream().max(Version::compareTo);
    }

    private static Optional<Version> lowest(Collection<Version> versions)  {
        if (versions.isEmpty()) {
            return Optional.empty();
        }
        return versions.stream().min(Version::compareTo);
    }

}
