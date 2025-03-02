# Zito Restaurant Ordering API
This is a RESTful API for managing a restaurant, built using Spring Boot and MongoDB.

## Requirements
- Java 11+
- MongoDB 4+

## Installation
1. Clone the repository: `git clone https://github.com/kotsoc/Zito.git`
2. Navigate to the project directory: `cd zito`
3. Build the project: `mvn clean package`
4. Run Mongodb replacing the path to the one that corresponds to your db: `mongod -dbpath ~/data/db`
4. Start the server: `java -jar target/zito-0.0.1-SNAPSHOT.jar`

By default, the server will listen on port 8080. You can change this by setting the `server.port` property in `application.properties`.

## Running from a container
1. Update the version number
2. run `mvn compile jib:build`
3.

## Usage
The API exposes several endpoints for managing menu items, orders, waiters, and tables. See the Swagger UI documentation at `http://localhost:8080/swagger-ui.html` for more information.

## Configuration
You can configure the database connection by setting the `spring.data.mongodb.uri` property in `application.properties`. Other properties, such as the database name, can be set as well.


## License
This project is licensed under the AGPL License. See LICENSE for details.