# Weathervane API
---
REST API for retrieving and persisting weather information

_Author: Dominic M. KRISTALY (kristaly.dominic@gmail.com)_ 

## Introduction
This is a simple application that requests its data from [OpenWeather](https://openweathermap.org/) and stores the result in a PostgreSQL database.

## Assumptions

* The API does not need a cache; each call to the API must retrieve and save the data to the database
* The API needs a minimal security layer (an API Key)

## How to use the application
### How to build this application
#### Requirements
To build this application, the following programs are needed to be installed:
* [Java JDK - Version 11](https://www.oracle.com/ro/java/technologies/javase-jdk11-downloads.html) - free Oracle account needed 
* [Apache Maven](https://maven.apache.org/download.cgi)
* [PostgreSQL version 10 or superior](https://www.postgresql.org) or access to a PostgreSQL instance (the user should have rights to create tables)

After you get the project from Git, open a shell window, navigate to the project root directory and execute the following command:

```
mvn clean
```
#### Application configuration
Create `weather` database in the PostgreSQL instance.

Edit the `application-prod.yml` file into the `src/main/resources/` directory. Change the following keys with your PostgreSQL information:

```
...
datasource:
  url: jdbc:postgresql://<postgresql-ip>:<postgresql-port>/weather
  driver-class-name: org.postgresql.Driver
  username: <username>
  password: <password>
  ...
```
#### Building the application
Open a shell window, navigate to the project root directory and execute the command:

```
mvn clean install
```
### Running the application 
In a shell window, from the root directory execute the command:

```
java -jar target/weathervane-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

It is possible to use an external properties (application*.yml) file (copied from `src/main/resources/application-prod.yml`), but then it must be sent as parameter:

```
java -jar target/weathervane-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod --spring.config.location=file:///<name-and-location-of-properties-file>
```

### How to use the application
#### From a shell window
To use the application you can use Postman (or curl from a shell window) with the following cURL:

```
curl -X GET \
  'http://127.0.0.1:8080/api/v1/fetch-weather?city={CITY_NAME}' \
  -H 'cache-control: no-cache' \
  -H 'x-api-key: 4VHZLCpYFB47P6u2jdbpNX7pGdruNXDo'
```

`{CITY_NAME}` must be replaced with the desired city.

**Request & response sample**

_Request:_
```
curl -X GET \
  'http://127.0.0.1:8080/api/v1/fetch-weather?city=brasov' \
  -H 'cache-control: no-cache' \
  -H 'x-api-key: 4VHZLCpYFB47P6u2jdbpNX7pGdruNXDo'
```

_Response:_
```json
{
"city": "Braşov",
"country": "RO",
"temperature": 285.01,
"timestamp": "2021-09-09 22:22:43.063"
}
```
#### From Swagger
You can also use Swagger from a browser:

```
http://127.0.0.1:8080/swagger-ui.html
```

## Development and architectural decisions

### Changes in dependencies and tools
* Spring Boot Version upgraded to 2.3.12.RELEASE
* Replaced WebMVC with Webflux
* Added `jackson-datatype-jsr310` (for correct serialization of LocalDateTime)
* Added liquibase (the required table is created automatically)
* Added Sleuth (for tracing purposes - Zipkin can be configured)
* Added `reactor-test` for testing on webflux
* Added lombok
* Added Swagger (`springdoc-openapi-webflux-ui`)
* Added Spring Actuator
* Added Spring AOP
* Added `micrometer-registry-prometheus` (for timing the duration of a call)
* Added `spring-boot-devtools`
* Added H2 database for tests only

### Configuration changes
The configuration was configured for 2 profiles:
* dev - `application-dev.yml`
* prod - `application-prod.yml`
* test - `application-test.yml` (in test)

#### File `application-dev.yml` with comments
```yaml
# allows selective log levels for different profiles
logging:
  level: 
    ROOT: INFO
    com.assignment.spring: INFO

spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/weather
    username: postgres
    password: user
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          lob:
            non_contextual_creation: true
  liquibase:
    change-log: classpath:liquibase/master.xml
# Sleuth can be linked to a Zipkin server    
#  zipkin:
#    baseUrl: <TBD>

# configuring actuators/AOP/Prometheus
management:
  server:
    port: 8080
  endpoints:
    web:
      exposure:
        include: '*'
    health:
      sensitive: false
    metrics:
      enabled: true
    prometheus:
      enabled: true
    export:
      prometheus:
        enabled: true

springdoc:
  swagger-ui:
    operationsSorter: method

weather-server:
  url-base: http://api.openweathermap.org
  url-ep: /data/2.5/weather
  app-id: 0dfbe2718bf191168a9120f515982533
  # the webclient will retry up to 3 times in case the OpenWeatherMap API doesn't respond 
  retries: 3
  # the timeout for the call of the OpenWeatherMap API 
  timeout: 5000

# the API Key -based security settings for the Wathervane API
api-security:
  api-key-header-name: x-api-key
  api-key-secret: CF3U3tJXNd2FNTZxt7aFVkQQiLOJm6pG
```

### Development details

Lombok was use throughout the project, as needed.

Improved existing entities (JPA and models). 

Dependency injection is done through the use of constructors (used lombok's `@RequiredArgsConstructor` for this purpose).

The settings for the OpenWeatherMap API and for security are kept in separate classes (`config` package).

To be able to time the API call duration, a bean for Micrometer is declared in `AppConfig` class.

The entities for responses (successful call or errors) are employed solely for this purpose.

The errors are encapsulated in two types of custom exception (`CityNotFoundException` and `GenericApiException`).

Added the API Key filter that checks the headers of each request for the API Key. The solution was chosen only because of its simplicity.

Added a WebClient bean for the OpenWeatherMap API (package `webclient`).

The `WeatherController` class is the REST controller and includes the Swagger documentation annotations.

Created 2 services (interface+implementation):
* one for calling the OpenWeatherMap API
* one for implementing the rest of the logic and handling errors


### Error handling

The errors are propagated using `Mono.error()`. The `WeatherService` handles the errors and converts them into `ResponseEntity` objects to be returned to the controller.

For the calls with missing or wrong API Key, the ApiKeyFilter will return HTTP 401.

If the city name is not found, HTTP 404 is returned and, supplementary, a custom JSON object is also returned (see `ErrorResponse` class).

If any other error occurs, HTTP 500 is returned; the body will contain custom JSON object (see `ErrorResponse` class).

For more details on the error JSON object, please use Swagger:

```
http://127.0.0.1:8080/swagger-ui.html
```

### Project organization
Split the code into several packages:

```
src
├───main
│   ├───java
│   │   └───com
│   │       └───assignment
│   │           └───spring
│   │               │   WeatherApplication.java
│   │               │
│   │               ├───api
│   │               │       WeatherController.java
│   │               │
│   │               ├───config
│   │               │       ApiSecurityConfig.java
│   │               │       AppConfig.java
│   │               │       WeatherServerConfig.java
│   │               │
│   │               ├───dto
│   │               │       ErrorResponse.java
│   │               │       WeatherResponse.java
│   │               │
│   │               ├───exception
│   │               │       CityNotFoundException.java
│   │               │       GenericApiException.java
│   │               │
│   │               ├───persistence
│   │               │   ├───entities
│   │               │   │       WeatherEntity.java
│   │               │   │
│   │               │   └───repositories
│   │               │           WeatherRepository.java
│   │               │
│   │               ├───security
│   │               │       ApiKeyFilter.java
│   │               │
│   │               ├───service
│   │               │   │   OpenWeatherMapService.java
│   │               │   │   WeatherService.java
│   │               │   │
│   │               │   └───impl
│   │               │           OpenWeatherMapServiceImpl.java
│   │               │           WeatherServiceImpl.java
│   │               │
│   │               └───webclient
│   │                   │   WeatherApiWebClient.java
│   │                   │
│   │                   └───model
│   │                           Clouds.java
│   │                           Coord.java
│   │                           Main.java
│   │                           Sys.java
│   │                           Weather.java
│   │                           WeatherApiResponse.java
│   │                           Wind.java
│   │
│   └───resources
│       │   application-dev.yml
│       │   application-prod.yml
│       │   application.yml
│       │
│       └───liquibase
│           │   master.xml
│           │
│           └───changelog
│                   20210908_weather_table_init.xml
│
└───test
    ├───java
    │   └───com
    │       └───assignment
    │           └───spring
    │                   FlowsTests.java
    │                   WeatherApplicationTests.java
    │
    └───resources
            application-test.yml
```

### Tests

Test are run on the `test` profile. Tests cover the "happy flow" and all handled errors (missing city, missing or wrong API Key, city not found).

To run tests, open a shell window, navigate to the project root directory and execute the command:

```
mvn test
```

> For any further information, please contact me on kristaly.dominic@gmail.com


