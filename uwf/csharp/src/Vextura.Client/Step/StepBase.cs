namespace Vextura.Client.Step;

/// <summary>
/// Base class for all step implementations. Extend this and override
/// <see cref="ExecuteAsync"/> to write business logic.
/// </summary>
public abstract class StepBase
{
    public string Name { get; }

    /// <summary>
    /// The vex-fn function name this step maps to.
    /// Defaults to <see cref="Name"/> when not explicitly set.
    /// </summary>
    public string FnName { get; private set; }

    internal RetryPolicy? RetryConfig { get; private set; }
    internal TimeSpan Timeout { get; private set; } = TimeSpan.Zero;
    internal ManualApprovalConfig? ManualConfig { get; private set; }
    internal string FailurePolicy { get; private set; } = "abort";
    internal string? Operation { get; private set; }

    protected StepBase(string name)
    {
        Name = name;
        FnName = name;
    }

    /// <summary>Configure automatic retry for this step.</summary>
    protected StepBase WithRetry(RetryPolicy policy)
    {
        RetryConfig = policy;
        FailurePolicy = "retry";
        return this;
    }

    /// <summary>Set a per-step execution timeout.</summary>
    protected StepBase WithTimeout(TimeSpan timeout)
    {
        Timeout = timeout;
        return this;
    }

    /// <summary>
    /// Mark this step as requiring manual approval before proceeding.
    /// </summary>
    protected StepBase AsManual(ManualApprovalConfig config)
    {
        ManualConfig = config;
        FailurePolicy = "pause_for_approval";
        return this;
    }

    /// <summary>Skip this step on failure instead of aborting the workflow.</summary>
    protected StepBase SkipOnFailure()
    {
        FailurePolicy = "skip";
        return this;
    }

    /// <summary>
    /// Override the vex-fn function name (defaults to step <see cref="Name"/>).
    /// </summary>
    protected StepBase WithFn(string fnName)
    {
        FnName = fnName;
        return this;
    }

    /// <summary>Set the operation forwarded in the VexEvent. Defaults to step name.</summary>
    protected StepBase WithOperation(string operation)
    {
        Operation = operation;
        return this;
    }

    /// <summary>
    /// Implement your business logic here.
    /// Read inputs from <paramref name="data"/>, write outputs via <see cref="StepResult.Ok"/>.
    /// </summary>
    protected internal abstract Task<StepResult> ExecuteAsync(
        StepContext context,
        StepData data,
        CancellationToken cancellationToken = default);
}
