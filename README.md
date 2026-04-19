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

## 4. Conceptual Report & Theory Answers

**Part 1.1: Default Lifecycle of a JAX-RS Resource Class**
**Question:** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.
**Answer:** By default, JAX-RS resource classes are instantiated per-request (request-scoped). A new instance is dynamically created to handle each incoming HTTP request and is destroyed immediately after the response is sent. Because of this architectural decision, instance variables cannot be used to store persistent data across multiple client requests. To maintain state without a database, we must manage our in-memory data structures (like `HashMap` datasets) at the application level—typically by using `static` variables or navigating the Singleton pattern (e.g., our `DataStore` class). Since multiple concurrent requests (threads) access this shared `static` memory, we must ensure these structures use thread-safe primitives (like `ConcurrentHashMap`) or explicit synchronization blocks to prevent race conditions and data corruption.

**Part 1.2: The "Discovery" Endpoint & HATEOAS**
**Question:** Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?
**Answer:** HATEOAS (Hypermedia as the Engine of Application State) enables a REST API to guide the client dynamically by embedding URL links (e.g., `_links`) to related resources directly into the JSON response. This is a hallmark of advanced Richardson Maturity Model (Level 3) applications because it decouples the client from the server's internal routing structure. Client developers no longer need to hardcode absolute endpoint paths relying on static PDF documentation; instead, the client application can organically "discover" available actions and nested relationships at runtime. If the server decides to change a URL path in the future, the client naturally adapts without breaking, fostering severe API flexibility and resilience.

**Part 2.1: Room Management Collection Retrieval**
**Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.
**Answer:** Returning *only IDs* significantly reduces the initial JSON payload size, preserving server bandwidth and speeding up the initial transfer. However, it negatively affects client-side processing by causing the "N+1 query problem"—the client is forced to execute a flood of subsequent, high-latency `GET /{roomId}` HTTP requests to fetch meaningful data for each ID to paint a UI. Conversely, returning the *full room objects* increases the initial payload size (which can become heavy at scale without pagination), but drastically reduces the total number of HTTP connections the client must establish. This eliminates recursive network latency overhead and allows the client side to render the dataset immediately.

**Part 2.2: DELETE Idempotency**
**Question:** Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.
**Answer:** Yes, the fundamental nature of the `DELETE` operation remains idempotent in our implementation. If a client sends a `DELETE` request for an empty valid room (e.g., `EMPTY-101`), the server successfully removes the entity and returns an HTTP `204 No Content`. If the client mistakenly sends the *exact same* `DELETE` request again a minute later, the server logic will identify that the resource no longer exists and return an HTTP `404 Not Found`. Even though the HTTP response *status code* differs computationally, the overarching *state of the server system* remains completely unchanged (the target room continues to not exist). Therefore, repeating the operation multiple times produces the exact same end state as executing it once, fully satisfying the definition of REST protocol idempotency.

