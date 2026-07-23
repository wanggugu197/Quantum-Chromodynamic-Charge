# Quantum Chromodynamic Charge

Quantum Chromodynamic Charge is a NeoForge mod about absurd explosive power, controlled demolition, and instant industrial-scale construction. It adds high-yield charges, activation stars, coordinate tools, an Area Destroyer for engineered destruction, and a Structure Placer for deploying preset platforms, roads, plots, and factory shells.

This is not a subtle mod. It is built for test worlds, creative engineering, server events, mapmaking, stress testing, and players who want a button that can turn a mountain into a crater.

## What This Mod Adds

### High-Yield Explosives

The mod starts with familiar TNT-like blocks and quickly escalates into world-shaping devices.

- Powder Barrel: a compact explosive with a smaller blast than the advanced charges.
- Bigger TNT: a stronger TNT-style block designed for larger demolition work.
- Nuclear Bomb: a primed explosive entity that clears a large spherical area after its fuse.
- Naquadria Charge: a dormant high-energy charge that can be triggered with the Quantum Star.
- Leptonic Charge: a stronger charge activated by the Gravi Star.
- Quantum Chromodynamic Charge: the top-tier charge, activated by the Unstable Star for extreme destruction.

Use these in disposable worlds first. Some effects are large enough to permanently reshape terrain, erase builds, and put heavy pressure on servers.

### Star-Triggered Charges

Some charges do not behave like normal TNT. They are triggered by pairing a special item with a matching charge block:

- Quantum Star + Naquadria Charge: creates a large spherical blast.
- Gravi Star + Leptonic Charge: creates a much larger spherical blast.
- Unstable Star + Quantum Chromodynamic Charge: creates an enormous spherical blast.

To use them, hold the correct star and right-click the matching charge block.

### Coordinate Positioning Card

The Coordinate Positioning Card is the utility item that ties the large machines together.

- Right-click a block to record its position.
- Sneak-use the card to clear the stored position.
- The saved coordinate is shown in the item tooltip.

Coordinate cards are used by area-based demolition and region export workflows.

## Area Destroyer

The Area Destroyer is a block-based demolition console. It accepts explosive items, reads their total yield, and converts that yield into one of several clearing modes.

Open its interface and configure:

- Mode: Sphere, Chunk, or Area.
- Explosives: insert supported explosive blocks/items to provide yield.
- Coordinate Cards: required for area mode.
- Enable switch: arms the machine.
- Detonate button: starts the clearing task.
- Heightmap and light update switches: tune world update cost and visual correctness.

### Area Destroyer Modes

Sphere mode clears a spherical region centered on the machine. This is the most direct way to create craters, underground chambers, void spheres, or controlled testing volumes.

Chunk mode clears chunks in a square pattern around the machine. It is intended for large-scale land clearing and stress testing.

Area mode clears the rectangular region between two coordinate cards. This is the precision option: mark two opposite corners, place both cards in the machine, add enough explosive yield, and detonate.

### Performance Notes

The Area Destroyer clears blocks progressively over ticks instead of trying to destroy everything in one instant. This keeps huge operations more manageable, but large jobs are still expensive.

If lighting artifacts appear after very large clears, keep light updates enabled. If you are clearing enormous test regions and only care about speed, disabling light updates can reduce load, but visual lighting may need later correction.

## Structure Placer

The Structure Placer is the constructive half of the mod. It can place built-in structure presets over time, using a material-bank system instead of simple free placement.

Its interface includes:

- Blueprint tab: choose a preset library and structure template.
- Material tab: load and unload structure component materials.
- Settings tab: adjust offset, mirroring, rotation, placement speed, heightmap updates, light updates, and block-skipping behavior.
- Export tab: export a selected world region into a structure pattern and block mapping.

### Built-In Structure Libraries

The mod ships with several preset libraries:

- Demo Preset: a simple stone platform for testing.
- Platform Standard Library Alpha: high-saturation chessboards and panel platforms.
- Platform Standard Library Beta: small, medium, large, and mixed-use plot foundations.
- Platform Extension Library: road floors and gray light-strip floors.
- Factory Standard Library: standard factory building shells and long-corridor factory structures.

These presets are useful for quickly preparing flat industrial layouts, roads, factory grids, test pads, and decorative platform networks.

### Structure Materials

The Structure Placer uses three material categories:

- Frame
- Plate
- Finish

Each category has multiple component tiers. Load components into the Structure Placer to convert them into stored material points, then spend those points when placing structures.

## Region Export

The Structure Placer can also export a region from the world.

Basic workflow:

1. Record two opposite corners with Coordinate Positioning Cards.
2. Place both cards into the Structure Placer.
3. Open the Export tab.
4. Export the region.

Exports are written as an `.mbs` structure pattern plus a `.json` block mapping under `logs/platform/`.

This is mainly useful for pack makers, builders, and developers who want to turn in-world builds into reusable structure presets.

## Server Owner Notes

Quantum Chromodynamic Charge is intentionally destructive. Before enabling it on a public server, decide who is allowed to use the high-yield blocks and machines.

Recommended server policies:

- Restrict the creative tab or item access if your server has economy, claims, or progression.
- Test the Area Destroyer and top-tier charges in a separate dimension or backup world.
- Keep backups before running large clears.
- Prefer enabling heightmap and light updates for survival-facing worlds.
- Use config switches to disable specific explosion entry points if needed.

The mod includes YAML configuration for major explosion controls, including star-triggered explosions, nuclear-bomb explosion behavior, and whether the Area Destroyer may start clearing tasks.

## Developer Notes

This project is a NeoForge mod using Gradle and Java 25.

Important implementation areas:

- Mod entry point: `QuantumChromodynamicChargeMod`
- Content registration: `QCCRegistration`
- Common event hooks: `NeoForgeCommonEvent`
- Area Destroyer UI and logic: `AreaDestroyerBlockEntity`
- Structure Placer UI and logic: `StructurePlacerBlockEntity`
- Progressive clearing engines: `SphereExplosion`, `ChunkExplosion`, `AreaExplosion`
- Fast block writes and lighting sync: `ILevel`
- Built-in structure preset registry: `StructureBuiltinPresets`
- Structure IO and mapping: `StructureFileIO`, `StructureMappingIO`, `StructureExporter`

Useful Gradle commands:

```bash
./gradlew compileJava
./gradlew build
```

On Windows:

```bat
gradlew.bat compileJava
gradlew.bat build
```

## Safety Warning

This mod can remove millions of blocks, kill entities in large regions, and permanently alter terrain. Use test worlds, backups, and server-side permission controls. If you are not prepared to lose the area, do not press the button.

## License

Quantum Chromodynamic Charge is licensed under the GNU General Public License v3.
