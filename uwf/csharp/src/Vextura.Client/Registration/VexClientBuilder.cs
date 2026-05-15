using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using Vextura.Client.Step;
using Vextura.Client.Workflow;

namespace Vextura.Client.Registration;

/// <summary>
/// Entry point for connecting to the Vextura engine and registering workflows.
///
/// Example:
/// <code>
/// await new VexClientBuilder(new VexClientOptions
///     {
///         EngineUrl = "http://10.249.1.51",
///         Tenant    = "acme"
///     })
///     .AddWorkflow(new LoanApprovalWorkflow())
///     .AddWorkflowProvider(new FraudReviewProvider(container))
///     .ConnectAsync();
/// </code>
/// </summary>
public sealed class VexClientBuilder
{
    private readonly VexClientOptions _options;
    private readonly List<WorkflowBase> _workflows = [];

    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
        WriteIndented = false
    };

    public VexClientBuilder(VexClientOptions options) => _options = options;

    public VexClientBuilder AddWorkflow(WorkflowBase workflow)
    {
        _workflows.Add(workflow);
        return this;
    }

    public VexClientBuilder AddWorkflowProvider(IWorkflowProvider provider)
    {
        _workflows.Add(provider.GetWorkflow());
        return this;
    }

    /// <summary>
    /// Registers all workflows with the engine, then returns.
    /// Throws <see cref="InvalidOperationException"/> if any registration fails.
    /// </summary>
    public async Task ConnectAsync(CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_options.EngineUrl))
            throw new InvalidOperationException("VexClientOptions.EngineUrl is required.");

        using var http = BuildHttpClient();

        foreach (var wf in _workflows)
        {
            var payload = BuildPayload(wf);
            var json = JsonSerializer.Serialize(payload, JsonOpts);
            var content = new StringContent(json, Encoding.UTF8, "application/json");

            var response = await http.PostAsync(
                $"{_options.EngineUrl.TrimEnd('/')}/api/v1/workflows",
                content,
                cancellationToken);

            if (!response.IsSuccessStatusCode)
            {
                var body = await response.Content.ReadAsStringAsync(cancellationToken);
                throw new InvalidOperationException(
                    $"Failed to register workflow '{wf.Id}': " +
                    $"HTTP {(int)response.StatusCode} — {body}");
            }

            Console.WriteLine($"[vextura] registered workflow: {wf.Id}");
        }

        Console.WriteLine($"[vextura] {_workflows.Count} workflow(s) registered.");
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private HttpClient BuildHttpClient()
    {
        var client = new HttpClient { Timeout = _options.Timeout };
        if (!string.IsNullOrWhiteSpace(_options.AuthToken))
            client.DefaultRequestHeaders.Authorization =
                new AuthenticationHeaderValue("Bearer", _options.AuthToken);
        return client;
    }

    private WorkflowPayload BuildPayload(WorkflowBase wf) => new()
    {
        Id = wf.Id,
        Name = wf.Name,
        Description = string.IsNullOrWhiteSpace(wf.Description) ? null : wf.Description,
        Tenant = string.IsNullOrWhiteSpace(_options.Tenant)
            ? throw new InvalidOperationException("VexClientOptions.Tenant is required.")
            : _options.Tenant,
        Steps = wf.GetSteps().Select(BuildStepPayload).ToList()
    };

    private static StepPayload BuildStepPayload(StepBase s) => new()
    {
        Name = s.Name,
        Fn = s.FnName,
        Operation = s.Operation,
        FailurePolicy = s.FailurePolicy == "abort" ? null : s.FailurePolicy,
        TimeoutSec = s.Timeout > TimeSpan.Zero ? (int)s.Timeout.TotalSeconds : 0,
        Retry = s.RetryConfig is null ? null : new RetryPayload
        {
            MaxAttempts = s.RetryConfig.MaxAttempts,
            InitialDelayMs = s.RetryConfig.InitialDelayMs,
            BackoffMultiplier = s.RetryConfig.BackoffMultiplier == 0
                ? null : s.RetryConfig.BackoffMultiplier,
            MaxDelayMs = s.RetryConfig.MaxDelayMs == 0 ? null : s.RetryConfig.MaxDelayMs,
        },
        Manual = s.ManualConfig is null ? null : new ManualPayload
        {
            Instructions = s.ManualConfig.Instructions,
            ApproverRoles = s.ManualConfig.ApproverRoles.Count > 0
                ? s.ManualConfig.ApproverRoles.ToList() : null,
            TimeoutSec = s.ManualConfig.Timeout > TimeSpan.Zero
                ? (int)s.ManualConfig.Timeout.TotalSeconds : 0,
        }
    };

    // ── JSON payload shapes (match engine WorkflowDefinition) ─────────────────

    private sealed class WorkflowPayload
    {
        public string Id { get; init; } = string.Empty;
        public string Name { get; init; } = string.Empty;
        public string? Description { get; init; }
        public string Tenant { get; init; } = string.Empty;
        public List<StepPayload> Steps { get; init; } = [];
    }

    private sealed class StepPayload
    {
        public string Name { get; init; } = string.Empty;
        public string Fn { get; init; } = string.Empty;
        public string? Operation { get; init; }
        public string? FailurePolicy { get; init; }
        public int TimeoutSec { get; init; }
        public RetryPayload? Retry { get; init; }
        public ManualPayload? Manual { get; init; }
    }

    private sealed class RetryPayload
    {
        public int MaxAttempts { get; init; }
        public int InitialDelayMs { get; init; }
        public double? BackoffMultiplier { get; init; }
        public int? MaxDelayMs { get; init; }
    }

    private sealed class ManualPayload
    {
        public string Instructions { get; init; } = string.Empty;
        public List<string>? ApproverRoles { get; init; }
        public int TimeoutSec { get; init; }
    }
}
