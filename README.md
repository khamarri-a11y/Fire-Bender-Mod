# Fire Bender Falcon Wings — Adaptive Visuals (Fabric 1.20.1)

This is a Java source project, **not a compiled or tested mod JAR**.

Added to the previous source:
- Adaptive body-oriented falcon wings: hover spread, fast downstroke, slow upstroke,
  dive/sprint folding, reduced spread on landing.
- Cosmetic Soulfire landing ring on transition from airborne to grounded.
- Energy-dependent flame trail intensity, when the scoreboard is available.
- HUD low-energy warning pulse, spend flash, and energy-used notification.
- Overdrive entry spiral, expanded brighter wings, and existing aura.
- Previous directional flamethrower/breath visuals and all datapack powers retained.

The HUD reads scoreboard `fb_energy`. The matching datapack tries to synchronize
the Origins energy resource with this scoreboard. This command needs verification
with your installed Origins version, and the HUD may be absent if it fails.
The mod does not change any power damage, costs, or keybinds.

Build using Java 17, Gradle 8.x and internet access for Fabric Loom dependencies:
    gradle build

A Gradle wrapper is not included. The code has not been compiled in this environment;
build errors or runtime integration issues may remain. Keep a backup of your current
working pack and do not install this as if it were a ready-to-use JAR.

The client currently detects wings via flying/gliding state, not directly from the
Origins B-key state. Full sync requires an additional server-to-client channel.
