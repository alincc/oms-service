[![Build Status](https://travis-ci.org/htchepannou/oms-service.svg?branch=master)](https://travis-ci.org/htchepannou/oms-service)
[![Code Coverage](https://img.shields.io/codecov/c/github/htchepannou/oms-service/master.svg)](https://codecov.io/github/htchepannou/oms-service?branch=master)
[![JDK](https://img.shields.io/badge/jdk-1.8-brightgreen.svg)](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)


## Order Manager System
This service manages all orders

## Requirements
- Java 1.8
- Maven
- MySQL


## Downstreams
- [ferari-service](https://www.github.com/htchepannou/ferari-service)
- [tontine-service](https://www.github.com/htchepannou/tontine-service)

## Installation
### Setup IDE
#### Install Lombok plugin

##### IntelliJ IDE
Menu `preferences > plugins`, type `Lombok` in the search field then click Install

##### Eclipse IDE
The installation procedure can be found [here](https://projectlombok.org/setup/eclipse)

### Database Installation
Initialize the local database
```
$ mysql -uroot
...
mysql> CREATE DATABASE oms;
```

Clone the code repository locally and build it.
```
$ git clone git@github.com:htchepannou/oms-service.git
$ cd oms-service
$ mvn clean install
```

This will generate the service binary ``target/oms-service.jar``

### Run the server
This allow to run the server locally on port `8080`, using the local database and calling downstream services that are running remotely at Heroku.

```
$ java -jar target/oms-service.jar
```


### Run the server locally
This allow to run the server locally on port `18083`, using the local database and calling downstream services that are running on the locally.

- Install and run all downstream services locally:
  - [ferari-service](https://github.com/htchepannou/ferari-service#run-the-server-with-local-profile)
  - [tontine-service](https://github.com/htchepannou/tontine-service#run-the-server-with-local-profile)
  
- Run the server with `local` profile:
```
$ java -Dspring.profiles.active=local -jar target/oms-service.jar
```

## Licens
This project is open source sofware under the [MIT License](https://opensource.org/licenses/MIT)
