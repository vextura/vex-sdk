# Vextura.Client — C# / .NET 8

Vextura Workflow Engine client SDK for .NET 8+.

Source of truth: `api/workflow-api.smithy`

## Install

```
dotnet add package Vextura.Client
```

## Usage

```csharp
// Define a workflow
public class PaymentWorkflow : WorkflowBase {
    public PaymentWorkflow() : base("payment-flow", "Payment Flow") {}

    public override IEnumerable<StepBase> GetSteps() => [
        new CreditScoringStep(),
        new FraudCheckStep(),
    ];
}

// Connect and register
await new VexClientBuilder(new VexClientOptions {
    EngineUrl = "http://engine.vextura.io:8080",
    Tenant    = "acme",
})
.AddWorkflow(new PaymentWorkflow())
.ConnectAsync();
```
