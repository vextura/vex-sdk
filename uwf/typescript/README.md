# @vextura/sdk — TypeScript/JavaScript

Vextura Workflow Engine client SDK for TypeScript and Node.js.

Source of truth: `api/workflow-api.smithy`

## Install

```bash
npm install @vextura/sdk
```

## Usage

```ts
import { VexClient } from '@vextura/sdk'

const vex = VexClient.create({
  endpoint: 'http://engine.vextura.io:8080',
  licenseKey: 'VX-PRO-XXXX-XXXX-XXXX-XXXX',
})

// Execute workflow and wait for result
const result = await vex.executeAndWait('payment-flow', { amount: 1000 })

// Async execute + poll manually
const run = await vex.execute('payment-flow', { amount: 1000 })
const status = await vex.getStatus(run.runId)
```
