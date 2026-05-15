namespace Vextura.Client.Step;

public sealed class StepContext
{
    public string RunId { get; init; } = string.Empty;
    public string WorkflowId { get; init; } = string.Empty;
    public string Tenant { get; init; } = string.Empty;
    public string StepName { get; init; } = string.Empty;
    public string Operation { get; init; } = string.Empty;
    public string PendingApprovalStep { get; init; } = string.Empty;
    public int RetryCount { get; init; }
    public int MaxRetries { get; init; }
}
