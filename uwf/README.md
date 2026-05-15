# vex-sdk/uwf — Workflow Engine Client SDKs

Client SDKs for integrating with the **Vextura Unified Workflow Engine**.

Source of truth: `api/workflow-api.smithy` (inside the engine — types mirror the Smithy model).

> These SDKs connect **to** the engine over HTTP REST. They are not for writing vex-fn containers — for that see `../go`, `../java`, `../ts`.

## SDKs

| Language | Directory | Package |
|----------|-----------|---------|
| TypeScript / Node.js | [`typescript/`](./typescript) | `@vextura/sdk` |
| Java 21 | [`java/`](./java) | `io.vextura:vextura-sdk` |
| C# / .NET 8 | [`csharp/`](./csharp) | `Vextura.Client` |

---

## TypeScript

```bash
npm install @vextura/sdk
```

```ts
import { VexClient } from '@vextura/sdk'

const vex = VexClient.create({
  endpoint: 'http://engine.vextura.io:8080',
  licenseKey: 'VX-PRO-A4B2-9F3C-D7E1-2K8M',
})

const result = await vex.executeAndWait('transaction-validation', {
  amount: 5000,
  currency: 'KZT',
})
```

---

## Java 21

```xml
<dependency>
  <groupId>io.vextura</groupId>
  <artifactId>vextura-sdk</artifactId>
  <version>1.2.0</version>
</dependency>
```

```java
var client = VexClient.builder()
    .endpoint("http://engine.vextura.io:8080")
    .licenseKey("VX-PRO-A4B2-9F3C-D7E1-2K8M")
    .build();

var result = client.executeAndWait("transaction-validation",
    Map.of("amount", 5000, "currency", "KZT"));
```

---

## C# / .NET 8

```csharp
await new VexClientBuilder(new VexClientOptions {
    EngineUrl = "http://engine.vextura.io:8080",
    Tenant    = "acme",
    AuthToken = "VX-PRO-A4B2-9F3C-D7E1-2K8M",
})
.AddWorkflow(new MyWorkflow())
.ConnectAsync();
```
