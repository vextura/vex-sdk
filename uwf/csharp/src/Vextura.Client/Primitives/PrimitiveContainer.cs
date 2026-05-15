namespace Vextura.Client.Primitives;

/// <summary>
/// Lightweight DI container for service primitives.
/// Build it once in Program.cs and inject into workflow providers.
///
/// Example:
/// <code>
/// var container = new PrimitiveContainer.Builder()
///     .With&lt;IBankingPrimitive&gt;(new BankingClient(cfg.Banking))
///     .With&lt;INotifyPrimitive&gt;(new NotifyClient(cfg.Notify))
///     .Build();
/// </code>
/// </summary>
public sealed class PrimitiveContainer
{
    private readonly Dictionary<Type, IServicePrimitive> _primitives;

    private PrimitiveContainer(Dictionary<Type, IServicePrimitive> primitives)
        => _primitives = primitives;

    public T Get<T>() where T : class, IServicePrimitive
    {
        if (_primitives.TryGetValue(typeof(T), out var p) && p is T typed)
            return typed;
        throw new InvalidOperationException(
            $"Primitive {typeof(T).Name} is not registered. " +
            $"Call .With<{typeof(T).Name}>(...) on the builder.");
    }

    public bool Has<T>() where T : class, IServicePrimitive =>
        _primitives.ContainsKey(typeof(T));

    public static Builder NewBuilder() => new();

    public sealed class Builder
    {
        private readonly Dictionary<Type, IServicePrimitive> _map = [];

        public Builder With<T>(T implementation) where T : class, IServicePrimitive
        {
            _map[typeof(T)] = implementation;
            return this;
        }

        public PrimitiveContainer Build() => new(_map);
    }
}
