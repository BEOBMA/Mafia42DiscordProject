# Codebase Sections

This project has been reorganized into section-oriented source directories while preserving existing package names and runtime behavior.

## Sections

- `bootstrap/`: application startup entrypoint.
- `presentation/`: Discord-facing command and interaction handlers.
- `application/`: game orchestration, loop, and gameplay systems.
- `domain/`: core Mafia42 domain model (jobs, abilities, definitions).
- `infrastructure/`: integrations with external Discord and Lavalink services.

## Notes

- Kotlin package declarations remain unchanged (`org.beobma.mafia42discordproject...`).
- The reorganization is filesystem-level only, intended to improve navigation and maintenance.
- No functional behavior changes were introduced by this structure update.
