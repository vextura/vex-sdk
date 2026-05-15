namespace Vextura.Client.Step;

public sealed class ManualApprovalConfig
{
    public string Instructions { get; init; } = string.Empty;
    public IReadOnlyList<string> ApproverRoles { get; init; } = [];
    public TimeSpan Timeout { get; init; } = TimeSpan.Zero;
}
