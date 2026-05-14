# update-bom-maven-plugin
Updates a Maven BOM/POM based on other BOMs like "spring-boot-dependencies" and/or Quarkus BOM

## Background
Custom libraries are most often used in a final product with some kind of framework like [Spring Boot](https://spring.io/projects/spring-boot)
or [Quarkus](https://quarkus.io/). These frameworks depend on many other third-party libraries that are most likely
maintained in a framework specific [Maven BOM](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Bill_of_Materials_.28BOM.29_POMs).

Aligning dependencies of your custom library with two (or more) frameworks like Spring Boot AND Quarkus is challenging
as they may have different versions of the same third party libraries.

This Maven plugin tries to harmonize the versions of third-party JARs in your own BOM with two (or more) frameworks.

## Example
Let's assume you want to harmonize your library with Quarkus AND Spring Boot versions and it depends on FasterXML Jackson.
Now Spring depends on "com.fasterxml.jackson.core:jackson-annotations" version "2.18.3", whereas Quarkus is on version "2.18.2".
Which version you want to use for your custom library POM? The highest (2.18.3) or the lowest (2.18.2) version?

It gets even worse if there is a major (though incompatible) change:
"org.flywaydb:flyway-database-mongodb" in Spring Boot has "10.20.1" and Quarkus is already on "11.3.4".
You will need to fix a version manually in such case.

## Logic for updating
There are the following situations to handle for updates:

| BOM 1 | BOM 2 | Update (lowest) | Update (highest) |
|-------|-------|-----------------|------------------|
| 1.0.0 | 1.0.0 | 1.0.0           | 1.0.0            |
| 1.0.1 | 1.0.0 | 1.0.0           | 1.0.1            |
| 1.0.0 | 1.0.1 | 1.0.0           | 1.0.1            |
| 1.1.0 | 1.0.1 | 1.0.1           | 1.1.0            |
| 1.0.2 | 1.3.0 | 1.0.2           | 1.3.0            |
| 2.0.0 | 1.9.0 | CONFLICT        | CONFLICT         |

The "lowest" setting means "always take the lowest compatible version" and "highest" means "always take the highest compatible version".
The "highest compatible" strategy is the default.
Being "compatible" is used in the sense of [semantic versioning](https://semver.org/) where "minor" and "patch" versions are supposed to be compatible. 

"CONFLICT" means you have to explicitly set which version to use. This is done by adding an entry to 
the `fixed-versions` section in the plugin configuration shown below. You will need to investigate what
incompatible changes there are and if this affects your library. Eventually, there is no solution at all
that satisfies users of both frameworks.

## Precondition


## Configuration

```xml
<plugin>
    <groupId>org.fuin</groupId>
    <artifactId>update-bom-maven-plugin</artifactId>
    <version>@project.version@</version>
    <configuration>
        <strategy>lowest</strategy>
        <frameworks>
            <framework>io.quarkus:quarkus-bom:3.24.4</framework>
            <framework>org.springframework.boot:spring-boot-dependencies:3.5.3</framework>
        </frameworks>
        <fixed-versions>
            <fixed-version>com.fasterxml.jackson.core:jackson-annotations:2.18.3</fixed-version>
            <fixed-version>org.flywaydb:*:10.20.1</fixed-version>
        </fixed-versions>
    </configuration>
</plugin>
```
Possible plugin config:

| parameter     | description                                                                                                                                               | default   |
|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| strategy      | The strategy for automatic updates to use: `lowest` or `highest`                                                                                          | `highest` |
| framework-bom | groupId:artifactId:version of the framework BOMs to use for determining versions                                                                          | -         |
| fixed-version | groupId:artifactId:version of a fixed dependency to use. You can use `*` for the artifactId to use the same fixed version for all artifacts of the group. | -         |

