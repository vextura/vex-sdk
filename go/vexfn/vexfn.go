// Package vexfn is the VexEdge function SDK.
//
// Every VexEdge function communicates over stdin/stdout using the [VexEvent]
// wire format. vex-fn writes a JSON-encoded VexEvent (Type "fn.invoke") to
// the container's stdin as a newline-terminated line; the function must write
// a JSON-encoded VexEvent response as a newline-terminated line to stdout.
//
// In warm-pool mode the container is kept alive and serves multiple requests
// sequentially. [Handle] implements the full loop so function authors only
// write a typed handler — the container lifecycle is transparent.
//
// Minimal example:
//
//	package main
//
//	import (
//		"context"
//		"github.com/vextura/vex-sdk-go/vexfn"
//	)
//
//	func main() { vexfn.Handle(handle) }
//
//	func handle(ctx context.Context, evt vexfn.Event[ChargeInput]) (*ChargeOutput, error) {
//		return &ChargeOutput{Charged: true}, nil
//	}
package vexfn

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"os"
	"time"
)

// VEX_NATS_URL is the environment variable the SDK reads to connect a
// PrimitivesClient so step functions can call primitive containers.
const envNATSURL = "VEX_NATS_URL"

// Type constants for VexEvent.Type.
const (
	TypeInvoke = "fn.invoke"
	TypeResult = "fn.result"
	TypeError  = "fn.error"
)

// VexEvent is the canonical JSON wire format for all messages on NATS and
// over the stdin/stdout boundary.
type VexEvent struct {
	ID        string          `json:"id"`
	TraceID   string          `json:"trace_id"`
	Version   string          `json:"version"`
	Type      string          `json:"type"`
	Tenant    string          `json:"tenant"`
	Fn        string          `json:"fn"`
	Operation string          `json:"operation"`
	Mode      string          `json:"mode,omitempty"`
	Timestamp time.Time       `json:"timestamp"`
	Source    *EventSource    `json:"source,omitempty"`
	Data      json.RawMessage `json:"data,omitempty"`
	Error     *EventError     `json:"error,omitempty"`
	ReplyTo   string          `json:"reply_to,omitempty"`
}

// EventSource describes the inbound HTTP request that triggered the function.
type EventSource struct {
	Path        string            `json:"path"`
	Method      string            `json:"method"`
	PathParams  map[string]string `json:"path_params,omitempty"`
	QueryParams map[string]string `json:"query_params,omitempty"`
	Identity    string            `json:"identity,omitempty"`
	IP          string            `json:"ip,omitempty"`
}

// EventError carries a structured error from a function invocation.
type EventError struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

// Event is the typed view of a VexEvent presented to handler authors.
// Data is already decoded from VexEvent.Data into T.
type Event[T any] struct {
	ID        string
	TraceID   string
	Type      string
	Tenant    string
	Fn        string
	Operation string
	Mode      string
	Timestamp time.Time
	Source    *EventSource
	Data      T
	Raw       *VexEvent

	// primitives is the optional client for calling primitive containers.
	// Nil when VEX_NATS_URL is not set.
	primitives *PrimitivesClient
}

// Primitives returns the client for calling primitive containers via NATS.
// Returns nil when the VEX_NATS_URL environment variable is not configured
// (typical in local unit tests without NATS).
//
// Example:
//
//	result, err := evt.Primitives().Call("corebanking-charge", myRequest)
func (e *Event[T]) Primitives() *PrimitivesClient {
	return e.primitives
}

// Handler is the function signature your implementation must match.
type Handler[Req, Resp any] func(ctx context.Context, evt Event[Req]) (*Resp, error)

// maxScanToken is the maximum size of a single event line (16 MB).
const maxScanToken = 16 * 1024 * 1024

