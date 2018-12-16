# dockerized-example-services

[![Build Status](https://travis-ci.org/nebtrx/dockerized-example-services.svg?branch=master)](https://travis-ci.org/nebtrx/dockerized-example-services)

This repo is a sample app showing how to dockerize [Scala](https://www.scala-lang.org/) 
microservices in a [monorepo](https://en.wikipedia.org/wiki/Monorepo). 

The solution consists in a simple `common` library containing shared type definitions, 
a `producer` service pulling status updates from [Twitter](https://twitter.com) and 
pumping those, every one second, to a `consumer` web app through [Rabbit](https://www.rabbitmq.com/) hole, 
cough..I mean, queue. The web app renders the incoming status data in real time. 

## Development

This solution uses a pure FP approach, a bunch of streams, and some of 
the greatest libraries in the FP Scala ecosystem like:

1. [cats](https://typelevel.org/cats/)
2. [cats-effects](https://typelevel.org/cats-effect/)
3. [fs2](http://fs2.io/)
4. [http4s](https://http4s.org/)
4. [fs2-rabbit](https://gvolpe.github.io/fs2-rabbit/)

## Try it out locally

**Note**: This project relies on having [Docker](https://docs.docker.com/install/) installed 
on your system

1. Clone this repo and `cd` into it
 
```bash
$ git clone git@github.com:nebtrx/dockerized-example-services.git

$ cd  dockerized-example-services
```

2. Build the project and create the containers' images

```bash
$ sbt docker:publishLocal
```

5. Copy the sample configuration file and edit it to match your configuration 
so your docker can pick up the env vars

```bash
$ cp .env.example .env
```

4. Create and start the containers

```bash
$ docker-compose up
```

5. Check [http://localhost:8080/stream](http://localhost:8080/stream) on your browser and 
watch the magic take place

6. Optionally, you can check [http://localhost:15672](http://localhost:15672) to access 
`rabbitmq` console    


## License
This project is licensed under the [MIT License](LICENSE.md).
