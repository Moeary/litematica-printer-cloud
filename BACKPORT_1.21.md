# Mapped Minecraft 1.21 backports

This owned branch keeps the mapped 1.21 builds separate from the unmapped 26.x
main branch. The starting implementation is the existing 1.21.1 port (1.1.2).
The verified mapped implementations include the upstream 1.1.3 delta
at b096341a114d9ffc188b385cec970ec9dd15a3a3. API-specific adapters retain the
mapped Minecraft/Litematica interfaces and Java 21.

The workflow matrix lists the verified targets. Java 21 is required.
Example: `./gradlew -Ptargets=1.21.10 :core:test :1.21.10:remapJar`.

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

The isolated production-client driver also exercises all seven upstream 4.1.2
Chain AUTO-toggle transitions through the real Minecraft mouse handler, and
three fill height filters with authoritative inventory/world assertions.
All three clients additionally exercise the upstream 1.1.3 ice-to-water scenarios:
solid/liquid/air support, repeated production at the same coordinate, and upward
and downward bubble-column preservation and recreation. Authoritative server
world state and survival ice counts are checked rather than client prediction.

Stress variants record the same-position placement cooldown separately.
The production ports preserve upstream 1.1.3 placement prediction behavior.
Minecraft 1.21.10 has a retained packet-loss failure: two wrong server blocks
in the 15% loss case with the default placement cooldown of three ticks.
A separate research branch holds an experimental anchor fix. That change is
not included in these production ports. Failed runs remain verification evidence;
mapped compilation and ordinary gameplay success do not erase stress failures.

The GitHub workflow checks mapped compilation and core unit tests. It does not
claim that those checks rerun all local GUI/gameplay scenarios. It does not
publish release assets on branch pushes.
