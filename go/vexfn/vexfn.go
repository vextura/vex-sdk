// Package vexfn provides the VexEdge function runtime for Go containers.
//
// Usage:
//
//	func main() {
//	    vexfn.Run(func(ctx context.Context, event *vexfn.VexEvent) (*vexfn.VexResponse, error) {
//	        // your handler
//	        return &vexfn.VexResponse{OutputData: map[string]any{"ok": true}}, nil
//	    })
//	}
package vexfn

import (
	"bufio"
	"context"
	"encoding/json"
	"fmt"
	"os"
	"time"
)

// VexEvent is the JSON structure written to the function's stdin by vex-executor.
type VexEvent struct {
	EventID       string         `json:"eventId"`
	WorkflowRunID string         `json:"workflowRunId"`
	StepID        string         `json:"stepId"`
	InputData     map[string]any `json:"inputData"`
	Metadata      *EventMetadata `json:"metadata,omitempty"`
}

// EventMetadata carries tracing and tenant context.
type EventMetadata struct {
	TraceID  string `json:"traceId,omitempty"`
	SpanID   string `json:"spanId,omitempty"`
	TenantID string `json:"tenantId,omitempty"`
	Priority int    `json:"priority,omitempty"`
}

// VexResponse is the JSON structure the function writes to stdout.
type VexResponse struct {
	EventID      string         `json:"eventId"`
	Success      bool           `json:"success"`
	OutputData   map[string]any `json:"outputData,omitempty"`
	ErrorCode    string         `json:"errorCode,omitempty"`
	ErrorMessage string         `json:"errorMessage,omitempty"`
	DurationMs   int64          `json:"durationMs,omitempty"`
}

// Handler is a user-supplied function that processes one VexEvent.
type Handler func(ctx context.Context, event *VexEvent) (*VexResponse, error)

// Run starts the stdin/stdout processing loop.
// It blocks until stdin is closed (container shutdown).
func Run(handler Handler) {
	scanner := bufio.NewScanner(os.Stdin)
	encoder := json.NewEncoder(os.Stdout)

	for scanner.Scan() {
		line := scanner.Bytes()
		if len(line) == 0 {
			continue
		}

		var event VexEvent
		if err := json.Unmarshal(line, &event); err != nil {
			writeError(encoder, "", "PARSE_ERROR", err.Error())
			continue
		}

		start := time.Now()
		resp, err := handler(context.Background(), &event)
		durationMs := time.Since(start).Milliseconds()

		if err != nil {
			writeError(encoder, event.EventID, "HANDLER_ERROR", err.Error())
			continue
		}

		if resp == nil {
			resp = &VexResponse{}
		}
		resp.EventID = event.EventID
		resp.Success = true
		resp.DurationMs = durationMs

		if err := encoder.Encode(resp); err != nil {
			fmt.Fprintf(os.Stderr, "vexfn: encode error: %v\n", err)
		}
	}

	if err := scanner.Err(); err != nil {
		fmt.Fprintf(os.Stderr, "vexfn: scanner error: %v\n", err)
	}
}

func writeError(enc *json.Encoder, eventID, code, msg string) {
	_ = enc.Encode(&VexResponse{
		EventID:      eventID,
		Success:      false,
		ErrorCode:    code,
		ErrorMessage: msg,
	})
}
