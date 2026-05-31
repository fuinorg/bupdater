# bupdater-app
Spring Boot console application that patches a BOM `pom.xml` in the same way as the Maven plugin.
This is mainly useful for manual testing and for developing the core outside of the Maven plugin.

The equivalent of the Maven `configuration` settings can be found in [application.yaml](src/main/resources/application-update.yaml).
As usual, the settings can also be overridden on the command line, for example: `--org.fuin.bupdater.strategy=HIGHEST`.
