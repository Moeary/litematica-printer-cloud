# Mapped Minecraft 1.21 backports

This owned branch keeps the mapped 1.21 builds separate from the unmapped 26.x
main branch. The starting implementation is the existing 1.21.1 port (1.1.2).
It does not claim parity with every change in upstream 1.1.3.

Included additional targets: 1.21, 1.21.2 and 1.21.10. Java 21 is required.
Example: `./gradlew -Ptargets=1.21.10 :core:test :1.21.10:remapJar`.
The existing 1.21.1 source is retained for subsequent verification.

Local production-client validation records are retained outside this repository.
Validation includes actual
survival inventory assertions, real v3/v4 Quick Shulker, both bedrock miners and
15% bidirectional loss against a real dedicated server. With Servux hand
confirmation enabled, zero wrong server blocks is the acceptance criterion.
Without confirmation, wrong/missing blocks are observations. Direct API must
not remain permanently busy; legacy stalls under loss are not a rejection.

The standalone audit drivers are separate artifacts, never included in the
production JAR. To build them, request `remapAuditJar` / `remapAuditServerJar`
and provide `-PnetworkChaosJar=<absolute matching NetworkChaos JAR>`.

The GitHub workflow checks mapped compilation and core unit tests. It does not
claim that those checks rerun all local GUI/gameplay scenarios. It does not
publish release assets on branch pushes.
