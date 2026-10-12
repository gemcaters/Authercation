# AltAuth

A Minecraft Fabric authentication mod targeting **Minecraft 26.3**, designed to support Microsoft and Ely.by accounts while preserving `online-mode=true`.

## Features

- Microsoft account authentication
- Ely.by account authentication
- Server-side session verification
- Split client and common source sets
- Secure authentication with no offline fallback

## Architecture

- **Common:** Shared authentication logic, configuration, and server integration.
- **Client:** Account management UI and client-side integration.
- **Authentication:** Provider-specific session verification and login integration.

## Security

AltAuth requires valid authentication sessions and does not trust usernames alone. Authentication failures must be rejected, and account credentials or access tokens must never be logged or stored in plaintext.

## Compatibility

- Minecraft 26.3
- Fabric Loader
- Microsoft accounts with valid Minecraft Java Edition access
- Ely.by accounts through a compatible authentication integration

The server must retain `online-mode=true`. Provider compatibility must be verified against the actual Minecraft authentication pipeline.

enforce-secure-profile=false is recommended, players joining via third-party providers don't have signed chat keys

## Status

**Work in progress.** Authentication provider integration and Minecraft 26.3 compatibility are under development.

## License

To be determined.
