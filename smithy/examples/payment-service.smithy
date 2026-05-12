$version: "2"

namespace vextura.examples.payment

use smithy.api#required
use vextura.fn#vexFn
use vextura.gate#vexGate
use vextura.gate#vexGateService
use vextura.common#vexDomain

@vexDomain(name: "fn")
@vexGateService(basePath: "/api/v1", defaultRequiresAuth: true)
service PaymentService {
    version: "1.0"
    operations: [ProcessPayment, GetPaymentStatus]
}

/// vex-fn: validates + routes payment transactions.
@vexFn(name: "process-payment", timeoutMs: 5000, memoryMib: 128)
operation ProcessPayment {
    input := {
        @required transactionId: String
        @required amount: Double
        @required currency: String
        cardToken: String
    }
    output := {
        @required success: Boolean
        @required authCode: String
        declineReason: String
    }
}

/// vex-gate: HTTP GET exposed at /api/v1/payments/{transactionId}
@vexGate(method: "GET", path: "/payments/{transactionId}")
operation GetPaymentStatus {
    input := {
        @required transactionId: String
    }
    output := {
        @required status: String
        @required amount: Double
        @required currency: String
    }
}