**Part 3.1: Sensor Resource & Media Types**
**Question:** We explicitly use the `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as `text/plain` or `application/xml`. How does JAX-RS handle this mismatch?
**Answer:** By explicitly declaring `@Consumes(MediaType.APPLICATION_JSON)`, we strictly instruct the JAX-RS runtime provider (Jersey) to only accept requests mapped with the HTTP header `Content-Type: application/json`. If a client misconfigures their request and attempts to send an XML or plaintext payload, the JAX-RS router framework intercepts the request at the entry boundary before it even touches our `SensorResource` logic method. Jersey elegantly detects the mismatch and automatically rejects the request natively, generating and returning an HTTP `415 Unsupported Media Type` status response to the client immediately—guaranteeing that our application logic doesn't crash from an unreadable payload string.

**Part 3.2: Filtered Retrieval & Search Parameters**
**Question:** You implemented this filtering using `@QueryParam`. Contrast this with an alternative design where the type is part of the URL path (e.g., `/api/v1/sensors/type/CO2`). Why is the query parameter approach generally considered superior for filtering and searching collections?
**Answer:** Sub-URL path parameters (e.g., `/{id}`) denote a strict structural, hierarchical identifier pointing to a single explicit resource entity or sub-directory. Conversely, query parameters (e.g., `?type=CO2`) act as optional filters mathematically applied across a base collection. The query parameter array approach is vastly superior for search functionality because it allows filtering dimensions to be naturally optional (returning all sensors if omitted) and easily combinable (e.g., `?type=CO2&status=ACTIVE`). Embedding filters into paths leads to rigid, combinatorial explosions of URL routing definitions (e.g. `/type/{t}/status/{s}`) which creates brittle and unintuitive monolithic endpoint designs.

**Part 4.1: Sub-Resource Locator Pattern**
**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., `sensors/{id}/readings/{rid}`) in one massive controller class?
**Answer:** The Sub-Resource Locator acts as a traffic router. By binding `@Path("/{sensorId}/readings")` to a method that merely returns a brand new instance of `SensorReadingResource`, we cleanly delegate the request context (the current `sensorId`) over to isolated class architecture. This fulfills the Single Responsibility Principle—keeping the primary `SensorResource` exclusively focused on managing Parent lifecycle configurations while shifting all complex chronological tracking responsibilities exclusively to `SensorReadingResource`. In massive enterprise APIs, combining all nested depth definitions inside one monolithic controller class leads to bloated thousand-line files that are notoriously difficult to version control (merge conflicts), debug, scale, and test independently.

**Part 5.2: Dependency Validation Exception Mapping**
**Question:** Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?
**Answer:** An HTTP `404 Not Found` implies that the target URI endpoint mapping *itself* physically does not exist on the server (e.g., a bad typo in the URL path). When a client sends a `POST` request to create a Sensor pointing to a non-existent `roomId` foreign key, the endpoint domain (`/api/v1/sensors`) *does* successfully exist, and the JSON payload syntax is perfectly formatted. Syntactically it is correct; however, the business engine fails to process it because it contains semantically invalid relationship data. HTTP `422 Unprocessable Entity` exactly defines this condition: the server understands the content-type and payload structure, but the enclosed instruction fails strict referential business logic validations.

**Part 5.4: Cybersecurity & Internal Stack Traces**
**Question:** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?
**Answer:** Exposing raw Java stack traces (like `NullPointerException` traces cascading from `org.glassfish.jersey...`) provides malicious actors with explicit internal reconnaissance on the system’s backend architecture—known as Information Disclosure. Hackers uniquely scrape this trace to map internal package/class structures, identify the specific backend framework running (Jersey/Grizzly), deduce precisely which Java versions or third-party dependency libraries are utilized, and expose exact file directory execution layers. Armed with this map, attackers can cross-reference the leaked third-party library names with open CVE (Common Vulnerabilities and Exposures) databases to pivot toward highly targeted, library-specific zero-day remote code exploitation attacks. By intercepting these exceptions mathematically and collapsing them into flat, generic `500 Server Error` JSON models (using `ExceptionMapper<Throwable>`), we entirely obscure this blueprint.

**Part 5.5: Global Logging Filters vs. Boilerplate Methods**
**Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting `Logger.info()` statements inside every single resource method?
**Answer:** Using `ContainerRequestFilter` and `ContainerResponseFilter` implements an Aspect-Oriented Programming (AOP) methodology. Logging is a "cross-cutting concern"—a requirement logically spanning the entire application. Injecting `Logger.info()` inside every single REST transaction manually causes atrocious code duplication, introduces human error (developers forgetting to embed the logging line in newly created endpoints), and clutters critical business logic methods with irrelevant system tooling code. A global filter abstracts this into a singular architectural layer: guaranteeing 100% observability coverage across the entire system automatically, centralizing log-structure formatting decisions to one file, and keeping all target endpoint code pristinely focused solely on domain business directives.