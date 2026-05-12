# vex-sdk-java

Java 21 SDK for writing VexEdge function containers and gate handlers.

Published to GitHub Packages as `io.vextura:vex-sdk-java`.

## Requirements

- Java 21+
- Gradle 8+ (recommended) or Maven 3.8+

## Installation

### Step 1 — GitHub personal access token

Go to https://github.com/settings/tokens → **Generate new token (classic)** → tick **`read:packages`** → copy the token.

### Step 2 — Add credentials

**Gradle** — add to `~/.gradle/gradle.properties`:
```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.token=YOUR_GITHUB_TOKEN
```

**Maven** — add to `~/.m2/settings.xml`:
```xml
<settings>
  <servers>
    <server>
      <id>vextura-github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

### Step 3 — Add repository + dependency

**Gradle** (`build.gradle`):
```groovy
repositories {
    maven {
        url = uri('https://maven.pkg.github.com/vextura/vex-sdk')
        credentials {
            username = project.findProperty('gpr.user') ?: System.getenv('GITHUB_USERNAME')
            password = project.findProperty('gpr.token') ?: System.getenv('GITHUB_TOKEN')
        }
    }
}

dependencies {
    implementation 'io.vextura:vex-sdk-java:1.0.0'
}
```

**Maven** (`pom.xml`):
```xml
<repositories>
  <repository>
    <id>vextura-github</id>
    <url>https://maven.pkg.github.com/vextura/vex-sdk</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>io.vextura</groupId>
    <artifactId>vex-sdk-java</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

## Quick start

```java
import io.vextura.sdk.vexfn.VexFn;
import io.vextura.sdk.vexfn.VexResponse;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        VexFn.run((event, ctx) -> {
            String txId   = (String) event.inputData().get("transactionId");
            double amount = (double) event.inputData().getOrDefault("amount", 0.0);

            return VexResponse.ok(
                event.eventId(),
                Map.of("approved", true, "authCode", "AUTH-" + txId),
                0L
            );
        });
    }
}
```

## Calling other functions (PrimitivesClient)

```java
String result = ctx.primitives().call("kyc-check", "{\"customerId\":\"c-001\"}");
```

## Deploy

```bash
vexctl fn deploy --name process-payment --image ghcr.io/vextura/process-payment:latest
```

## Smithy source

Types in this SDK are aligned with `smithy/traits/vexfn.smithy`.