// Handle is the entry point for all VexEdge functions.
//
// Call it from main() with your typed handler. It reads newline-delimited
// [VexEvent] JSON from stdin in a loop, calling the handler for each event
// and writing the response to stdout before reading the next event. This loop
// continues until stdin is closed (EOF), which is the signal from vex-fn that
// the container is being retired.
//
// In cold-start mode (WarmInstances: 0) stdin is closed after one event, so
// the loop terminates after one invocation — identical behaviour to the old
// single-read SDK, but no code change is needed in the handler.
//
// Handler panics are caught, returned as fn.error events, and the loop
// continues so the container can serve subsequent requests.
func Handle[Req, Resp any](handler Handler[Req, Resp]) {
	scanner := bufio.NewScanner(os.Stdin)
	scanner.Buffer(make([]byte, maxScanToken), maxScanToken)

	out := bufio.NewWriter(os.Stdout)

	// Single shared PrimitivesClient for the container lifetime.
	// Tenant is not known until the first event arrives, so we lazily
	// initialise it on the first invocation.
	var primsClient *PrimitivesClient
	natsURL := os.Getenv(envNATSURL)

	for scanner.Scan() {
		line := bytes.TrimSpace(scanner.Bytes())
		if len(line) == 0 {
			continue
		}

		var wire VexEvent
		if err := json.Unmarshal(line, &wire); err != nil {
			writeErrorTo(out, &VexEvent{}, "decode_error", "decode input event: "+err.Error())
			out.Flush() //nolint:errcheck
			continue
		}

		var req Req
		if len(wire.Data) > 0 && string(wire.Data) != "null" {
			if err := json.Unmarshal(wire.Data, &req); err != nil {
				writeErrorTo(out, &wire, "decode_error", "decode event data: "+err.Error())
				out.Flush() //nolint:errcheck
				continue
			}
		}

		// Lazily create the PrimitivesClient on the first event (tenant is now known).
		if primsClient == nil && natsURL != "" {
			primsClient = newPrimitivesClient(natsURL, wire.Tenant)
		}

		evt := Event[Req]{
			ID:         wire.ID,
			TraceID:    wire.TraceID,
			Type:       wire.Type,
			Tenant:     wire.Tenant,
			Fn:         wire.Fn,
			Operation:  wire.Operation,
			Mode:       wire.Mode,
			Timestamp:  wire.Timestamp,
			Source:     wire.Source,
			Data:       req,
			Raw:        &wire,
			primitives: primsClient,
		}

		resp, err := invokeHandler(handler, context.Background(), evt)
		if err != nil {
			writeErrorTo(out, &wire, "fn_error", err.Error())
			out.Flush() //nolint:errcheck
			continue
		}

		dataBytes, err := json.Marshal(resp)
		if err != nil {
			writeErrorTo(out, &wire, "encode_error", "encode response: "+err.Error())
			out.Flush() //nolint:errcheck
			continue
		}

		result := VexEvent{
			ID:        wire.ID,
			TraceID:   wire.TraceID,
			Version:   wire.Version,
			Type:      TypeResult,
			Tenant:    wire.Tenant,
			Fn:        wire.Fn,
			Operation: wire.Operation,
			Timestamp: time.Now(),
			Data:      json.RawMessage(dataBytes),
		}
		json.NewEncoder(out).Encode(result) //nolint:errcheck
		out.Flush()                         //nolint:errcheck
	}

	if err := scanner.Err(); err != nil {
		os.Exit(1)
	}

	// Release the NATS connection when the container is retiring.
	if primsClient != nil {
		primsClient.Close()
	}
}

// invokeHandler calls the handler with panic recovery.
func invokeHandler[Req, Resp any](handler Handler[Req, Resp], ctx context.Context, evt Event[Req]) (resp *Resp, err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf("panic: %v", r)
		}
	}()
	return handler(ctx, evt)
}

// writeErrorTo writes a fn.error VexEvent to w.
func writeErrorTo(w *bufio.Writer, wire *VexEvent, code, message string) {
	errEvent := VexEvent{
		ID:        wire.ID,
		TraceID:   wire.TraceID,
		Version:   wire.Version,
		Type:      TypeError,
		Tenant:    wire.Tenant,
		Fn:        wire.Fn,
		Operation: wire.Operation,
		Timestamp: time.Now(),
		Error: &EventError{
			Code:    code,
			Message: message,
		},
	}
	json.NewEncoder(w).Encode(errEvent) //nolint:errcheck
}
