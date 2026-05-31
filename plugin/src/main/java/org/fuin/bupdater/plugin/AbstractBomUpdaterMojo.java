package org.fuin.bupdater.plugin;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.fuin.bupdater.core.BomUpdater;
import org.fuin.bupdater.core.CoreException;
import org.fuin.bupdater.core.Strategy;
import org.jspecify.annotations.Nullable;

import java.util.List;

public abstract class AbstractBomUpdaterMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "strategy", defaultValue = "HIGHEST")
    Strategy strategy = Strategy.HIGHEST;

    @Nullable
    @Parameter(property = "frameworks")
    List<String> frameworks;

    @Nullable
    @Parameter(property = "fixedVersions")
    List<String> fixedVersions;

    @Nullable
    @Parameter(property = "includes")
    List<String> includes;


    protected BomUpdater createBomUpdater() throws MojoExecutionException {
        final ContextOverrides overrides = ContextOverrides.create().build();
        final Runtime runtime = Runtimes.INSTANCE.getRuntime();
        getLog().debug("Runtimes.getRuntime: " + runtime);
        try (Context context = runtime.create(overrides)) {
            final BomUpdater.Builder builder = new BomUpdater.Builder(context, includes).strategy(strategy);
            if (frameworks == null) {
                getLog().info("Using model from POM");
                builder.addBoms(project.getModel());
            } else {
                builder.addBoms(frameworks);
            }
            builder.addFixedVersions(fixedVersions);
            return builder.build();
        } catch (CoreException ex) {
            throw new MojoExecutionException(ex);
        }
    }

}
