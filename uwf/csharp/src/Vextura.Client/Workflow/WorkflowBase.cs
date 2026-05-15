using Vextura.Client.Step;

namespace Vextura.Client.Workflow;

/// <summary>
/// Base class for all workflow definitions.
/// Extend this, override <see cref="GetSteps"/>, and register via
/// <see cref="Registration.VexClientBuilder.AddWorkflow"/>.
/// </summary>
public abstract class WorkflowBase
{
    public string Id { get; }
    public string Name { get; }
    public string Description { get; protected set; } = string.Empty;

    protected WorkflowBase(string id, string name)
    {
        Id = id;
        Name = name;
    }

    /// <summary>
    /// Return the ordered list of steps for this workflow.
    /// </summary>
    public abstract IEnumerable<StepBase> GetSteps();
}
