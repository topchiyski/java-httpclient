# Java-HttpClient

[![CI](https://github.com/topchiyski/java-httpclient/actions/workflows/ci.yml/badge.svg)](https://github.com/topchiyski/java-httpclient/actions/workflows/ci.yml)

A small API testing project built on Java's built-in `java.net.http.HttpClient` (Java 11+), with [TestNG](https://testng.org/) for assertions and [SLF4J](https://www.slf4j.org/) for logging. The tests call the public [httpbingo.org](https://httpbingo.org) echo service, so no other HTTP or REST library is needed.

This repo is the companion example for my LinkedIn article: [Zero-Dependency API Testing in Java: Ditch Heavy Libraries for Built-In HttpClient + TestNG](https://www.linkedin.com/pulse/15-second-wait-before-test-1-even-starts-stoyan-topchiyski-qa1pf).

## Tech stack

| Component      | Version  |
|----------------|----------|
| Java           | 21       |
| Maven          | 3.10.1 via the Maven Wrapper (no install needed) |
| TestNG         | 7.10.2   |
| SLF4J (simple) | 2.0.16   |

## Project structure

```
java-httpclient/
├── pom.xml
├── mvnw, mvnw.cmd                 # Maven Wrapper for Linux/macOS and Windows
└── src/test/java/
    ├── base/
    │   └── BaseApiTest.java        # Shared client, constants, and request/logging helpers
    ├── main/
    │   └── Main.java               # Runs all tests from a plain main method
    └── tests/
        ├── SimpleGetTest.java      # Minimal standalone GET example
        ├── CreateUserPostTest.java # POST with JSON body and Bearer token
        ├── HardDeadlineTest.java   # Whole-test deadline with @Test(timeOut = ...)
        ├── HeaderCheckTest.java    # Reading and checking response headers
        └── StatusCheckTest.java    # Data-driven status code checks (@DataProvider)
```

## Core class: `BaseApiTest`

Test classes extend `BaseApiTest`, which provides:

- **`CLIENT`**: one `HttpClient` shared by the whole suite. It follows redirects (`Redirect.NORMAL`) and has a 5 second connect timeout.
- **`BASE_URL`**: `https://httpbingo.org`.
- **`requestTo(endpoint)`**: returns an `HttpRequest.Builder` with the full URL, a 10 second request timeout, and `Content-Type` / `Accept: application/json` headers already set.
- **`sendAndLog(request)`**: sends the request and logs the method, URI, status code, and response body.
- **Status constants**: `HTTP_OK`, `HTTP_UNAUTHORIZED`, `HTTP_NOT_FOUND`.
- **`TEST_TIMEOUT_MS`**: a 15 second hard deadline for `@Test(timeOut = TEST_TIMEOUT_MS)`. The request timeout only covers the status line and headers, so a server that stalls while sending the body could otherwise hang a test forever.

Example:

```java
public class HeaderCheckTest extends BaseApiTest {

    @Test
    public void testResponseHeaders() throws Exception {
        HttpRequest request = requestTo("/get").GET().build();
        HttpResponse<String> response = sendAndLog(request);

        String contentType = response.headers().firstValue("content-type").orElse("");
        Assert.assertTrue(contentType.contains("application/json"));
    }
}
```

## Tests

| Test class           | Endpoint(s)                                  | What it checks |
|----------------------|----------------------------------------------|----------------|
| `SimpleGetTest`      | `GET /get`                                   | Status is 200 and the body is echoed back. Does not use `BaseApiTest`, so it shows the bare client API. |
| `CreateUserPostTest` | `POST /post`                                 | Sends a JSON text block with a Bearer token, then checks for status 200 and that the payload is echoed. |
| `HardDeadlineTest`   | `GET /get`                                   | Status is 200, with the whole test, body included, limited to `TEST_TIMEOUT_MS`. |
| `HeaderCheckTest`    | `GET /get`                                   | `Content-Type` response header contains `application/json`. |
| `StatusCheckTest`    | `GET /status/200`, `/status/401`, `/status/404` | Each endpoint returns the expected status code. Uses `@DataProvider` and `BodyHandlers.discarding()`. |

## Getting started

### Prerequisites

- JDK 21 or newer (CI runs the tests on JDK 21 and 25)
- Internet access (the tests call `https://httpbingo.org`, and the first run downloads Maven and the dependencies)

The Maven Wrapper downloads Maven automatically; a separate Maven installation is not required. On Windows, use `.\mvnw.cmd` instead of `./mvnw`.

### Run all tests

```bash
./mvnw clean test
```

Expected result: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`. If tests fail with connection errors, check that https://httpbingo.org is reachable.

### Run a single test class

```bash
./mvnw test -Dtest=StatusCheckTest
```

### Run from an IDE

In IntelliJ IDEA or Eclipse with the TestNG plugin, open any class under `src/test/java/tests` and run it directly.

### Run from a `main` method (`Main`)

`main.Main` runs all test classes through TestNG's programmatic API from a plain `main` method, then exits with a non-zero code if any test fails. Run it from your IDE, or from the command line:

```bash
./mvnw test-compile exec:java -Dexec.mainClass=main.Main -Dexec.classpathScope=test
```

## Logging

Logging uses `slf4j-simple` and goes to stderr. Each request logs the outgoing method and URI, and each response logs the status code and body, so test output shows the full HTTP exchange.

## Adding a new test

1. Create a class in `src/test/java/tests` that extends `BaseApiTest`.
2. Build a request with `requestTo("/your-endpoint")`.
3. Send it with `sendAndLog(request)`, or with `CLIENT.send(...)` if you need a different body handler.
4. Check the result with TestNG's `Assert` (argument order is `actual, expected`).

## License

This project is licensed under the [MIT License](LICENSE).
