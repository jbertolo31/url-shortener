URL Shortener
======

## Overview

URL Shortener is just a sample project. A modern Spring WebFlux microservice approach designed with new standards and 
best practises in mind. The project is separated into 3 modules setup for the 3 different types of OAuth2 
implementations: Authorization Server, Resource Server and Client.

Shortening URLs is done by generating a random String and associating it the user's long URL. Nothing fancy, no 
hashing or masking. Caching short URLs is performed to reduce calls to the database. Once a short URL has been visited 
it will be deemed active and will be cached for subsequent lookups.

A 'reveal' page is included that users can use to inspect the original URL incase there are any trust issues with the 
short URL.

A Scheduled maintenance component will attempt to clean up expired short URLs in the cache and database. This ensures
the key is available again for use in the future.

## Authorization Server Module

The OAuth2 Authorization Server. Setup in `application.yml`. Authentication with OpenID Connect, uses JWT. The single 
client allows authorization code flow and client credentials flow. This module is currently not a reactive 
implementation, meaning it runs on Tomcat and not Netty.

## API Module

The OAuth2 Resource Server. A traditonal RESTful API using Spring WebFlux and functional endpoints. Connects to MongoDB 
on the defaut port 27017 and Redis on 6379. Implements [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) 
Problem Details for detailed error responses. Uses Swagger for quick and easy API docs.

## Web Module

The OAuth2 Client. A reactive web BFF (Backend-For-Frontend) to supply up resources. Uses Thymeleaf for a few simple 
pages but setup for React if/when the project needs it.

## Test Coverage

A comprehensive testing framework is included. Integration testing is faciliated with abstract classes wired up to 
connect to MongoDB/Redis TestContainers. A custom 'Repository Populator' class preloads MongoDB documents from JSON 
resources. Full integration testing can be performed with security checks. Annotations to customize the Spring security 
context make testing authorization easy. The project is 100% covered plus some.

## SonarQube

This project is configured to connect to SonarQube for code inspection and analysis.

### API Documentation

Once you have the app running, simply visit http://localhost:8090/api/docs/swagger-ui/webjars/swagger-ui/index.html

## Setup

Requirements:
- Java JDK 17
- Docker
- MongoDB
- Redis

It should be fairly straight-forward to run this project. Simply open with IntelliJ and run. Before running the project 
you will need an available MongoDB and Redis. Testing uses Docker and if run in a pipeline you will need DockerINDocker.

To spin up some docker containers you can run:

```bash
docker run -d --name mongo --publish 27017:27017 mongo:latest
docker run -d --name redis --publish 6379:6379 redis:latest
docker run -d --name sonarqube --publish 9000:9000 --env SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
--env SONAR_FORCEAUTHENTICATION=false sonarqube:latest
```
