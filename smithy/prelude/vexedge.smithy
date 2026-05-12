$version: "2"

namespace vextura.edge

use smithy.api#documentation
use smithy.api#required
use vextura.fn#VexEvent
use vextura.fn#VexResponse

/// VexEdge is the abstract runtime contract every vex-fn container implements.
/// It documents the stdin/stdout wire protocol shared by all language SDKs.
///
/// Processing loop (pseudocode):
///   while (line = readLine(stdin)) {
///     event = JSON.parse(line)   // VexEvent
///     response = handler(event)  // user code
///     writeLine(stdout, JSON.stringify(response))  // VexResponse
///   }
service VexEdgeRuntime {
    version: "1.0"
    operations: [HandleEvent]
}

operation HandleEvent {
    input := {
        @required
        event: VexEvent
    }
    output := {
        @required
        response: VexResponse
    }
}
