using System.Text.Json;

namespace Vextura.Client.Step;

public sealed class StepData
{
    private readonly Dictionary<string, object?> _data;

    public StepData() => _data = new(StringComparer.OrdinalIgnoreCase);

    public StepData(Dictionary<string, object?> data) =>
        _data = new(data, StringComparer.OrdinalIgnoreCase);

    public string GetString(string key, string defaultValue = "") =>
        _data.TryGetValue(key, out var v) ? Convert.ToString(v) ?? defaultValue : defaultValue;

    public int GetInt(string key, int defaultValue = 0) =>
        _data.TryGetValue(key, out var v) ? Convert.ToInt32(v) : defaultValue;

    public long GetLong(string key, long defaultValue = 0) =>
        _data.TryGetValue(key, out var v) ? Convert.ToInt64(v) : defaultValue;

    public double GetDouble(string key, double defaultValue = 0) =>
        _data.TryGetValue(key, out var v) ? Convert.ToDouble(v) : defaultValue;

    public decimal GetDecimal(string key, decimal defaultValue = 0) =>
        _data.TryGetValue(key, out var v) ? Convert.ToDecimal(v) : defaultValue;

    public bool GetBool(string key, bool defaultValue = false) =>
        _data.TryGetValue(key, out var v) ? Convert.ToBoolean(v) : defaultValue;

    public T? Get<T>(string key) where T : class =>
        _data.TryGetValue(key, out var v) ? v as T : null;

    public bool Has(string key) => _data.ContainsKey(key);

    public StepData Set(string key, object? value)
    {
        _data[key] = value;
        return this;
    }

    public StepData Merge(StepData other)
    {
        foreach (var (k, v) in other._data)
            _data[k] = v;
        return this;
    }

    public IReadOnlyDictionary<string, object?> ToDictionary() => _data;

    internal static StepData FromJson(JsonElement element)
    {
        var dict = new Dictionary<string, object?>(StringComparer.OrdinalIgnoreCase);
        foreach (var prop in element.EnumerateObject())
            dict[prop.Name] = JsonElementToObject(prop.Value);
        return new StepData(dict);
    }

    private static object? JsonElementToObject(JsonElement el) => el.ValueKind switch
    {
        JsonValueKind.String => el.GetString(),
        JsonValueKind.Number when el.TryGetInt64(out var i) => i,
        JsonValueKind.Number => el.GetDouble(),
        JsonValueKind.True => true,
        JsonValueKind.False => false,
        JsonValueKind.Null => null,
        _ => el.GetRawText()
    };
}
