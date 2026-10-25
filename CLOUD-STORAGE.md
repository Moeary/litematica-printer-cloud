# Cloud Storage 1.21.1 compatibility changes

This local AGPL-3.0 fork is based on water2004/litematica-printer v1.1.5,
commit `80b2d225f56038e0d9a40f07c151fc965ab8b108`. Upstream authors and license are retained.
Local changes dated 2026-10-25 are limited to Minecraft 1.21.1.

Corresponding source: https://github.com/Moeary/litematica-printer-cloud/tree/cloud/1.21.1
Server plugin: https://github.com/Moeary/Cloud-Storage

`1.1.5-cloud.1+1.21.1` adds a bounded `cloudstorage:demand` Fabric C2S payload.
Only owned CloudStorage v3 tokens use this optional protocol. In auto mode, request
the desired materials and wait for the actual server catalogue before opening.
Fixed catalogues and ordinary shulker integration retain the legacy path.

When inventory is full, return one plain, non-target block stack from main storage
through normal server-validated container clicks, then take the requested material.
Hotbar, equipment, custom components, shulkers and default blocked materials are
protected. No item is discarded or created locally. If there is no safe candidate,
restocking pauses. Extra server-specific blocked materials may require manual room.

Build using the adjacent project's pixi Java 21 environment:

```powershell
pixi run --manifest-path ../Cloud-Storage/pixi.toml pwsh -NoProfile -Command "& './gradlew.bat' '-Ptargets=1.21.1' ':1.21.1:remapJar' ':1.21.1:remapCloudTestJar' --console=plain"
```

The `cloud-test` artifact is a separate executable test driver. Never install it in
the real client. Production JARs do not include test source sets. Full corresponding
source remains in this directory, including Gradle build files, core, version sources,
license and test driver; include these when redistributing the modified binary.

The two repositories should be cloned into sibling folders for the command above.
The installed production JAR was validated with the complete local Fabric modpack,
connecting as a non-OP player to real Paper 1.21.1. Single-stack restock took 351 ms;
returning a non-target stack from a full inventory and taking a new material took
343 ms. Server-side balances were conserved and no PENDING transfer remained.
The printer retains its current job while awaiting the first catalogue response,
with a bounded timeout for missing stock. These measurements do not represent a
complete large-schematic construction benchmark.
