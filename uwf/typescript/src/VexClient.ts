import axios, { AxiosInstance, AxiosError } from 'axios'
import {
  VexClientConfig,
  VexError,
  VexWorkflowNotFoundError,
  VexExecutionNotFoundError,
  VexExecutionTimeoutError,
  VexAuthError,
  WorkflowInfo,
  ListWorkflowsResponse,
  ExecutionStatus,
  AsyncExecuteResponse,
  ExecutionResult,
  ExecutionMetrics,
  ListExecutionsResponse,
  ListExecutionsParams,
} from './types'

const SDK_VERSION = '1.2.0'
const DEFAULT_TIMEOUT_MS = 30_000
const DEFAULT_MAX_RETRIES = 3
const DEFAULT_RETRY_BASE_DELAY_MS = 500
const DEFAULT_POLL_INTERVAL_MS = 1_000
const DEFAULT_EXECUTE_AND_WAIT_TIMEOUT_MS = 120_000

// ─────────────────────────────────────────────────────────────
// VexClient — Vextura Workflow Engine REST API client
//
// Usage:
//   const vex = VexClient.create({
//     endpoint: 'http://engine.vextura.io:8080',
//     licenseKey: 'VX-PRO-A4B2-9F3C-D7E1-2K8M',
//   })
//   const result = await vex.executeAndWait('transaction-validation', { amount: 5000 })
// ─────────────────────────────────────────────────────────────
export class VexClient {
  private readonly http: AxiosInstance
  private readonly config: Required<VexClientConfig>

  private constructor(config: VexClientConfig) {
    this.config = {
      endpoint: config.endpoint.replace(/\/$/, ''),
      licenseKey: config.licenseKey ?? '',
      bearerToken: config.bearerToken ?? '',
      timeoutMs: config.timeoutMs ?? DEFAULT_TIMEOUT_MS,
      maxRetries: config.maxRetries ?? DEFAULT_MAX_RETRIES,
      retryBaseDelayMs: config.retryBaseDelayMs ?? DEFAULT_RETRY_BASE_DELAY_MS,
    }

    this.http = axios.create({
      baseURL: this.config.endpoint,
      timeout: this.config.timeoutMs,
      headers: this.buildAuthHeaders(),
    })

    this.http.interceptors.request.use((req) => {
      req.headers['X-Vextura-SDK'] = `typescript/${SDK_VERSION}`
      return req
    })

    this.http.interceptors.response.use(
      (res) => res,
      (err: AxiosError) => Promise.reject(this.mapError(err)),
    )
  }

  /** Primary factory method. */
  static create(config: VexClientConfig): VexClient {
    return new VexClient(config)
  }

  // ─── Workflows ──────────────────────────────────────────────

  /** List all registered workflows. */
  async listWorkflows(): Promise<ListWorkflowsResponse> {
    return this.withRetry(() =>
      this.http.get<ListWorkflowsResponse>('/api/v1/workflows').then((r) => r.data),
    )
  }

  /** Get a single workflow by ID. */
  async getWorkflow(workflowId: string): Promise<WorkflowInfo> {
    return this.withRetry(() =>
      this.http.get<WorkflowInfo>(`/api/v1/workflows/${workflowId}`).then((r) => r.data),
    )
  }

  // ─── Execution ──────────────────────────────────────────────

  /**
   * Trigger async execution. Returns immediately with a runId.
   * Use getStatus() / getResult() to poll, or use executeAndWait().
   */
  async execute(
    workflowId: string,
    inputData: Record<string, unknown> = {},
    options: { timeoutMs?: number; metadata?: Record<string, unknown> } = {},
  ): Promise<AsyncExecuteResponse> {
    return this.withRetry(() =>
      this.http
        .post<AsyncExecuteResponse>(`/api/v1/workflows/${workflowId}/async-execute`, {
          ...inputData,
          ...(options.metadata ? { metadata: options.metadata } : {}),
          timeoutMs: options.timeoutMs ?? this.config.timeoutMs,
        })
        .then((r) => r.data),
    )
  }

  /**
   * Execute a workflow and block until it completes or times out.
   * This is the primary convenience method for most use cases.
   *
   * @param workflowId  - Registered workflow ID or name
   * @param inputData   - Workflow input payload
   * @param timeoutMs   - Max time to wait (default: 120s)
   * @returns           - Final execution result
   * @throws VexExecutionTimeoutError if workflow doesn't complete in time
   * @throws VexError on workflow failure
   */
  async executeAndWait(
    workflowId: string,
    inputData: Record<string, unknown> = {},
    timeoutMs: number = DEFAULT_EXECUTE_AND_WAIT_TIMEOUT_MS,
  ): Promise<ExecutionResult> {
    const run = await this.execute(workflowId, inputData)
    return this.waitForCompletion(run.runId, timeoutMs, run.pollAfterMs)
  }

