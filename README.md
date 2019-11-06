# Rutebanken-helpers [![CircleCI](https://circleci.com/gh/entur/rutebanken-helpers/tree/master.svg?style=svg)](https://circleci.com/gh/entur/rutebanken-helpers/tree/master)

Java code which can be re-used in our different java projects.


## Building the libraries
Some modules depend on [Google PubSub Emulator](https://cloud.google.com/pubsub/docs/emulator) for running unit tests.

[The emulator is not available as a Maven dependency in any public Maven repository](https://stackoverflow.com/questions/38447415/google-pubsub-emulator-maven-repo).
Options to deal with this dependency include:

1. Adding the emulator to your local Maven proxy under the following Maven GAV:
```
<groupId>com.google.cloud</groupId>
<artifactId>pubsub-emulator</artifactId>
<version>0.1-SNAPSHOT</version>
```
2. Checking in the emulator under source control.

The automatic downloading of the emulator can be deactivated in the Maven build by setting the following property:
```
mvn clean install -Dentur.google.pubsub.emulator.download.skip=true
```
A Spring property must then be set to point to a custom location for the emulator:
```
entur.pubsub.emulator.path=/path/to/emulator/pubsub-emulator-0.1-SNAPSHOT.jar
```
3. Disabling altogether unit tests during the build process:
```
mvn clean install -DskipTests -Dentur.google.pubsub.emulator.download.skip=true
```
