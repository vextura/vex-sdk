namespace Vextura.Client.Registration;

public sealed class VexClientOptions
{
    /// <summary>Engine REST API base URL. e.g. http://10.249.1.51</summary>
    public string EngineUrl { get; init; } = string.Empty;

    /// <summary>Default tenant for all registered workflows.</summary>
    public string Tenant { get; init; } = string.Empty;

    /// <summary>Optional auth token sent as Bearer in API calls.</summary>
    public string? AuthToken { get; init; }

    /// <summary>HTTP request timeout. Defaults to 30 seconds.</summary>
    public TimeSpan Timeout { get; init; } = TimeSpan.FromSeconds(30);
}
