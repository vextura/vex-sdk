using Vextura.Client.Step;

namespace Vextura.Client.Workflow;

/// <summary>
/// Fluent builder for defining simple, static workflows without subclassing.
///
/// Example:
/// <code>
/// var wf = WorkflowBuilder
///     .Create("loan-approval", "Loan Approval")
///     .WithDescription("Full loan approval pipeline")
///     .AddStep("credit-scoring").WithFn("credit-scorer").WithRetry(3).Done()
///     .AddStep("risk-assessment").WithFn("risk-assessor").Done()
///     .Build();
/// </code>
/// </summary>
public sealed class WorkflowBuilder
{
    private readonly string _id;
    private readonly string _name;
    private string _description = string.Empty;
    private readonly List<StepDefinition> _steps = [];

    private WorkflowBuilder(string id, string name)
    {
        _id = id;
        _name = name;
    }

    public static WorkflowBuilder Create(string id, string name) => new(id, name);

    public WorkflowBuilder WithDescription(string description)
    {
        _description = description;
        return this;
    }

    public StepDefinition AddStep(string name) => new(name, this);

    internal WorkflowBuilder AddStepDefinition(StepDefinition step)
    {
        _steps.Add(step);
        return this;
    }

    public BuiltWorkflow Build() => new(_id, _name, _description, _steps);

    // ── Step definition DSL ───────────────────────────────────────────────────

    public sealed class StepDefinition
    {
        private readonly string _name;
        private readonly WorkflowBuilder _parent;
        private string _fn;
        private string? _operation;
        private string _failurePolicy = "abort";
        private TimeSpan _timeout = TimeSpan.Zero;
        private RetryPolicy? _retry;
        private ManualApprovalConfig? _manual;

        internal StepDefinition(string name, WorkflowBuilder parent)
        {
            _name = name;
            _fn = name;
            _parent = parent;
        }

        public StepDefinition WithFn(string fn) { _fn = fn; return this; }
        public StepDefinition WithOperation(string op) { _operation = op; return this; }
        public StepDefinition WithTimeout(TimeSpan t) { _timeout = t; return this; }
        public StepDefinition SkipOnFailure() { _failurePolicy = "skip"; return this; }

        public StepDefinition WithRetry(int maxAttempts, int initialDelayMs = 500, double backoff = 2.0)
        {
            _retry = new RetryPolicy
            {
                MaxAttempts = maxAttempts,
                InitialDelayMs = initialDelayMs,
                BackoffMultiplier = backoff
            };
            _failurePolicy = "retry";
            return this;
        }

        public StepDefinition WithManualApproval(string instructions, TimeSpan timeout = default)
        {
            _manual = new ManualApprovalConfig { Instructions = instructions, Timeout = timeout };
            _failurePolicy = "pause_for_approval";
            return this;
        }

        public WorkflowBuilder Done()
        {
            _parent.AddStepDefinition(this);
            return _parent;
        }

        internal string Name => _name;
        internal string Fn => _fn;
        internal string? Operation => _operation;
        internal string FailurePolicy => _failurePolicy;
        internal TimeSpan Timeout => _timeout;
        internal RetryPolicy? Retry => _retry;
        internal ManualApprovalConfig? Manual => _manual;
    }

    // ── Concrete WorkflowBase produced by the builder ─────────────────────────

    public sealed class BuiltWorkflow : WorkflowBase
    {
        private readonly IReadOnlyList<StepDefinition> _defs;

        internal BuiltWorkflow(string id, string name, string description,
            IReadOnlyList<StepDefinition> defs)
            : base(id, name)
        {
            Description = description;
            _defs = defs;
        }

        public override IEnumerable<StepBase> GetSteps() =>
            _defs.Select(d => new BuiltStep(d));

        // Thin StepBase wrapper around a builder StepDefinition
        private sealed class BuiltStep : StepBase
        {
            internal BuiltStep(StepDefinition def) : base(def.Name)
            {
                WithFn(def.Fn);
                if (def.Operation is not null) WithOperation(def.Operation);
                if (def.Retry is not null) WithRetry(def.Retry);
                if (def.Manual is not null) AsManual(def.Manual);
                if (def.Timeout > TimeSpan.Zero) WithTimeout(def.Timeout);
                if (def.FailurePolicy == "skip") SkipOnFailure();
            }

            protected internal override Task<StepResult> ExecuteAsync(
                StepContext context, StepData data, CancellationToken cancellationToken = default)
                => throw new NotSupportedException(
                    "BuiltWorkflow steps are definition-only. " +
                    "Override ExecuteAsync in a concrete StepBase subclass for step containers.");
        }
    }
}