  // ─── Status & Results ───────────────────────────────────────

  /** Get current execution status. */
  async getStatus(runId: string): Promise<ExecutionStatus> {
    return this.withRetry(() =>
      this.http.get<ExecutionStatus>(`/api/v1/executions/${runId}`).then((r) => r.data),
    )
  }

  /** Get final execution result (only available when status is completed/failed). */
  async getResult(runId: string): Promise<ExecutionResult> {
    return this.withRetry(() =>
      this.http
        .get<{ result: ExecutionResult; status: string }>(`/api/v1/executions/${runId}/result`)
        .then((r) => r.data.result),
    )
  }

  /** Get execution performance metrics. */
  async getMetrics(runId: string): Promise<ExecutionMetrics> {
    return this.withRetry(() =>
      this.http.get<ExecutionMetrics>(`/api/v1/executions/${runId}/metrics`).then((r) => r.data),
    )
  }

  /** List executions with optional filters. */
  async listExecutions(params: ListExecutionsParams = {}): Promise<ListExecutionsResponse> {
    return this.withRetry(() =>
      this.http.get<ListExecutionsResponse>('/api/v1/executions', { params }).then((r) => r.data),
    )
  }

  // ─── Execution Control ──────────────────────────────────────

  async cancel(runId: string): Promise<void> {
    await this.http.post(`/api/v1/executions/${runId}/cancel`)
  }

  async pause(runId: string): Promise<void> {
    await this.http.post(`/api/v1/executions/${runId}/pause`)
  }

  async resume(runId: string): Promise<void> {
    await this.http.post(`/api/v1/executions/${runId}/resume`)
  }

  async retry(runId: string): Promise<void> {
    await this.http.post(`/api/v1/executions/${runId}/retry`)
  }

  // ─── Health ─────────────────────────────────────────────────

  async healthCheck(): Promise<boolean> {
    try {
      const res = await this.http.get<{ status: string }>('/health')
      return res.data.status === 'healthy'
    } catch {
      return false
    }
  }

  // ─── Internal ───────────────────────────────────────────────

  private async waitForCompletion(
    runId: string,
    timeoutMs: number,
    initialPollMs: number,
  ): Promise<ExecutionResult> {
    const deadline = Date.now() + timeoutMs
    let pollInterval = initialPollMs || DEFAULT_POLL_INTERVAL_MS

    while (Date.now() < deadline) {
      const status = await this.getStatus(runId)

      if (status.isTerminal) {
        if (status.status === 'completed') {
          return this.getResult(runId)
        }
        throw new VexError(
          `Workflow execution ${runId} ${status.status}: ${status.errorMessage ?? 'no details'}`,
          500,
          status.errorMessage,
        )
      }

      // Exponential backoff capped at 5s
      await sleep(Math.min(pollInterval, 5_000))
      pollInterval = Math.min(pollInterval * 1.5, 5_000)
    }

    throw new VexExecutionTimeoutError(runId, timeoutMs)
  }

  private async withRetry<T>(fn: () => Promise<T>): Promise<T> {
    let lastError: unknown
    for (let attempt = 0; attempt <= this.config.maxRetries; attempt++) {
      try {
        return await fn()
      } catch (err) {
        lastError = err
        // Only retry on 5xx or network errors, not 4xx
        if (err instanceof VexError && err.statusCode < 500) throw err
        if (attempt < this.config.maxRetries) {
          await sleep(this.config.retryBaseDelayMs * Math.pow(2, attempt))
        }
      }
    }
    throw lastError
  }

  private buildAuthHeaders(): Record<string, string> {
    if (this.config.licenseKey) {
      return { 'X-Vextura-License': this.config.licenseKey }
    }
    if (this.config.bearerToken) {
      return { Authorization: `Bearer ${this.config.bearerToken}` }
    }
    return {}
  }

  private mapError(err: AxiosError): VexError {
    const status = err.response?.status ?? 0
    const body = err.response?.data as Record<string, string> | undefined
    const details = body?.details ?? body?.error ?? err.message

    if (status === 401 || status === 403) return new VexAuthError()
    if (status === 404) {
      if (details?.includes('workflow')) return new VexWorkflowNotFoundError(details)
      return new VexExecutionNotFoundError(details ?? 'unknown')
    }
    return new VexError(body?.error ?? `HTTP ${status}`, status, details)
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
