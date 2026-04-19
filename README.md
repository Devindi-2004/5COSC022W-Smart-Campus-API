# Smart Campus Sensor & Room Management API (5COSC022W)

## 1. Overview of API Design
The "Smart Campus" API is a high-performance JAX-RS RESTful web service designed to manage a university's infrastructure. The API strictly adheres to RESTful architectural principles, providing a stateless, resource-oriented interface exchanging JSON data.
- **Resource Hierarchy:** The API uses intuitive, noun-based endpoints (e.g., `/rooms`, `/sensors`). Deeply nested resources, such as a sensor's historical data logs, are cleanly handled using the JAX-RS Sub-Resource Locator pattern (`/sensors/{id}/readings`) to maintain encapsulation.
- **Data Integrity & Side Effects:** The API enforces strict business logic to maintain referential integrity. For example, a Room cannot be deleted if it contains active Sensors (`409 Conflict`), and new Sensors must link to a valid Room ID (`422 Unprocessable Entity`). Posting a new SensorReading automatically updates the parent Sensor's current value state.
- **Resilient Error Handling:** The system acts gracefully under failure. It maps application-specific exceptions to precise HTTP status codes with structured JSON error bodies, while a global `ExceptionMapper<Throwable>` cleanly intercepts unexpected crashes to return `500 Internal Server Error`, completely hiding internal server stack traces to prevent cybersecurity information disclosure.
- **Observability:** All network traffic is monitored via custom JAX-RS `ContainerRequestFilter` and `ContainerResponseFilter` layers, automatically logging HTTP request methods, URIs, and status codes.

## 2. Setup & Execution Instructions

**Prerequisites:**
- Java Development Kit (JDK) 11 or higher installed and added to your system PATH.
*(Note: A global Maven installation is not required as this project includes the Maven Wrapper).*

**Step-by-step build and launch instructions (Option: Native Build):**
1. Open a terminal or command prompt in the root directory of this project (where the `pom.xml` file is located).
2. Clean previous builds and compile the Java source code by utilizing the included Maven Wrapper:
   - **On Windows:**
     ```bash
     .\mvnw.cmd clean compile
     ```
   - **On Mac/Linux:**
     ```bash
     ./mvnw clean compile
     ```
3. Launch the embedded Grizzly HTTP container to proxy the JAX-RS application by executing:
   - **On Windows:**
     ```bash
     .\mvnw.cmd exec:java
     ```
   - **On Mac/Linux:**
     ```bash
     ./mvnw exec:java
     ```
4. The server will initialize and start listening for connections on `http://localhost:8080/api/v1/`. Keep this terminal window open to keep the server alive. You can safely stop the API server at any time by pressing the `Enter` key in the terminal.

---

### Easy to Use / Recommended (Option: Docker)
If you do not want to install any Java runtime or configure paths, you can run this entire REST API using Docker.
1. Ensure [Docker Desktop](https://www.docker.com/products/docker-desktop/) is running.
2. In the terminal, build the container image:
   ```bash
   docker build -t smartcampus-api .
   ```
3. Run the containerized API server:
   ```bash
   docker run -p 8080:8080 smartcampus-api
   ```
The API is now running identically at `http://localhost:8080/api/v1/`.

## 3. Sample cURL Commands

Below are five sample cURL commands demonstrating successful standard interactions with the API. Note that the commands are designed to be run in sequence manually (creating a room before creating its corresponding sensor).

**1. Discovery Endpoint (HATEOAS and Metadata):**
```bash
curl -X GET http://localhost:8080/api/v1/
```

**2. Create a targeted "Room" (Returns 201 Created & Location Header):**
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
-H "Content-Type: application/json" \
-d "{\"id\":\"LAB-101\", \"name\":\"Chemistry Lab\", \"capacity\":30}"
```

**3. Register a New Sensor to the Existing "LAB-101" Room:**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
-H "Content-Type: application/json" \
-d "{\"id\":\"CO2-999\", \"type\":\"CO2\", \"status\":\"ACTIVE\", \"currentValue\":400.0, \"roomId\":\"LAB-101\"}"
```

**4. Filter the Sensor Collection by Type (Query Parameters):**
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

**5. Post a New Reading for the Sensor (Updating Parent Entity Value):**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/CO2-999/readings \
-H "Content-Type: application/json" \
-d "{\"value\": 415.5}"
```

---

## 4. Theory Questions & Report Answers

**Q1: How does this Smart Campus API adhere to RESTful principles?**
**Answer:** The API uses standard HTTP methods (`GET`, `POST`, `DELETE`) for CRUD operations, providing a uniform interface. Resources are managed using appropriate nouns (`/rooms`, `/sensors`) rather than verbs. The service is entirely stateless, where each request contains all context needed to execute the transaction, avoiding server-side session persistence. It also uses standard HTTP status codes (`200 OK`, `201 Created`, `409 Conflict`, `422 Unprocessable Entity`) to denote API behavior semantics. Additionally, the discovery endpoint implements HATEOAS by returning `_links` that point clients to available resources without prior knowledge of URL structures.

**Q2: What is the role of JAX-RS (Jersey) and Maven in this project?**
**Answer:** Maven acts as the build automation and dependency management tool. It automatically fetches the required external libraries defined in the `pom.xml` file, avoiding manually tracking JARs, while providing standardized commands (e.g. `mvn compile`). JAX-RS is the Java API specification for building REST Web Services, and Jersey is its official reference implementation. It allows us to easily declare REST endpoints through Java annotations (e.g., `@Path`, `@GET`, `@PathParam`, `@Produces`), automatically handling lower-level servlet mappings, JSON marshalling/unmarshalling, and HTTP request routing.

**Q3: How are Error Handling and Logging implemented to meet enterprise standards?**
**Answer:** We implemented separate layers for robustness:
- Custom Exception Mapper classes (`ExceptionMapper<T>`) intercept specific Java business-logic RuntimeExceptions (`RoomNotEmptyException`, etc.) to dynamically translate them into precise JSON HTTP payload errors with exact status codes (`409`, `422`, `403`). A global catch-all `ExceptionMapper<Throwable>` prevents system stack traces from leaking via a flat `500 Server Error`.
- Network observability was introduced using global Filters (`ContainerRequestFilter` and `ContainerResponseFilter`) acting as middleware to intercept and predictably print inbound URLs/Methods and outbound status codes.