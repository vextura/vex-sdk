using System.Text.Json;
using System.Text.Json.Nodes;

namespace Vextura.Client.Step;

/// <summary>
/// Runs a step container. Reads VexEvents from stdin, calls the handler,
/// writes results to stdout — identical to the Go vexfn.Handle() loop.
///
/// Usage in a step container's Program.cs:
///   await StepRunner.RunAsync(new MyCreditScoringStep());
/// </summary>
public static class StepRunner
{
    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower,
        PropertyNameCaseInsensitive = true
    };

    public static async Task RunAsync(StepBase handler, CancellationToken cancellationToken = default)
    {
        var stdin = Console.OpenStandardInput();
        var stdout = Console.OpenStandardOutput();
        using var reader = new StreamReader(stdin);
        await using var writer = new StreamWriter(stdout) { AutoFlush = true };

        while (!cancellationToken.IsCancellationRequested)
        {
            var line = await reader.ReadLineAsync(cancellationToken);
            if (line is null) break; // EOF — container is retiring
            if (string.IsNullOrWhiteSpace(line)) continue;

            VexEvent? evt;
            try { evt = JsonSerializer.Deserialize<VexEvent>(line, JsonOpts); }
            catch (Exception ex)
            {
                await WriteError(writer, new VexEvent(), "decode_error", ex.Message, JsonOpts);
                continue;
            }

            if (evt is null) continue;

            var data = evt.Data is not null
                ? StepData.FromJson(evt.Data.Value)
                : new StepData();

            var ctx = new StepContext
            {
                RunId = evt.TraceId ?? string.Empty,
                WorkflowId = evt.Fn ?? string.Empty,
                Tenant = evt.Tenant ?? string.Empty,
                StepName = evt.Fn ?? string.Empty,
                Operation = evt.Operation ?? string.Empty,
            };

            StepResult result;
            try { result = await handler.ExecuteAsync(ctx, data, cancellationToken); }
            catch (Exception ex)
            {
                await WriteError(writer, evt, "fn_error", ex.Message, JsonOpts);
                continue;
            }

            var response = new VexEvent
            {
                Id = evt.Id,
                TraceId = evt.TraceId,
                Version = evt.Version,
                Type = "fn.result",
                Tenant = evt.Tenant,
                Fn = evt.Fn,
                Operation = evt.Operation,
                Timestamp = DateTimeOffset.UtcNow,
                Data = result.Success
                    ? JsonSerializer.SerializeToElement(result.UpdatedData.ToDictionary(), JsonOpts)
                    : (JsonElement?)null,
                Error = result.Success ? null : new VexEventError
                {
                    Code = "fn_error",
                    Message = result.Error ?? "step failed"
                }
            };

            await writer.WriteLineAsync(JsonSerializer.Serialize(response, JsonOpts));
        }
    }

    private static async Task WriteError(
        StreamWriter w, VexEvent src, string code, string message, JsonSerializerOptions opts)
    {
        var err = new VexEvent
        {
            Id = src.Id, TraceId = src.TraceId, Version = src.Version,
            Type = "fn.error", Tenant = src.Tenant, Fn = src.Fn, Operation = src.Operation,
            Timestamp = DateTimeOffset.UtcNow,
            Error = new VexEventError { Code = code, Message = message }
        };
        await w.WriteLineAsync(JsonSerializer.Serialize(err, opts));
    }

    // ── Internal wire types (not exposed to clients) ──────────────────────────

    private sealed class VexEvent
    {
        public string? Id { get; set; }
        public string? TraceId { get; set; }
        public string? Version { get; set; }
        public string? Type { get; set; }
        public string? Tenant { get; set; }
        public string? Fn { get; set; }
        public string? Operation { get; set; }
        public string? Mode { get; set; }
        public DateTimeOffset Timestamp { get; set; }
        public JsonElement? Data { get; set; }
        public VexEventError? Error { get; set; }
    }

    private sealed class VexEventError
    {
        public string? Code { get; set; }
        public string? Message { get; set; }
    }
}
