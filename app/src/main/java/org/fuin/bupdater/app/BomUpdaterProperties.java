package org.fuin.bupdater.app;

import org.fuin.bupdater.core.PrintType;
import org.fuin.bupdater.core.Strategy;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.io.File;
import java.util.List;

/**
 * Type safe configuration properties.
 */
@SuppressWarnings("NullAway.Init")
@ConfigurationProperties("org.fuin.bupdater")
@Validated
public class BomUpdaterProperties {

    /**
     * Path and file of the 'pom.xml' to update.
     */
    private File pomFile;

    /**
     * Strategy to update the dependencies for minor/patch conflicts. Defaults to HIGHEST.
     */
    private Strategy strategy = Strategy.HIGHEST;

    /**
     * GAVs of the frameworks to use. Example: "io.quarkus:quarkus-bom:3.23.4".
     */
    List<String> frameworks;

    /**
     * GAVs of the dependencies to set manually. Example: "com.fasterxml.jackson.core:^.*$:2.19.0".
     */
    @Nullable
    private List<String> fixedVersions;

    /**
     * GAs that define exactly which artifacts end up in the updated BOM. Only listed
     * {@code groupId:artifactId} entries are inserted. Example: "com.fasterxml.jackson.core:^.*$".
     */
    @Nullable
    private List<String> includes;

    /**
     * Print all dependencies or only major differences.
     */
    private PrintType printType = PrintType.FAILED;

    /**
     * Path and file of the HTML output.
     */
    @Nullable
    private File printFile;

    public List<String> getFrameworks() {
        return frameworks;
    }

    public void setFrameworks(List<String> frameworks) {
        this.frameworks = frameworks;
    }

    public File getPomFile() {
        return pomFile;
    }

    public void setPomFile(File pomFile) {
        this.pomFile = pomFile;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    @Nullable
    public List<String> getFixedVersions() {
        return fixedVersions;
    }

    public void setFixedVersions(@Nullable List<String> fixedVersions) {
        this.fixedVersions = fixedVersions;
    }

    @Nullable
    public List<String> getIncludes() {
        return includes;
    }

    public void setIncludes(@Nullable List<String> includes) {
        this.includes = includes;
    }

    public PrintType getPrintType() {
        return printType;
    }

    public void setPrintType(PrintType printType) {
        this.printType = printType;
    }

    public @Nullable File getPrintFile() {
        return printFile;
    }

    public void setPrintFile(@Nullable File printFile) {
        this.printFile = printFile;
    }
}