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

**Step-by-step build and launch instructions:**
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