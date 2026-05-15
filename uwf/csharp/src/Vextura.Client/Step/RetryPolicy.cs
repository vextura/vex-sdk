namespace Vextura.Client.Step;

public sealed class RetryPolicy
{
    public int MaxAttempts { get; init; } = 3;
    public int InitialDelayMs { get; init; } = 500;
    public double BackoffMultiplier { get; init; } = 2.0;
    public int MaxDelayMs { get; init; } = 0;
    public IReadOnlyList<string> RetryableErrors { get; init; } = [];
}
