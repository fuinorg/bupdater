package org.fuin.bupdater.app;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import org.fuin.bupdater.core.BomUpdater;
import org.fuin.bupdater.core.BomVersions;
import org.fuin.bupdater.core.CoreException;
import org.fuin.bupdater.core.HtmlPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Command line application to update a BOM 'pom.xml'.
 */
@EnableConfigurationProperties(BomUpdaterProperties.class)
public class BomUpdaterApp implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(BomUpdaterApp.class);

    private final BomUpdaterProperties properties;

    public BomUpdaterApp(BomUpdaterProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties==null");
    }

    @Override
    public void run(ApplicationArguments args) {
        LOG.info("Starting BOM Updater...");

        if (args.getNonOptionArgs().size() != 1) {
            throw new CoreException("Expected the command type (PRINT or UPDATE) as single argument, but was: " + Arrays.asList(args));
        }

        final Command command = Command.valueOf(args.getNonOptionArgs().get(0));

        final ContextOverrides overrides = ContextOverrides.create().build();
        final Runtime runtime = Runtimes.INSTANCE.getRuntime();
        LOG.debug("Runtimes.getRuntime: {}", runtime);

        try (Context context = runtime.create(overrides)) {
            final BomUpdater.Builder builder = new BomUpdater.Builder(context, properties.getIncludes());
            builder.addBoms(properties.getFrameworks());
            builder.addFixedVersions(properties.getFixedVersions());
            final BomUpdater updater = builder.build();
            if (command == Command.UPDATE) {
                if (updater.isFailed()) {
                    throw new RuntimeException("Failed major diff: " + updater.getFailedAsString());
                }
                final List<BomVersions> anyDiffs = updater.getAnyDiffs().toList();
                if (!anyDiffs.isEmpty()) {
                    LOG.info("Any id diffs: {}", updater.getAnyAsString());
                }
                updater.updateDependencies(properties.getPomFile());
            } else if (command == Command.PRINT) {
                if (properties.getPrintFile() == null) {
                    throw new CoreException("Print file is null");
                }
                new HtmlPrinter().write(updater, properties.getPrintType(), properties.getPrintFile());
            } else {
                throw new CoreException("Unknown command: " + command);
            }
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .bannerMode(Banner.Mode.OFF)
                .sources(BomUpdaterApp.class)
                .run(args);
    }

}
