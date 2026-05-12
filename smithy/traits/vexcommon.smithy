$version: "2"

namespace vextura.common

use smithy.api#trait
use smithy.api#pattern

/// Common identifier shapes reused across Vextura services.
string TenantId

@pattern("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
string Uuid

@pattern("^run-[0-9a-zA-Z]{16}$")
string RunId

@pattern("^step-[0-9a-zA-Z]{16}$")
string StepId

/// Annotates a structure field as a tenant-scoped resource identifier.
@trait(selector: "member")
structure tenantScoped {
    autoInject: Boolean
}

/// Annotates a service as belonging to a specific Vextura domain.
@trait(selector: "service")
structure vexDomain {
    /// Domain name: "fn" | "gate" | "registry" | "executor" | "pipeline"
    name: String

    /// Whether the service is internal-only (not exposed via vex-gate).
    internal: Boolean
}
