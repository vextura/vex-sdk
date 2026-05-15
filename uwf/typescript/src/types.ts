// ─────────────────────────────────────────────────────────────
// Vextura SDK — Core types (mirrors Smithy model)
// Source of truth: api/workflow-api.smithy
// ─────────────────────────────────────────────────────────────

export type WorkflowStatus =
  | 'pending'
  | 'running'
  | 'completed'
  | 'failed'
  | 'cancelled'
  | 'paused'
  | 'queued'
  | 'processing'

export type AsyncExecutionStatus =
  | 'queued'
  | 'processing'
  | 'completed'
  | 'failed'
  | 'timeout'

export interface WorkflowInfo {
  workflowId: string
  name: string
  description: string
  stepCount: number
  steps?: StepInfo[]
}

export interface StepInfo {
  name: string
  childStepCount: number
  isParallel: boolean
}

export interface ExecutionStatus {
  runId: string
  workflowId: string
  status: WorkflowStatus
  currentStep?: string
  currentStepIndex: number
  currentChildStepIndex: number
  progress: number
  startTime?: string
  endTime?: string
  errorMessage?: string
  lastAttemptedStep?: string
  isTerminal: boolean
  metadata?: Record<string, unknown>
}

export interface AsyncExecuteResponse {
  runId: string
  status: AsyncExecutionStatus
  message: string
  statusUrl: string
  resultUrl: string
  pollAfterMs: number
  estimatedCompletionMs: number
  expiresAt?: string
}

export interface ExecutionResult {
  runId: string
  workflowId: string
  status: AsyncExecutionStatus
  result: Record<string, unknown>
  completedAt: string
  executionTimeMillis?: number
  stepCount?: number
  errorDetails?: string
}

export interface ExecutionMetrics {
  runId: string
  workflowId: string
  totalSteps: number
  completedSteps: number
  failedSteps: number
  totalChildSteps: number
  completedChildSteps: number
  failedChildSteps: number
  totalDurationMillis: number
  averageStepDuration: number
  successRate: number
}

export interface ListWorkflowsResponse {
  workflows: WorkflowInfo[]
  count: number
}

export interface ListExecutionsResponse {
  executions: ExecutionInfo[]
  count: number
  limit: number
  offset: number
}

export interface ExecutionInfo {
  runId: string
  workflowId: string
  status: WorkflowStatus
  currentStepIndex: number
  currentChildStepIndex: number
  startTime?: string
  endTime?: string
  errorMessage?: string
  isTerminal: boolean
  isRunning: boolean
  isPending: boolean
  createdAt: string
  updatedAt: string
}

export interface ListExecutionsParams {
  workflowId?: string
  status?: WorkflowStatus
  limit?: number
  offset?: number
}

// ─── Auth ─────────────────────────────────────────────────────

export type AuthMode = 'license-key' | 'bearer-token' | 'none'

export interface VexClientConfig {
  /** Base URL of the Vextura engine HTTP API. Example: http://engine.vextura.io:8080 */
  endpoint: string

  /**
   * License key issued by the Vextura portal (VX-PRO-XXXX-XXXX-XXXX-XXXX).
   * Recommended for all production use — sent as X-Vextura-License header.
   */
  licenseKey?: string

  /**
   * Bearer token for banks with existing JWT/OIDC infrastructure.
   * Sent as Authorization: Bearer <token>.
   * If both licenseKey and bearerToken are set, licenseKey takes priority.
   */
  bearerToken?: string

  /** HTTP request timeout in milliseconds. Default: 30000 */
  timeoutMs?: number

  /** Number of retries on transient failures (5xx, network). Default: 3 */
  maxRetries?: number

  /** Base delay between retries in milliseconds (exponential backoff). Default: 500 */
  retryBaseDelayMs?: number
}

// ─── Errors ───────────────────────────────────────────────────

export class VexError extends Error {
  constructor(
    message: string,
    public readonly statusCode: number,
    public readonly details?: string,
  ) {
    super(message)
    this.name = 'VexError'
  }
}

export class VexWorkflowNotFoundError extends VexError {
  constructor(workflowId: string) {
    super(`Workflow not found: ${workflowId}`, 404)
    this.name = 'VexWorkflowNotFoundError'
  }
}

export class VexExecutionNotFoundError extends VexError {
  constructor(runId: string) {
    super(`Execution not found: ${runId}`, 404)
    this.name = 'VexExecutionNotFoundError'
  }
}

export class VexExecutionTimeoutError extends VexError {
  constructor(runId: string, timeoutMs: number) {
    super(`Execution ${runId} did not complete within ${timeoutMs}ms`, 408)
    this.name = 'VexExecutionTimeoutError'
  }
}

export class VexAuthError extends VexError {
  constructor() {
    super('Invalid or missing Vextura license key', 401)
    this.name = 'VexAuthError'
  }
}
