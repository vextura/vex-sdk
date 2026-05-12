/**
 * VexEdge wire types aligned with smithy/traits/vexfn.smithy.
 */

export interface EventMetadata {
  traceId?: string;
  spanId?: string;
  tenantId?: string;
  priority?: number;
}

export interface VexEvent {
  eventId: string;
  workflowRunId: string;
  stepId: string;
  inputData: Record<string, unknown>;
  metadata?: EventMetadata;
}

export interface VexResponse {
  eventId: string;
  success: boolean;
  outputData?: Record<string, unknown>;
  errorCode?: string;
  errorMessage?: string;
  durationMs?: number;
}

export type VexFnHandler = (
  event: VexEvent,
  ctx: VexContext
) => Promise<VexResponse> | VexResponse;

export interface VexContext {
  tenantId?: string;
  traceId?: string;
  primitives: PrimitivesClient;
}

export interface PrimitivesClient {
  call(fnName: string, input: unknown): Promise<unknown>;
}
