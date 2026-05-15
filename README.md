# vex-sdk

Official SDK monorepo for the **Vextura Workflow Platform** — Smithy-first.

> Source of truth for all types: `smithy/` directory.

---

## Structure

| Directory | Purpose |
|-----------|---------|
| [`smithy/`](./smithy) | Smithy IDL definitions — source of truth for all types and traits |
| [`go/`](./go) | VexEdge **fn** SDK for Go — write function containers |
| [`java/`](./java) | VexEdge **fn** SDK for Java 21 — write function containers |
| [`ts/`](./ts) | VexEdge **fn** SDK for TypeScript — write function containers |
| [`uwf/typescript/`](./uwf/typescript) | Workflow Engine **client** SDK for TypeScript (`@vextura/sdk`) |
| [`uwf/java/`](./uwf/java) | Workflow Engine **client** SDK for Java 21 (`io.vextura:vextura-sdk`) |
| [`uwf/csharp/`](./uwf/csharp) | Workflow Engine **client** SDK for .NET 8 (`Vextura.Client`) |

---

## VexEdge fn SDKs (write function containers)

These SDKs implement the vex-fn stdin/stdout wire protocol defined in `smithy/traits/vexfn.smithy`.

### Go

```bash
go get github.com/vextura/vex-sdk/go
```

```go
func main() { vexfn.Handle(handle) }

func handle(ctx context.Context, evt vexfn.Event[Input]) (*Output, error) {
    return &Output{Approved: true}, nil
}
```

### Java 21

```groovy
dependencies {
    implementation 'io.vextura:vex-sdk-java:1.0.0'
}
```

```java
VexFn.run((event, ctx) ->
    VexResponse.ok(event.eventId(), Map.of("approved", true), 0L));
```

### TypeScript

```bash
npm install @vextura/vex-sdk
```

```ts
import { run } from '@vextura/vex-sdk'
run(async (event, ctx) => ({ eventId: event.eventId, success: true, outputData: { approved: true } }))
```

---

## Workflow Engine Client SDKs (call the engine)

These SDKs call the Vextura Workflow Engine HTTP API defined in `smithy/` (`workflow-api.smithy`).

### TypeScript

```bash
npm install @vextura/sdk
```

```ts
import { VexClient } from '@vextura/sdk'
const vex = VexClient.create({ endpoint: 'http://engine:8080', licenseKey: 'VX-PRO-...' })
const result = await vex.executeAndWait('payment-flow', { amount: 5000 })
```

### Java 21

```xml
<dependency>
  <groupId>io.vextura</groupId>
  <artifactId>vextura-sdk</artifactId>
  <version>1.2.0</version>
</dependency>
```

```java
var result = VexClient.builder()
    .endpoint("http://engine:8080").licenseKey("VX-PRO-...")
    .build()
    .executeAndWait("payment-flow", Map.of("amount", 5000));
```

### C# / .NET 8

```csharp
await new VexClientBuilder(new VexClientOptions { EngineUrl = "http://engine:8080", Tenant = "acme" })
    .AddWorkflow(new PaymentWorkflow())
    .ConnectAsync();
```

---

## CLI

```bash
curl -fsSL https://github.com/vextura/releases/releases/latest/download/install.sh | sh
```

---

## Platform engine

Distributed as Docker images — contact your Vextura engineer for access.
`ghcr.io/vextura/uwf-engine:latest`
