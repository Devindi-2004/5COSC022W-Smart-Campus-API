# Smart Campus Sensor & Room Management API (5COSC022W)

## 1. Overview
The "Smart Campus" API is a JAX-RS RESTful web service designed to manage campus infrastructure. It tracks `Rooms` and a diverse array of `Sensors` (e.g., CO2, Occupancy, Temperature) associated with those rooms, including timestamped `SensorReadings`.

## 2. Setup & Execution Instructions

**Prerequisites:**
- Java Development Kit (JDK) 11 or higher
- Apache Maven

**Step-by-step build and run:**
1. Open a terminal in the root directory (where `pom.xml` is located).
2. Clean and compile the project by running:
   ```bash
   mvn clean compile
   ```
3. Run the embedded Grizzly HTTP server:
   ```bash
   mvn exec:java
   ```
4. The server will start at `http://localhost:8080/api/v1/`. Leave the terminal open to keep the server running. Press `Enter` in the terminal to stop the server safely.

## 3. Sample cURL Commands

**1. Get Discovery Endpoint:**
```bash
curl -X GET http://localhost:8080/api/v1/
```

**2. Create a New Room:**
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
-H "Content-Type: application/json" \
-d '{"id":"LAB-101", "name":"Chemistry Lab", "capacity":30}'
```

**3. Get all Sensors of type "Temperature":**
```bash
curl -X GET http://localhost:8080/api/v1/sensors?type=Temperature
```

**4. Register a New Sensor to an Existing Room:**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
-H "Content-Type: application/json" \
-d '{"id":"CO2-999", "type":"CO2", "status":"ACTIVE", "currentValue":400.0, "roomId":"LIB-301"}'
```

