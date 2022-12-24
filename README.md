# Issues poc

## Requires

- Docker Engine
    - Use the [Testcontainers](https://www.testcontainers.org/)
    - Recommend [Docker Desktop](https://docs.docker.com/desktop/)
- Java 8+
    - Recommend [Eclipse Adoptium Temurin](https://adoptium.net/temurin/releases/)
    - Set the `JAVA_HOME` environment variable
- sbt
    - Setup `sbt` to your machine
    - https://www.scala-sbt.org/1.x/docs/Setup.html

## Source code

https://github.com/chatwork/aws-sdk-java-v2-issues-poc/blob/main/src/test/scala/example/AwsSdkIssuesPoc.scala

## Exec

Run `sbt test` on your terminal.

Expected result is follows as:
```
[info] welcome to sbt 1.8.0 (Temurin Java 1.8.0_352)
...
...
[info] AwsSdkIssuesPoc:
[info] - The `ResponseSubscription` silently stops when a error has occurred on `Subscriber.onNext` *** FAILED ***
[info]   Expected exception example.AwsSdkIssuesPoc$MyException to be thrown, but java.util.concurrent.TimeoutException was thrown (AwsSdkIssuesPoc.scala:123)
[info] - The `ItemsSubscription silently stops when a error has occurred on `Subscriber.onNext` *** FAILED ***
[info]   Expected exception example.AwsSdkIssuesPoc$MyException to be thrown, but java.util.concurrent.TimeoutException was thrown (AwsSdkIssuesPoc.scala:135)
[info] - The `FlatteningSubscriber` and `MappingSubscriber` tells `Subscriber.onError` the error that occurred in `Subscriber.onNext`.
[info] Run completed in 19 seconds, 243 milliseconds.
[info] Total number of tests run: 3
[info] Suites: completed 1, aborted 0
[info] Tests: succeeded 1, failed 2, canceled 0, ignored 0, pending 0
[info] *** 2 TESTS FAILED ***
[error] Failed tests:
[error] 	example.AwsSdkIssuesPoc
[error] (Test / test) sbt.TestsFailedException: Tests unsuccessful
...
```
