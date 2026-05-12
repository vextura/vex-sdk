# @vextura/vex-sdk

TypeScript/Node.js SDK for writing VexEdge function containers.

Published to GitHub Packages as `@vextura/vex-sdk`.

## Installation

### Step 1 — Authenticate with GitHub Packages

Add to `~/.npmrc`:
```
//npm.pkg.github.com/:_authToken=YOUR_GITHUB_TOKEN
@vextura:registry=https://npm.pkg.github.com
```

### Step 2 — Install

```bash
npm install @vextura/vex-sdk
```

## Quick start

```typescript
import { run, VexEvent, VexContext } from '@vextura/vex-sdk';

run(async (event: VexEvent, ctx: VexContext) => {
  const txId   = event.inputData['transactionId'] as string;
  const amount = event.inputData['amount'] as number;

  return {
    eventId: event.eventId,
    success: true,
    outputData: { approved: true, authCode: `AUTH-${txId}` },
  };
});
```

## Calling other functions

```typescript
const result = await ctx.primitives.call('kyc-check', { customerId: 'c-001' });
```

## Deploy

```bash
vexctl fn deploy --name process-payment --image ghcr.io/vextura/process-payment:latest
```

## Smithy source

Types are aligned with `smithy/traits/vexfn.smithy`.
