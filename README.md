# Serqo Java SDK

Official Java client for the Serqo Search API. Requires Java 11 or later. Jackson Databind is used to encode requests and preserve the flexible search response schema.

## Maven

```xml
<dependency>
  <groupId>io.serqo</groupId>
  <artifactId>serqo-java</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Quick start

```java
import io.serqo.sdk.SerqoClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

SerqoClient client = new SerqoClient(System.getenv("SERQO_API_KEY"));
JsonNode response = client.search(Map.of(
    "q", "espresso machines",
    "engine", "google",
    "type", "web",
    "gl", "us",
    "hl", "en"
), null);

JsonNode results = response.path("data").path("results");
System.out.println("Request ID: " + response.path("requestId").asText());
```

The returned `JsonNode` is the full response envelope. Keep the API key in server-side configuration.

## Methods and errors

- `search(query)` sends `POST /v1/search`; `search(query, idempotencyKey)` adds an idempotency key for a safe retry.
- `engines()`, `me()`, and `health()` call the corresponding public endpoints.
- API failures throw `SerqoException` with status, code, request ID, retryability, retry delay, and details.
- Searches are not retried automatically. Reuse an idempotency key only with identical parameters.
- See the [shared API reference](API.md) for all search parameters, response fields, and pagination behavior.
