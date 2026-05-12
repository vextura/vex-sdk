$version: "2"

namespace vextura.gate

use smithy.api#trait
use smithy.api#documentation

/// Marks a service operation as a vex-gate HTTP handler.
@trait(selector: "operation")
structure vexGate {
    /// HTTP method (GET, POST, PUT, DELETE, PATCH).
    method: String

    /// URL path template, e.g. "/api/v1/payments/{paymentId}".
    path: String

    /// Whether this endpoint requires a valid VXLIC JWT (default: true).
    requiresAuth: Boolean

    /// Rate-limit bucket name (empty = default per-tenant bucket).
    rateLimitBucket: String

    /// Upstream service name as registered in vex-registry.
    upstream: String
}

/// Applied to the service shape itself to set gateway-level defaults.
@trait(selector: "service")
structure vexGateService {
    /// Base URL path prefix for all operations in this service.
    basePath: String

    /// Default upstream service name.
    defaultUpstream: String

    /// Whether all operations require auth by default.
    defaultRequiresAuth: Boolean
}