**5. Post a new Reading for a Sensor:**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
-H "Content-Type: application/json" \
-d '{"value": 24.5}'
```

---

## 4. Coursework Questions & Answers (Conceptual Report)

### Part 1: Service Architecture & Setup
**Q1: In your report, explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.**
**Answer:** By default, JAX-RS treats Resource classes as "per-request." This means a brand new instance of the Resource class is instantiated for every incoming HTTP request and is garbage collected afterward. Because of this architectural decision, we cannot rely on instance variables within the Resource class to persist data between requests. To prevent data loss, the data structures handling our state (maps/lists) must be static or injected singletons. Furthermore, because multiple requests (and thus multiple threads) can access the data concurrently, race conditions can occur. To handle this, we must use thread-safe data structures like `ConcurrentHashMap` or synchronize our access methods, ensuring thread safety and data integrity even as request volumes surge.

**Q2: Why is the provision of ”Hypermedia” (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?**
**Answer:** Hypermedia as the Engine of Application State (HATEOAS) is the highest level of REST maturity (Richardson Maturity Model Level 3). It embeds navigational links within the JSON responses (e.g., `_links`), telling the client exactly what state transitions are currently available. It benefits client developers by decoupling them from hardcoded URI structures; clients navigate the API dynamically just as a human navigates a website by clicking links. This allows the backend to evolve URL structures, permission access, and logic without breaking the frontend, making the API significantly more resilient and self-describing than static documentation.

### Part 2: Room Management
**Q3: When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.**
**Answer:** Returning only IDs drastically reduces the payload size, saving significant network bandwidth, which is critical for mobile devices or highly concurrent systems. However, this forces the client to make multiple follow-up HTTP requests to fetch details for each room (the N+1 query problem), adding latency and shifting processing burden to the client-side. Returning full room objects increases the payload size and bandwidth consumption initially, but it minimizes the number of HTTP requests required. A trade-off is often used, such as pagination or returning a "summary" object with basic details and a hyperlink to fetch the full object if necessary.

**Q4: Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.**
**Answer:** Yes, the DELETE operation is idempotent. Idempotency means that making multiple identical requests has the same effect on the server state as making a single request. In our implementation, the first time a client sends a DELETE request for a specific room ID (assuming the room is empty of sensors), the room is successfully removed. If the client mistakenly sends the exact same DELETE request a second or third time, the server will correctly return a `404 Not Found` because the resource no longer exists. Crucially, the *server state* does not change beyond the outcome of the first request, fulfilling the definition of an idempotent operation.

### Part 3: Sensor Operations & Linking
**Q5: We explicitly use the @Consumes(MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?**
**Answer:** The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells the JAX-RS runtime that the endpoint only accepts payloads formatted as JSON. If a client attempts to send data utilizing a different `Content-Type` header (e.g., `text/plain` or `application/xml`), the JAX-RS runtime intercepts the request before it even reaches the Java method block. JAX-RS immediately handles this mismatch by returning an HTTP `415 Unsupported Media Type` response to the client. This protects the resource from attempting to deserialize formats it doesn't understand.

**Q6: You implemented this filtering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path (e.g., /api/v1/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections?**
**Answer:** URL paths (e.g., `/api/v1/sensors/TEMP-001`) are meant to identify specific, unique resources or hierarchical subdivisions. Query parameters (e.g., `?type=CO2`) are meant to provide variables to modify the outcome of fetching a collection. Using `@QueryParam` is considered superior for filtering because filters are often optional and combinable (e.g., `?type=CO2&status=ACTIVE`). If you embed filters in the URL path, the route architecture becomes extremely rigid, restrictive, and difficult to manage when multiple independent filters exist. Query parameters allow for flexible sorting, filtering, and pagination over the base `/sensors` collection resource.

### Part 4: Deep Nesting with Sub-Resources
**Q7: Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive controller class?**
**Answer:** The Sub-Resource Locator pattern allows a parent root resource to match a partial URI path and delegate the handling of the remainder of the path to a secondary, nested class. The architectural benefit is profound separation of concerns. Instead of building one massive "god class" handling hundreds of endpoints for `Sensors`, `SensorReadings`, `SensorLogs`, etc., the logic is encapsulated inside dedicated, specialized classes. This keeps file sizes manageable, allows multiple development teams to work on different nested resources without merge conflicts, makes unit testing significantly easier by isolating dependencies, and enables the reuse of the Sub-Resource class across multiple parent resource paths if necessary.

### Part 5: Advanced Error Handling
**Q8: Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?**
**Answer:** A `404 Not Found` typically indicates that the target URI itself does not correspond to an existing resource on the server (e.g., calling GET on a non-existent sensor). In contrast, if a client sends a POST to a valid endpoint (like `/api/v1/sensors`) with a valid JSON payload containing a data relation that breaks business rules (e.g., a `roomId` that doesn't exist), the target endpoint is technically valid, and the syntax is correct. `422 Unprocessable Entity` is the semantically accurate status code because it indicates the server understands the content type and the syntax is valid, but the server is unable to process the contained semantic instructions (the invalid dependency linkage).

**Q9: From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?**
**Answer:** Returning a raw stack trace exposes internal implementation details and the technological topography of the server to external consumers. This is a severe Information Disclosure vulnerability (CWE-200). A stack trace reveals the exact versions of frameworks being used (e.g., Hibernate, Jersey, Grizzly), the underlying database driver versions, class architectures, and precise file paths on the host system. Attackers use this blueprint to craft targeted exploits matching known CVEs for those specific library versions or file structures, vastly increasing the speed and success rate of exploitation.

**Q10: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?**
**Answer:** Using JAX-RS filters for logging promotes the DRY (Don't Repeat Yourself) principle and Separation of Concerns. Cross-cutting concerns—like logging, authentication, and CORS—apply globally. If we manually insert `Logger.info()` inside every resource method, it clutters the business logic with infrastructural code. Furthermore, it is highly error-prone; a developer might forget to add the logging statement when creating a new endpoint in the future. By using a `ContainerRequestFilter` and `ContainerResponseFilter`, the JAX-RS runtime intercepts all traffic centrally and automatically, guaranteeing 100% coverage for all endpoints while keeping the actual endpoint methods clean and focused solely on business logic.
