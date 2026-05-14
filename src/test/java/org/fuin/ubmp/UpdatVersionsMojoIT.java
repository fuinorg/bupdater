package org.fuin.ubmp;

import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

/**
 * Test for the {@link UpdateVersionsMojo} class.
 */
@MavenJupiterExtension
public class UpdatVersionsMojoIT {

    @MavenTest
    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:update")
    void testHighest(MavenExecutionResult result) {
        assertThat(result).isSuccessful();
        assertThat(result)
                .out()
                .info()
                .anyMatch(line -> {
                    System.err.println(line);
                    return line.contains("Latest version of utils4j:");
                });
    }

    @MavenTest
    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:update")
    void testLowest(MavenExecutionResult result) {
        assertThat(result).isSuccessful();
        assertThat(result)
                .out()
                .info()
                .anyMatch(line -> {
                    System.err.println(line);
                    return line.contains("Latest version of utils4j:");
                });
    }

}

