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

### Configuration changes
The configuration was configured for 2 profiles:
* dev - `application-dev.yml`
* prod - `application-prod.yml`

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

![Package organization](data:image/png;base64,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)

Added liquibase-related files in resources:

![Package organization](data:image/png;base64,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)

### Tests

Test are only run on the `dev` profile. 

> For any further information, please contact me on kristaly.dominic@gmail.com


