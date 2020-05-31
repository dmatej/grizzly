# Grizzly NIO

Writing scalable server applications in the Java™ programming language
has always been difficult. Before the advent of the Java New I/O API (NIO),
thread management issues made it impossible for a server to scale to
thousands of users. The Grizzly NIO framework has been designed to help
developers to take advantage of the Java™ NIO API. Grizzly’s goal is to
help developers to build scalable and robust servers using NIO as well
as offering extended framework components: Web Framework (HTTP/S),
WebSocket, Comet, and more!

## Getting Started

Grizzly currently has several lines of development in the following
branches:

- EE4J_8 The historical branch used to create the Grizzly version for Jakarta EE 8/GlassFish 5.1
- 2.4.4-RELEASE : This is the sustaining branch for 2.x.
- master : Currently in flux, will be updated later
- 3.0.0: The Jakarta-ized version of Jersey. Release 3.0.0-M1 has been created from this branch.



### Prerequisites

We have different JDK requirements depending on the branch in use:

- Oracle JDK 1.8 for master and 3.0.x branches.
- Oracle JDK 1.7 for 2.3.x.

Apache Maven 3.3.9 or later in order to build and run the tests.

### Installing

See https://javaee.github.io/grizzly/dependencies.html for the maven
coordinates of the 2.3.x release artifacts.

If building in your local environment:

```
mvn clean install
```


## Running the tests

```
mvn clean install
```

## License

This project is licensed under the EPL-2.0 - see the [LICENSE.txt](https://github.com/eclipse-ee4j/grizzly/blob/master/LICENSE.txt) file for details.

