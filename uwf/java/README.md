# io.vextura:vextura-sdk — Java 21

Vextura Workflow Engine client SDK for Java 21+.

Source of truth: `api/workflow-api.smithy`

## Install (Maven)

```xml
<dependency>
  <groupId>io.vextura</groupId>
  <artifactId>vextura-sdk</artifactId>
  <version>1.2.0</version>
</dependency>
```

## Usage

```java
var client = VexClient.builder()
    .endpoint("http://engine.vextura.io:8080")
    .licenseKey("VX-PRO-XXXX-XXXX-XXXX-XXXX")
    .build();

// Execute and wait (most common pattern)
var result = client.executeAndWait("payment-flow", Map.of("amount", 1000));
System.out.println(result.result());

// Async + poll
var run = client.workflows().asyncExecute("payment-flow", Map.of("amount", 1000));
var status = client.executions().getStatus(run.runId());
```
