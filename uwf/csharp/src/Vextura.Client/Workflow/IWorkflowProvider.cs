namespace Vextura.Client.Workflow;

/// <summary>
/// Factory pattern for workflows that need dependency injection.
/// Implement this when your workflow steps depend on external services.
/// </summary>
public interface IWorkflowProvider
{
    WorkflowBase GetWorkflow();
}
