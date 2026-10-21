# Mapped Minecraft 1.21 / Printer 1.1.3

Production builds target 1.21, 1.21.2 and 1.21.10 with Java 21.
They preserve upstream 1.1.3 placement behavior and the existing local-prediction
setting. The additional automatic no-prediction change is not included.

The experimental change and its evidence remain on
`dory/printer-1.1.3-anchor-fix-candidate` for separate research.

Native-client checks cover ordinary printing, direct and genuine legacy Quick
Shulker, optional ChainVein, height filters, ice-to-water and bubble columns.
Two released bedrock miners and real Servux/TCP packet-loss fixtures were run.

Known failure: an unchanged-upstream 1.21.10 default-cooldown 15% bidirectional
loss case records two wrong server blocks. A controlled case dropping eight
placement packets while keeping hand switches reproduces phantom support
blocks. The experiment fixes that reproduction, but is not shipped as an
upstream behavior change. Earlier failures remain evidence. 1.21 and 1.21.2
specified loss cases passed; full convergence under random loss is not asserted.

CI builds the three mapped targets and runs the five core unit cases. It does
not claim to run the native GUI/gameplay checks on every branch push.
