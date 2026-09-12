# ShinColle UX Parity Matrix

This is a living development document.

Initial status values are intentionally conservative.

Status values: `NOT_AUDITED`, `IN_PROGRESS`, `PARITY_CANDIDATE`, `VERIFIED`, and `KNOWN_DIFFERENCE`.

| Area | Feature | Legacy reference | Current reference | Status | Automated check | Manual/in-game check | Notes |
|---|---|---|---|---|---|---|---|
| AI | Follow owner | `EntityAIShipFollowOwner` | `ShipFollowOwnerGoal` | NOT_AUDITED | TODO | TODO | Brain migration may replace current Goal |
| AI | Sit / wait | `EntityAIShipSit` | `ShipSitGoal` | NOT_AUDITED | TODO | TODO | |
| AI | Flee | `EntityAIShipFlee` | `ShipFleeGoal` / `ShipBrain` PANIC mirror | NOT_AUDITED | TODO | TODO | Check health threshold and owner dependency |
| AI | Guarding | `EntityAIShipGuarding` | `ShipGuardingGoal` | NOT_AUDITED | TODO | TODO | |
| AI | Wander | `EntityAIShipWander` | `ShipWanderGoal` | NOT_AUDITED | TODO | TODO | |
| AI | Floating | `EntityAIShipFloating` | `ShipFloatingGoal` | NOT_AUDITED | TODO | TODO | Movement feel matters |
| AI | Pick item | `EntityAIShipPickItem` | `ShipPickItemGoal` | NOT_AUDITED | TODO | TODO | Check Goal blocking interactions |
| AI | Revenge target | `EntityAIShipRevengeTarget` | `ShipRevengeTargetGoal` | NOT_AUDITED | TODO | TODO | |
| AI | Range target | `EntityAIShipRangeTarget` | `ShipRangeTargetGoal` | NOT_AUDITED | TODO | TODO | |
| AI | Fuel-driven selector lifecycle | legacy `BasicEntityShip` fuel/task refresh | deferred refresh in `BasicEntityShip.aiStep` | PARITY_CANDIDATE | Selector refresh GameTest passes | TODO: exhaust/refuel during active combat | Fuel changes apply at the next safe server AI boundary; running Goals are stopped before replacement |
| Combat | Ranged attack | `EntityAIShipRangeAttack` | `ShipRangeAttackGoal` | IN_PROGRESS | Compile/build + regression suite | TODO: UseMelee chase + cannon animation/particles | Restored legacy strict range boundary, 32-tick pursuit refresh, 64-tick combat-parameter refresh, and attacker/target feedback; movement feel and LOS timing still need in-game verification |
| Combat | Melee/collide attack | `EntityAIShipAttackOnCollide` | `ShipAttackOnCollideGoal` | NOT_AUDITED | TODO | TODO | |
| Combat | Skill attack | `EntityAIShipSkillAttack` | `ShipSkillAttackGoal` | NOT_AUDITED | TODO | TODO | |
| Carrier | Carrier attack | `EntityAIShipCarrierAttack` | `ShipCarrierAttackGoal` | IN_PROGRESS | Hostile carrier GoalSelector launch GameTest passes | TODO: visually confirm Akagi/Kaga launch timing and positioning | Restored legacy strict range boundary, 32-tick pursuit refresh, 64-tick combat-parameter refresh, hostile 10/10 effective aircraft stock, launch height, and Akagi/Kaga carrier Goal |
| Aircraft | Aircraft attack | `EntityAIShipAircraftAttack` | `ShipAircraftAttackGoal` | IN_PROGRESS | Compile + hostile carrier launch GameTest | TODO: launch target retention, 16-tick retarget cadence, return-to-host behavior | Preserves the launch target across selector rebuild, scans every 16 ticks, and falls back to the live host target |
| Combat | Attack result text | `ParticleHelper.spawnAttackTextParticle` / `ParticleTexts` | same symbols | PARITY_CANDIDATE | Compile + texture hash comparison | TODO: inspect Miss/Critical/Double/Triple/Dodge at normal and near-camera distances | Restored the legacy server-side packet condition; texture and world-space quad dimensions match legacy, so close-camera scaling was not altered |
| AI | Watch closest | `EntityAIShipWatchClosest` | vanilla/custom look Goal in current ship setup | NOT_AUDITED | TODO | TODO | Verify exact modern owner |
| AI | Look idle | `EntityAIShipLookIdle` | current look-around behavior | NOT_AUDITED | TODO | TODO | Verify exact modern owner |
| AI | Open door | `EntityAIShipOpenDoor` | `ShipOpenDoorGoal` | NOT_AUDITED | TODO | TODO | |
| Navigation | Ship path navigation | `ShipPathNavigate` | `ShipNavigation` | NOT_AUDITED | TODO | TODO | Path result and motion feel both matter |
| Navigation | Path finder | `ShipPathFinder` | `ShipPathFinderCore` / vanilla `PathFinder` integration | NOT_AUDITED | TODO | TODO | |
| Navigation | Move helper/control | `ShipMoveHelper` | `ShipMoveControl` | NOT_AUDITED | TODO | TODO | Turn rate / vertical movement / water/fly behavior |
| Entity | Owner/taming interaction | legacy `BasicEntityShip` and helpers | current `BasicEntityShip` / `TamableAnimal` | NOT_AUDITED | TODO | TODO | |
| Entity | Ship persistent state | legacy ship NBT/capability state | `CapaShipSavedValues` / current entity state | NOT_AUDITED | TODO | TODO | |
| Entity | Inventory | legacy ship inventory capability/container | current `CapaShipInventory` / `ContainerShipInventory` | NOT_AUDITED | TODO | TODO | |
| GUI | Ship inventory GUI | legacy GUI | `GuiShipInventory` | NOT_AUDITED | TODO | TODO | Layout + interaction semantics |
| GUI | Formation | legacy GUI | `GuiFormation` | NOT_AUDITED | TODO | TODO | |
| GUI | Small shipyard | legacy GUI | `GuiSmallShipyard` | NOT_AUDITED | TODO | TODO | |
| GUI | Large shipyard | legacy GUI | `GuiLargeShipyard` | NOT_AUDITED | TODO | TODO | |
| GUI | Desk | legacy GUI | `GuiDesk` | NOT_AUDITED | TODO | TODO | |
| GUI | Crane | legacy GUI | `GuiCrane` | NOT_AUDITED | TODO | TODO | |
| GUI | Vol Core | legacy GUI | `GuiVolCore` | NOT_AUDITED | TODO | TODO | |
| GUI | Recipe paper | legacy GUI | `GuiRecipePaper` | NOT_AUDITED | TODO | TODO | |
| Interaction | Pointer / owner commands | legacy item/input flow | `PointerItem`, input/network handlers | NOT_AUDITED | TODO | TODO | |
| Interaction | Fishing | legacy ship fishing behavior | current `EntityShipFishingHook` flow | NOT_AUDITED | TODO | TODO | |
| Interaction | Mount/riding | legacy mount flow | current mount entities/input | NOT_AUDITED | TODO | TODO | |
| Audio/Visual | Emotion / reaction feedback | legacy packets/sounds/render state | current packets/sounds/emotion helpers | NOT_AUDITED | TODO | TODO | |
| Network | Entity sync | legacy S2C sync | `S2CEntitySyncPacket` | NOT_AUDITED | TODO | TODO | Judge resulting UX, not packet similarity |
| Network | GUI sync/input | legacy GUI packets | current GUI packets | NOT_AUDITED | TODO | TODO | |
| Gameplay | Config-driven behavior | legacy configs | `ConfigHandler` / current config | NOT_AUDITED | TODO | TODO | |
| Gameplay | Team/faction targeting | legacy Team/Target helpers | current `TeamData`, `TargetHelper` | NOT_AUDITED | TODO | TODO | |
| Gameplay | World/resource progression | legacy worldgen/loot | current worldgen/loot/datagen | NOT_AUDITED | TODO | TODO | Player progression parity |
