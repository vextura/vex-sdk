package vexfn

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/nats-io/nats.go"
)

// PrimitivesClient lets a step function call primitive functions (adapters to
// client systems) running in other vex-fn containers.
//
// The call chain:
//
//	step container (your business logic)
//	  → PrimitivesClient.Call("corebanking", data)
//	    → NATS: vexedge.<tenant>.fn.corebanking
//	      → primitive container (your client's adapter)
//	        → NATS: reply_to inbox
//	          → PrimitivesClient returns response
//
// PrimitivesClient is automatically injected into [Event] by [Handle] when the
// VEX_NATS_URL environment variable is set.  When VEX_NATS_URL is not set
// (local development without NATS), Primitives() returns nil and Call() returns
// an error — use the nil guard pattern:
//
//	if p := evt.Primitives(); p != nil {
//	    result, err := p.Call("some-fn", myData)
//	}
type PrimitivesClient struct {
	conn    *nats.Conn
	tenant  string
	timeout time.Duration
}

// newPrimitivesClient creates a PrimitivesClient connected to the given NATS server.
// Returns nil (not an error) if natsURL is empty — callers handle nil gracefully.
func newPrimitivesClient(natsURL, tenant string) *PrimitivesClient {
	if natsURL == "" {
		return nil
	}
	conn, err := nats.Connect(natsURL,
		nats.MaxReconnects(-1),
		nats.ReconnectWait(2*time.Second),
		nats.Timeout(5*time.Second),
	)
	if err != nil {
		return nil
	}
	return &PrimitivesClient{
		conn:    conn,
		tenant:  tenant,
		timeout: 120 * time.Second,
	}
}

// WithTimeout returns a copy of the client with the given per-call timeout.
func (p *PrimitivesClient) WithTimeout(d time.Duration) *PrimitivesClient {
	cp := *p
	cp.timeout = d
	return &cp
}

// Call invokes a primitive function and returns its response data.
//
// fnName is the vex-fn function name (the name field in the Smithy model).
// request is serialised to JSON and sent as VexEvent.Data.
// The returned json.RawMessage is the raw bytes of the primitive's response
// data — unmarshal into your expected type.
//
// Returns an error if the call times out, NATS is unavailable, or the
// primitive container returns an error field in its response.
func (p *PrimitivesClient) Call(fnName string, request interface{}) (json.RawMessage, error) {
	return p.CallContext(context.Background(), fnName, request)
}

// CallContext is like Call but respects a context deadline or cancellation.
func (p *PrimitivesClient) CallContext(ctx context.Context, fnName string, request interface{}) (json.RawMessage, error) {
	payload, err := json.Marshal(request)
	if err != nil {
		return nil, fmt.Errorf("primitives: marshal request: %w", err)
	}

	inbox := nats.NewInbox()
	replyCh := make(chan *nats.Msg, 1)
	sub, err := p.conn.Subscribe(inbox, func(m *nats.Msg) {
		select {
		case replyCh <- m:
		default:
		}
	})
	if err != nil {
		return nil, fmt.Errorf("primitives: subscribe reply: %w", err)
	}
	defer sub.Unsubscribe() //nolint:errcheck

	evt := struct {
		ID        string          `json:"id"`
		Type      string          `json:"type"`
		Tenant    string          `json:"tenant"`
		Fn        string          `json:"fn"`
		Operation string          `json:"operation"`
		Mode      string          `json:"mode"`
		Data      json.RawMessage `json:"data"`
		ReplyTo   string          `json:"reply_to"`
	}{
		ID:        newID(),
		Type:      "invoke",
		Tenant:    p.tenant,
		Fn:        fnName,
		Operation: fnName,
		Mode:      "sync",
		Data:      payload,
		ReplyTo:   inbox,
	}

	evtBytes, err := json.Marshal(evt)
	if err != nil {
		return nil, fmt.Errorf("primitives: marshal event: %w", err)
	}

	subject := fmt.Sprintf("vexedge.%s.fn.%s", p.tenant, fnName)
	if err := p.conn.Publish(subject, evtBytes); err != nil {
		return nil, fmt.Errorf("primitives: publish to %s: %w", subject, err)
	}

	timeout := p.timeout
	if deadline, ok := ctx.Deadline(); ok {
		if remaining := time.Until(deadline); remaining > 0 && remaining < timeout {
			timeout = remaining
		}
	}

	select {
	case msg := <-replyCh:
		var resp struct {
			Data  json.RawMessage `json:"data"`
			Error string          `json:"error,omitempty"`
		}
		if err := json.Unmarshal(msg.Data, &resp); err != nil {
			return nil, fmt.Errorf("primitives: unmarshal response: %w", err)
		}
		if resp.Error != "" {
			return nil, fmt.Errorf("primitive %s error: %s", fnName, resp.Error)
		}
		return resp.Data, nil

	case <-time.After(timeout):
		return nil, fmt.Errorf("primitives: %s timed out after %s", fnName, timeout)

	case <-ctx.Done():
		return nil, ctx.Err()
	}
}

// Close releases the underlying NATS connection.
// Call this when the container is shutting down; Handle() calls it automatically.
func (p *PrimitivesClient) Close() {
	if p != nil && p.conn != nil {
		p.conn.Drain() //nolint:errcheck
	}
}

// newID generates a simple unique ID.
func newID() string {
	return fmt.Sprintf("%d", time.Now().UnixNano())
}
