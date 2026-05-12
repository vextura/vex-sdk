$version: "2"

namespace vextura.fn

use smithy.api#trait
use smithy.api#documentation
use smithy.api#required
use smithy.api#pattern

/// Marks a service operation as a VexEdge function (vex-fn container).
/// The annotated operation receives a VexEvent on stdin and writes a
/// VexResponse to stdout (newline-delimited JSON).
@trait(selector: "operation")
structure vexFn {
    /// Human-readable name shown in the vex-console.
    name: String

    /// Execution timeout in milliseconds (default: 30 000).
    timeoutMs: Integer

    /// Maximum concurrent executions per container instance.
    maxConcurrency: Integer

    /// Memory limit in MiB (default: 128).
    memoryMib: Integer
}

/// Wire format written to the function's stdin.
structure VexEvent {
    @required
    eventId: String

    @required
    workflowRunId: String

    @required
    stepId: String

    @required
    inputData: Document

    metadata: VexEventMetadata
}

structure VexEventMetadata {
    traceId: String
    spanId: String
    tenantId: String
    priority: Integer
    deadline: Timestamp
}

/// Wire format written to stdout by the function.
structure VexResponse {
    @required
    eventId: String

    @required
    success: Boolean

    outputData: Document
    errorCode: String
    errorMessage: String
    durationMs: Long
}
