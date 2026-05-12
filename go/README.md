# vex-sdk/go

Go SDK for writing VexEdge functions and gate handlers.

## Module path

```
github.com/vextura/vex-sdk/go
```

## Installation

```bash
go get github.com/vextura/vex-sdk/go
```

## Quick start — writing a vex-fn container

```go
package main

import (
    "context"
    "fmt"

    vexfn "github.com/vextura/vex-sdk/go/vexfn"
)

func main() {
    vexfn.Run(func(ctx context.Context, event *vexfn.VexEvent) (*vexfn.VexResponse, error) {
        txID, _ := event.InputData["transactionId"].(string)
        amount, _ := event.InputData["amount"].(float64)

        fmt.Printf("Processing txn %s for %.2f\n", txID, amount)

        return &vexfn.VexResponse{
            OutputData: map[string]any{
                "approved": true,
                "authCode": "AUTH-001",
            },
        }, nil
    })
}
```

Build and deploy via `vexctl`:

```bash
vexctl fn deploy --name process-payment --image ghcr.io/vextura/process-payment:latest
```

## Calling other functions — PrimitivesClient

```go
pc := vexfn.NewPrimitivesClient() // reads VEX_PRIMITIVES_URL env var

var result map[string]any
if err := pc.Call(ctx, "kyc-check", map[string]any{"customerId": "c-001"}, &result); err != nil {
    return nil, err
}
```

## Smithy source

The types in this package are aligned with `smithy/traits/vexfn.smithy`.
