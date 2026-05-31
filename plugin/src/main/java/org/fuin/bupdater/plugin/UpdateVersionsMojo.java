package org.fuin.bupdater.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.fuin.bupdater.core.BomUpdater;
import org.fuin.bupdater.core.BomVersions;

import java.util.List;

@Mojo(name = "update",
        defaultPhase = LifecyclePhase.NONE,
        threadSafe = true)
public class UpdateVersionsMojo extends AbstractBomUpdaterMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

            final BomUpdater updater = createBomUpdater();

            if (updater.isFailed()) {
                getLog().error("There are major id changed that differ between the frameworks: " + updater.getFailedAsString());
                throw new MojoExecutionException("Failed major diff");
            }
            final List<BomVersions> anyDiffs = updater.getAnyDiffs().toList();
            if (!anyDiffs.isEmpty()) {
                getLog().info("Any id diffs: " + updater.getAnyAsString());
            }
            updater.updateDependencies(project.getModel().getPomFile());

    }

}
