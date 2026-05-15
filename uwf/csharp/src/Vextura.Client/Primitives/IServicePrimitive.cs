namespace Vextura.Client.Primitives;

/// <summary>
/// Marker interface for external service abstractions.
/// Implement one interface per external service (banking, notify, storage, etc.)
/// and inject it into steps that need it.
/// </summary>
public interface IServicePrimitive { }
