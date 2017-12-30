[![Build Status](https://travis-ci.org/htchepannou/oms-service.svg?branch=master)](https://travis-ci.org/htchepannou/oms-service)
[![Code Coverage](https://img.shields.io/codecov/c/github/htchepannou/oms-service/master.svg)](https://codecov.io/github/htchepannou/oms-service?branch=master)
[![JDK](https://img.shields.io/badge/jdk-1.8-brightgreen.svg)](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)


## Order Manager System
This service manages all orders

## Requirements
- Java 1.8
- Maven
- MySQL


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

### Run the server locally
```
$ java -Dspring.profiles.active=local -jar target/oms-service.jar
```
The server will run locally on the port `18082`
- Verify the status of the service at [http://localhost:28080/health](http://localhost:28080/health). The status should be `UP`. 
- Access the API documentation at [http://localhost:28080/swagger-ui.html](http://localhost:28080/swagger-ui.html) 

## License
This project is open source sofware under the [MIT License](https://opensource.org/licenses/MIT)
