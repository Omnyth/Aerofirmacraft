# GitHub Desktop Workflow

Repository folder:

`A:\AFC\Aerofirmacraft`

## Commit style

Make small commits after each working change.

Good commit examples:

- `Initial Aerofirmacraft scaffold`
- `Add permissions tracking`
- `Add starter dirt recipe`
- `Update Create and Aeronautics versions`
- `Add GregTech optional dependency list`

Avoid vague commits like:

- `changed stuff`
- `fix`
- `updates`

## What to commit

Commit:

- README.md
- docs/
- scripts/
- kubejs/
- datapacks/
- config templates
- mod list files
- permission tracking files

Do not commit:

- downloaded .jar files
- logs
- crash reports
- saves
- screenshots
- launcher/runtime folders

## Basic workflow

1. Make one focused change.
2. Launch/test the pack.
3. If it works, commit in GitHub Desktop.
4. If it breaks, revert or discard the change.
5. Keep mod jars local and tracked only through the mod list files.
