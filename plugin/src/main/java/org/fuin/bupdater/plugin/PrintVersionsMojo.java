package org.fuin.bupdater.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.fuin.bupdater.core.BomUpdater;
import org.fuin.bupdater.core.HtmlPrinter;
import org.fuin.bupdater.core.PrintType;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.Objects;

@Mojo(name = "print",
        defaultPhase = LifecyclePhase.NONE,
        threadSafe = true)
public class PrintVersionsMojo extends AbstractBomUpdaterMojo {

    @Nullable
    @Parameter(property = "printType", defaultValue = "FAILED")
    PrintType printType;

    @Nullable
    @Parameter(property = "printFile", defaultValue = "bupdater.html")
    String printFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final BomUpdater updater = createBomUpdater();
        final File buildDir = new File(project.getBuild().getDirectory());
        if (!buildDir.exists() && !buildDir.mkdirs()) {
            throw new MojoExecutionException("Could not create directory: " + buildDir.getAbsolutePath());
        }
        final File file = new File(buildDir, Objects.requireNonNull(printFile));
        getLog().info("HTML file: " + file.getAbsolutePath());
        new HtmlPrinter().write(updater, Objects.requireNonNull(printType), file);
    }

}
