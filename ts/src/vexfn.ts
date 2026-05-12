import * as readline from 'readline';
import * as http from 'http';
import { VexEvent, VexResponse, VexFnHandler, VexContext, PrimitivesClient } from './types';

function makePrimitivesClient(baseUrl: string | undefined): PrimitivesClient {
  return {
    async call(fnName: string, input: unknown): Promise<unknown> {
      if (!baseUrl) throw new Error('VEX_PRIMITIVES_URL not set');
      const body = JSON.stringify(input);
      return new Promise((resolve, reject) => {
        const url = new URL(`/fn/${fnName}`, baseUrl);
        const req = http.request(
          {
            hostname: url.hostname, port: url.port, path: url.pathname, method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) }
          },
          (res) => {
            let data = '';
            res.on('data', (chunk) => { data += chunk; });
            res.on('end', () => {
              if ((res.statusCode ?? 0) >= 300) {
                reject(new Error(`primitives call ${fnName}: HTTP ${res.statusCode}`));
              } else {
                resolve(JSON.parse(data));
              }
            });
          }
        );
        req.on('error', reject);
        req.write(body);
        req.end();
      });
    }
  };
}

/**
 * Start the stdin/stdout processing loop.
 * Call this from your container's entry point.
 *
 * @example
 * import { run } from '@vextura/vex-sdk';
 * run(async (event, ctx) => ({
 *   eventId: event.eventId,
 *   success: true,
 *   outputData: { approved: true },
 * }));
 */
export async function run(handler: VexFnHandler): Promise<void> {
  const primitivesUrl = process.env['VEX_PRIMITIVES_URL'];
  const primitives = makePrimitivesClient(primitivesUrl);

  const rl = readline.createInterface({ input: process.stdin, crlfDelay: Infinity });

  for await (const line of rl) {
    if (!line.trim()) continue;

    let event: VexEvent;
    try {
      event = JSON.parse(line) as VexEvent;
    } catch (e) {
      const errResp: VexResponse = {
        eventId: '', success: false, errorCode: 'PARSE_ERROR',
        errorMessage: (e as Error).message
      };
      process.stdout.write(JSON.stringify(errResp) + '\n');
      continue;
    }

    const ctx: VexContext = {
      tenantId: event.metadata?.tenantId,
      traceId: event.metadata?.traceId,
      primitives,
    };

    const start = Date.now();
    let resp: VexResponse;
    try {
      resp = await handler(event, ctx);
      resp = { ...resp, eventId: event.eventId, durationMs: Date.now() - start };
    } catch (e) {
      resp = {
        eventId: event.eventId, success: false,
        errorCode: 'HANDLER_ERROR', errorMessage: (e as Error).message
      };
    }

    process.stdout.write(JSON.stringify(resp) + '\n');
  }
}
