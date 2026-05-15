namespace Vextura.Client.Step;

public sealed class StepResult
{
    public bool Success { get; private init; }
    public bool Skipped { get; private init; }
    public string? Error { get; private init; }
    public StepData UpdatedData { get; private init; } = new();

    public static StepResult Ok(object updatedFields)
    {
        var data = new StepData();
        foreach (var prop in updatedFields.GetType().GetProperties())
            data.Set(ToSnakeCase(prop.Name), prop.GetValue(updatedFields));
        return new StepResult { Success = true, UpdatedData = data };
    }

    public static StepResult Ok(StepData data) =>
        new() { Success = true, UpdatedData = data };

    public static StepResult Fail(string error) =>
        new() { Success = false, Error = error };

    public static StepResult Skip() =>
        new() { Success = true, Skipped = true };

    private static string ToSnakeCase(string name)
    {
        var sb = new System.Text.StringBuilder();
        for (var i = 0; i < name.Length; i++)
        {
            if (char.IsUpper(name[i]) && i > 0) sb.Append('_');
            sb.Append(char.ToLower(name[i]));
        }
        return sb.ToString();
    }
}
