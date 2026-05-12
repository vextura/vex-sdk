package vexfn

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"strings"
	"time"
)

// PrimitivesClient calls other vex-fn functions or vex-gate endpoints
// from within a running container. The base URL is read from the
// VEX_PRIMITIVES_URL environment variable (injected by vex-executor).
type PrimitivesClient struct {
	baseURL    string
	httpClient *http.Client
}

// NewPrimitivesClient creates a client using the VEX_PRIMITIVES_URL env var.
// Returns nil if the variable is not set (safe — methods are nil-safe).
func NewPrimitivesClient() *PrimitivesClient {
	url := os.Getenv("VEX_PRIMITIVES_URL")
	if url == "" {
		return nil
	}
	return &PrimitivesClient{
		baseURL:    strings.TrimRight(url, "/"),
		httpClient: &http.Client{Timeout: 10 * time.Second},
	}
}

// Call invokes a named primitive/function and unmarshals the response.
func (c *PrimitivesClient) Call(ctx context.Context, fnName string, input, output any) error {
	if c == nil {
		return fmt.Errorf("primitives client not available (VEX_PRIMITIVES_URL not set)")
	}

	body, err := json.Marshal(input)
	if err != nil {
		return fmt.Errorf("primitives marshal: %w", err)
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost,
		fmt.Sprintf("%s/fn/%s", c.baseURL, fnName),
		strings.NewReader(string(body)))
	if err != nil {
		return fmt.Errorf("primitives request: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("primitives call %s: %w", fnName, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 300 {
		return fmt.Errorf("primitives call %s: HTTP %d", fnName, resp.StatusCode)
	}

	return json.NewDecoder(resp.Body).Decode(output)
}
