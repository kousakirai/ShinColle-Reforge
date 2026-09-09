# ShinColle-Reforge Project Context

This file describes the repository snapshot analyzed when the Codex instruction package was prepared.

## Current project

| Field | Value |
|---|---|
| Project | ShinColle-Reforge |
| Mod ID | `shincolle` |
| Root package | `com.lulan.shincolle` |
| Current mod version | `1.20.1.0.8.1` |
| Minecraft | `1.20.1` |
| Forge | `47.4.0` |
| ForgeGradle | `[6.0.16,6.2)` |
| Mappings | Parchment `2023.09.03-1.20.1` |
| Java | 17 |
| Main mod class | `com.lulan.shincolle.ShinColle` |
| Main entity registry | `com.lulan.shincolle.init.ModEntities` |
| Networking | `com.lulan.shincolle.network.ModNetworking` |
| Common config | `shincolle-common.toml` / `ConfigHandler` |

The build uses the Gradle Wrapper and has `-Xlint:all` enabled for Java compilation.

## Legacy UX reference

| Field | Value |
|---|---|
| Path | `temp_1_10_2` |
| Legacy mod version | `1.10.2.7.0` |
| Minecraft | `1.10.2` |
| Forge | `12.18.3.2221` |
| MCP mappings | `stable_29` |
| Java | 8 |

The legacy directory is a **read-only behavioral reference**.

## Source correspondence

At the analyzed snapshot:

- current `com/lulan/shincolle`: 446 Java files
- legacy `temp_1_10_2/.../com/lulan/shincolle`: 446 Java files
- 370 Java files share the same relative path
- 76 are current-only
- 76 are legacy-only

This high structural overlap is useful for parity analysis, but path/name similarity must not be mistaken for behavior parity.

## Current major packages

Approximate current Java file counts:

- `client`: 130
- `entity`: 113
- `item`: 42
- `block`: 26
- `utility`: 24
- `ai`: 22
- `tileentity`: 15
- `command`: 11
- `init`: 9
- `reference`: 9
- `network`: 7
- `capability`: 7
- `crafting`: 6
- `gametest`: 4

## Current AI architecture

Important current files include:

```text
com/lulan/shincolle/ai/brain/ShipBrain.java
com/lulan/shincolle/ai/ShipFollowOwnerGoal.java
com/lulan/shincolle/ai/ShipRangeAttackGoal.java
com/lulan/shincolle/ai/ShipCarrierAttackGoal.java
com/lulan/shincolle/ai/ShipAircraftAttackGoal.java
com/lulan/shincolle/ai/ShipAttackOnCollideGoal.java
com/lulan/shincolle/ai/ShipFleeGoal.java
com/lulan/shincolle/ai/ShipGuardingGoal.java
com/lulan/shincolle/ai/ShipFloatingGoal.java
com/lulan/shincolle/ai/ShipWanderGoal.java
com/lulan/shincolle/ai/ShipPickItemGoal.java
com/lulan/shincolle/ai/ShipRangeTargetGoal.java
com/lulan/shincolle/ai/path/ShipNavigation.java
com/lulan/shincolle/ai/path/ShipMoveControl.java
com/lulan/shincolle/ai/path/ShipNodeEvaluator.java
com/lulan/shincolle/ai/path/ShipPathFinderCore.java
```

`BasicEntityShip` currently:

- extends `TamableAnimal`
- uses custom `ShipNavigation`
- uses custom `ShipMoveControl`
- provides a Minecraft `Brain`
- syncs state through `ShipBrain`
- also registers multiple `GoalSelector`/`targetSelector` Goals

`ShipBrain` is currently transitional:

- declares common memories such as `ATTACK_TARGET`
- registers CORE/IDLE/FIGHT/PANIC activities
- currently has no custom sensors
- currently mirrors state such as target/hurt/flee state
- comments explicitly state that legacy GoalSelector still owns movement and weapon execution

This transitional architecture is **not** considered final.

Brain migration may replace it.

## Legacy AI architecture

Legacy equivalents include:

```text
EntityAIShipFollowOwner
EntityAIShipRangeAttack
EntityAIShipCarrierAttack
EntityAIShipAircraftAttack
EntityAIShipAttackOnCollide
EntityAIShipFlee
EntityAIShipGuarding
EntityAIShipFloating
EntityAIShipWander
EntityAIShipPickItem
EntityAIShipRangeTarget
EntityAIShipRevengeTarget
EntityAIShipSit
EntityAIShipSkillAttack
EntityAIShipOpenDoor
EntityAIShipWatchClosest
EntityAIShipLookIdle
```

Legacy custom movement/pathing includes:

```text
ShipMoveHelper
ShipPathNavigate
ShipPathFinder
ShipPath
ShipPathHeap
ShipPathPoint
```

`BasicEntityShip` in 1.10.2 registers legacy AI tasks with priorities that closely resemble many current Goal priorities.

Parity analysis must still verify semantics and timing rather than assuming the current ports are equivalent.

## Current GameTests

The repository currently contains:

```text
ShinColleEntityRegistryGameTests
ShinColleShipStateGameTests
ShinColleCombatGameTests
ShinColleGameTestRegistration
```

Use these as a base for deterministic parity tests where suitable.

## Current GUI/menu areas

Current player-facing GUIs include:

```text
GuiSmallShipyard
GuiLargeShipyard
GuiShipInventory
GuiFormation
GuiDesk
GuiCrane
GuiVolCore
GuiRecipePaper
GuiBook
```

Inventory/menu/container implementations also exist under:

```text
com/lulan/shincolle/client/gui/inventory
```

GUI parity must compare layout, interaction sequence, enabled/disabled states, inventory semantics, and network synchronization—not just whether a screen opens.

## Important gameplay/state systems

Investigate these when relevant:

```text
ConfigHandler
ServerDataManager
ShinWorldData
CapaShipInventory
CapaShipSavedValues
CapaTeitoku
TeamData
TargetHelper
FormationHelper
InteractHelper
CombatHelper
EntityHelper
InventoryHelper
PacketHelper
ID
Values
Attrs / AttrsAdv
```

## Project priority

The source hierarchy is:

```text
explicit user requirement
        ↓
legacy UX behavior
        ↓
correct Forge 1.20.1 implementation
        ↓
maintainable modern architecture
        ↓
current Reforge architecture
        ↓
minimal diff / style
```
