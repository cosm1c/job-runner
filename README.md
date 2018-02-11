# Job Runner #

Simple UI which receives streaming updates from jobs running on the server.

Proof of concept for streaming updates from the server over a WebSocket with multiple streams.

Tech Stack:
* [SBT](http://www.scala-sbt.org/) build
* [Scala 2.12](https://www.scala-lang.org/)
* [Akka](http://akka.io/) with [Akka Streams](http://doc.akka.io/docs/akka/current/scala/stream/index.html)
 and [Akka HTTP](http://doc.akka.io/docs/akka-http/current/scala/http/index.html)
* [Swagger](https://swagger.io/)


## SBT Development Environment ##

    sbt run


## SBT Release Command ##

    sbt clean assembly
