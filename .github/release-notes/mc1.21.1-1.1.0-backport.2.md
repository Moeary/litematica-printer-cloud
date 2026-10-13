# Litematica Printer 1.1.0-backport.2 — Minecraft 1.21.1

Maintenance backport for **Minecraft 1.21.1 only**.

## Fixed

Backports upstream `184b19fd`: retain Quick Shulker return requests after timeouts or partial transfers, resolve fresh inventory endpoints on retry, and rotate blocked return candidates so one unavailable box does not prevent other returns.

The newer Servux v2 handler-gate patch targets an API absent from the supported Litematica 0.19.61; the existing 1.21.1 adapter is retained.

## Requirements and validation

- Java 21+, Minecraft 1.21.1, Fabric Loader >=0.19.3, Fabric API 0.116.17+1.21.1, MaLiLib 0.21.10, and Litematica 0.19.61.
- Quick Shulker 4 direct-protocol support is optional and required for the repaired integration path.
- The 1.21.1 production JAR builds successfully; all five core tests pass.
- This maintenance pass does not repeat the full client integration matrix or validate return recovery against an actual packet-loss server. This is a backport prerelease.

Download `litematica-printer-1.1.0-backport.2+1.21.1.jar`; `SHA256SUMS.txt` records its checksum.

## 中文说明

修复 Quick Shulker 归还物品超时或部分传输后丢失归还意图的问题，并轮换尝试被阻塞的候选潜影盒。本次已构建正式 JAR 并验证核心测试；未重复完整客户端矩阵或实服丢包场景，按回迁预发布版发布。仅适用于 Minecraft 1.21.1。
