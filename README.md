# vex-sdk

Vextura multi-language SDK monorepo. All client SDKs are generated from / aligned with the Smithy IDL definitions in `smithy/`.

| Language | Directory | Package |
|---|---|---|
| Go | `go/` | `github.com/vextura/vex-sdk/go` |
| Java | `java/` | `io.vextura:vex-sdk-java` (GitHub Packages) |
| TypeScript | `ts/` | `@vextura/vex-sdk` |

## Repository layout

```
vex-sdk/
├── smithy/          # Smithy IDL — source of truth for all SDKs
│   ├── traits/      # Custom traits: vexfn, vexgate, vexcommon
│   ├── prelude/     # Platform-level shape definitions
│   └── examples/    # Annotated example service models
├── go/              # Go SDK
├── java/            # Java 21 SDK (Gradle)
├── ts/              # TypeScript / Node.js SDK
└── .github/
    └── workflows/
        └── release.yml
```

## Quick links

- [Go SDK](go/README.md)
- [Java SDK](java/README.md)
- [TypeScript SDK](ts/README.md)
- [Smithy traits](smithy/)
