# Super BOM Updater
Updates a Maven BOM based on other BOMs such as `spring-boot-dependencies` and/or the Quarkus BOM.
Helps you define and maintain your own "Super BOM" derived from multiple framework BOMs.

[![Maven Build](https://github.com/fuinorg/bupdater/actions/workflows/maven.yml/badge.svg)](https://github.com/fuinorg/bupdater/actions/workflows/maven.yml)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=bupdater&metric=coverage)](https://sonarcloud.io/dashboard?id=bupdater)
[![Maven Central](https://img.shields.io/maven-central/v/org.fuin.bupdater/bupdater-maven-plugin)](https://mvnrepository.com/artifact/org.fuin.bupdater/bupdater-maven-plugin)
[![LGPLv3 License](http://img.shields.io/badge/license-LGPLv3-blue.svg)](https://www.gnu.org/licenses/lgpl.html)
[![Java Development Kit 17](https://img.shields.io/badge/JDK-17-green.svg)](https://openjdk.java.net/projects/jdk/17/)

## Background
Custom libraries are most often used in a final product together with a framework like [Spring Boot](https://spring.io/projects/spring-boot)
or [Quarkus](https://quarkus.io/). These frameworks depend on many other third-party libraries, which are usually
managed in a framework-specific [Maven BOM](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Bill_of_Materials_.28BOM.29_POMs).

Aligning the dependencies of your custom library with two (or more) frameworks like Spring Boot **and** Quarkus is challenging,
because they may pin different versions of the same third-party libraries.

This Maven plugin harmonizes the versions of third-party JARs in your own BOM across two (or more) frameworks.

Simply define a list of all Maven artifacts (Group ID + Artifact ID) you want to include in your "Super BOM".

## Example
Let's assume you want to harmonize your library with both Quarkus and Spring Boot versions, and it depends on FasterXML Jackson.
Spring depends on `com.fasterxml.jackson.core:jackson-annotations` version `2.18.3`, whereas Quarkus is on version `2.18.2`.
Which version do you want to use for your custom library POM — the highest (`2.18.3`) or the lowest (`2.18.2`)?

It gets even trickier when there is a major (and therefore incompatible) change:
`org.flywaydb:flyway-database-mongodb` is at `10.20.1` in Spring Boot, while Quarkus is already on `11.3.4`.
In such cases, you will need to fix the version manually.

## Logic for updating
The following situations need to be handled during updates:

| BOM 1 | BOM 2 | Update (lowest) | Update (highest) |
|-------|-------|-----------------|------------------|
| 1.0.0 | 1.0.0 | 1.0.0           | 1.0.0            |
| 1.0.1 | 1.0.0 | 1.0.0           | 1.0.1            |
| 1.0.0 | 1.0.1 | 1.0.0           | 1.0.1            |
| 1.1.0 | 1.0.1 | 1.0.1           | 1.1.0            |
| 1.0.2 | 1.3.0 | 1.0.2           | 1.3.0            |
| 2.0.0 | 1.9.0 | CONFLICT        | CONFLICT         |

The `lowest` setting means "always take the lowest compatible version" and `highest` means "always take the highest compatible version".
The `highest compatible` strategy is the default. *Compatible* is used here in the sense of [semantic versioning](https://version.org/),
where `minor` and `patch` versions are expected to be compatible.

`CONFLICT` means you have to explicitly decide which version to use. This is done by adding an entry to
the `fixedVersions` section of the plugin configuration shown below. You will need to investigate what
incompatible changes exist and whether they affect your library. In some cases, there may be no solution
that satisfies users of both frameworks. The `includes` section of the configuration defines exactly
which artifacts end up in the updated BOM &mdash; only listed `groupId:artifactId` entries are inserted.
If an included artifact is not provided by any of the configured framework BOMs, its latest version
is resolved from Maven Central and inserted.

## Getting started

### Configuration
In the `pom.xml` to be updated, add the following two settings.

#### Begin/End Comments
The BEGIN/END comments mark the segment where the existing managed dependencies should be replaced.
All text between these two comments will be fully replaced with the current matching dependencies.
```xml
<dependencyManagement>
    <dependencies>
        <!-- BEGIN_BUPDATER_DEPENDENCIES -->
        <!-- END_BUPDATER_DEPENDENCIES -->
    </dependencies>
</dependencyManagement>
```
Manually maintained dependencies can be added before or after these comments.
They will not be touched by the updater.

#### Plugin
To be able to run the `bupdater:update` or `bupdater:print` goal, you must add the plugin:
```xml
<plugin>
    <groupId>org.fuin.bupdater</groupId>
    <artifactId>bupdater-maven-plugin</artifactId>
    <version>@project.version@</version>
    <configuration>
        <strategy>HIGHEST</strategy>
        <frameworks>
            <!-- Sets the exact framework versions to use. -->
            <framework>io.quarkus:quarkus-bom:3.24.4</framework>
            <framework>org.springframework.boot:spring-boot-dependencies:3.5.3</framework>
        </frameworks>
        <includes>
            <!-- Defines exactly the artifacts to include in the updated BOM. -->
            <include>com.fasterxml.jackson.core:^.*$</include>
            <include>org.slf4j:^.*$</include>
        </includes>
        <fixedVersions>
            <!-- Overwrites the version that would have been used by the updater. -->
            <fixed-version>com.fasterxml.jackson.core:jackson-annotations:2.18.3</fixed-version>
            <fixed-version>org.flywaydb:^.*$:10.20.1</fixed-version>
        </fixedVersions>
        <printType>FAILED</printType> <!-- Print ALL or only FAILED version updates. -->
        <printFile>bupdater.html</printFile> <!-- Name of the HTML file in the "target" folder -->
    </configuration>
</plugin>
```
Replace `@project.version@` with the latest version available on
[Maven Central](https://mvnrepository.com/artifact/org.fuin.bupdater/bupdater-maven-plugin).

Available plugin configuration parameters:

| Parameter      | Description                                                                                                                                                                                                                  | Default   |
|----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| strategy       | The strategy to use for automatic updates: `lowest` or `highest`.                                                                                                                                                            | `highest` |
| frameworks     | `groupId:artifactId:version` of the framework BOMs used to determine versions.                                                                                                                                               | -         |
| includes       | `groupId:artifactId` of an artifact that must appear in the updated BOM. The `artifactId` may be a regular expression covering several artifacts of the group; it must start with `^` and end with `$`. Only listed artifacts are inserted. Concrete (non-regex) entries that are not provided by any framework BOM are resolved to their latest version from Maven Central. | -         |
| fixedVersions | `groupId:artifactId:version` of a fixed dependency to use. The `artifactId` may be a regular expression to apply the same fixed version to several artifacts of the group; it must start with `^` and end with `$`.          | -         |

### Goals
- **PRINT** — Creates an HTML file in the target folder that shows the framework BOM versions and the harmonized ones.
- **UPDATE** — Updates the `pom.xml` in which the plugin is declared.

### Running without adding the plugin to the POM
You don't have to declare the plugin in your `pom.xml`. The goals can be invoked directly from the
command line using the fully qualified plugin coordinates `groupId:artifactId:version:goal`. Run the
command from the directory that contains the `pom.xml` you want to process:
```bash
# Update the dependencies directly in the pom.xml
mvn org.fuin.bupdater:bupdater-maven-plugin:@project.version@:update

# Create the HTML report in the "target" folder instead
mvn org.fuin.bupdater:bupdater-maven-plugin:@project.version@:print
```
Replace `@project.version@` with the latest version available on
[Maven Central](https://mvnrepository.com/artifact/org.fuin.bupdater/bupdater-maven-plugin).

All configuration parameters can be passed as `-D` system properties. List parameters
(`frameworks`, `includes`, `fixedVersions`) are comma-separated. Quote values that contain
shell-special characters such as the `^...$` regular expressions:
```bash
mvn org.fuin.bupdater:bupdater-maven-plugin:@project.version@:update \
    -Dstrategy=HIGHEST \
    -Dframeworks=io.quarkus:quarkus-bom:3.24.4,org.springframework.boot:spring-boot-dependencies:3.5.3 \
    -Dincludes='com.fasterxml.jackson.core:^.*$,org.slf4j:^.*$' \
    -DfixedVersions='com.fasterxml.jackson.core:jackson-annotations:2.18.3,org.flywaydb:^.*$:10.20.1'
```
The `update` goal still requires the `BEGIN_BUPDATER_DEPENDENCIES` / `END_BUPDATER_DEPENDENCIES`
comments described above to be present in the target `pom.xml`.

#### Optional: short `bupdater:` prefix
To use the shorter `mvn bupdater:update` form (without the `groupId:artifactId:version` part), add the
plugin group to your `~/.m2/settings.xml` once:
```xml
<pluginGroups>
    <pluginGroup>org.fuin.bupdater</pluginGroup>
</pluginGroups>
```

## Modules
This project contains the following Maven modules:

- [app](app) — Command line application, mainly for testing and development.
- [core](core) — Main functionality, independent of the Maven plugin and the command line app.
- [plugin](plugin) — Maven plugin to be used in your "Super BOM". Usage is described above.
