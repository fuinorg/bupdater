package org.fuin.ubmp;

/**
 * Strategy for updating versions.
 */
public enum Strategy {

    /** Always take the lowest compatible version. */
    LOWEST,

    /** Always take the highest compatible version. */
    HIGHEST

}
