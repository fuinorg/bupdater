package org.fuin.bupdater.plugin;

import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;

import java.io.File;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

/**
 * Test for the {@link UpdateVersionsMojo} class.
 */
@MavenJupiterExtension
public class UpdateVersionsMojoIT {

    @MavenTest
    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:update")
    void testHighest(MavenExecutionResult result) {
        assertThat(result).isSuccessful();
        assertThat(result)
                .out()
                .info()
                .anyMatch(line -> line.contains("Fixed versions: [co.elastic.clients:elasticsearch-java:8.18.5, com.fasterxml.jackson.core:^.*$:2.19.0, com.graphql-java:graphql-java:24.3.0, com.mysql:mysql-connector-j:8.3.0, jakarta.annotation:jakarta.annotation-api:3.0.0, org.hamcrest:hamcrest:3.0.0]"))
                .anyMatch(line -> line.contains("Included versions: [com.fasterxml.jackson.core:^.*$, jakarta.annotation:jakarta.annotation-api, org.hamcrest:hamcrest, org.slf4j:^.*$]"))
                .anyMatch(line -> line.contains("Strategy: HIGHEST"))
                .anyMatch(line -> line.contains("Any id diffs:"))
                .anyMatch(line -> line.contains("Resolved artifact: com.fasterxml.jackson.core"))
                .anyMatch(line -> line.contains("Resolved artifact: jakarta.annotation:jakarta.annotation-api"))
                .anyMatch(line -> line.contains("Resolved artifact: org.hamcrest:hamcrest"))
                .anyMatch(line -> line.contains("Resolved artifact: org.slf4j"));

    }

    @MavenTest
    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:update")
    void testLowest(MavenExecutionResult result) {
        assertThat(result).isSuccessful();
        assertThat(result)
                .out()
                .info()
                .anyMatch(line -> line.contains("Fixed versions: [co.elastic.clients:elasticsearch-java:8.18.5, com.fasterxml.jackson.core:^.*$:2.19.0, com.graphql-java:graphql-java:24.3.0, com.mysql:mysql-connector-j:8.3.0, jakarta.annotation:jakarta.annotation-api:3.0.0, org.hamcrest:hamcrest:3.0.0]"))
                .anyMatch(line -> line.contains("Included versions: [com.fasterxml.jackson.core:^.*$, jakarta.annotation:jakarta.annotation-api, org.hamcrest:hamcrest, org.slf4j:^.*$]"))
                .anyMatch(line -> line.contains("Strategy: LOWEST"))
                .anyMatch(line -> line.contains("Any id diffs:"))
                .anyMatch(line -> line.contains("Resolved artifact: com.fasterxml.jackson.core"))
                .anyMatch(line -> line.contains("Resolved artifact: jakarta.annotation:jakarta.annotation-api"))
                .anyMatch(line -> line.contains("Resolved artifact: org.hamcrest:hamcrest"))
                .anyMatch(line -> line.contains("Resolved artifact: org.slf4j"));
    }

    @MavenTest
    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:print")
    void testPrint(MavenExecutionResult result) {
        assertThat(result).isSuccessful();
        assertThat(result)
                .out()
                .info()
                .anyMatch(line -> line.contains("HTML file:"))
                .anyMatch(line -> line.contains("Resolved artifact: com.fasterxml.jackson.core"))
                .anyMatch(line -> line.contains("Resolved artifact: jakarta.annotation:jakarta.annotation-api"))
                .anyMatch(line -> line.contains("Resolved artifact: org.hamcrest:hamcrest"))
                .anyMatch(line -> line.contains("Resolved artifact: org.slf4j"));
        assertThat(new File("target/maven-it/org/fuin/bupdater/plugin/UpdateVersionsMojoIT/testPrint/project/target/test-print.html")).exists();
    }

}

