# Legacy ↔ Current AI/Movement Map

This map is a navigation aid, not proof of parity.

| Legacy 1.10.2 | Current Reforge | Notes |
|---|---|---|
| `EntityAIShipFollowOwner` | `ShipFollowOwnerGoal` | Must compare distance/teleport/path timing |
| `EntityAIShipSit` | `ShipSitGoal` | |
| `EntityAIShipFlee` | `ShipFleeGoal` + Brain PANIC state mirror | Current Brain may later become authoritative |
| `EntityAIShipGuarding` | `ShipGuardingGoal` | |
| `EntityAIShipWander` | `ShipWanderGoal` | |
| `EntityAIShipFloating` | `ShipFloatingGoal` | |
| `EntityAIShipPickItem` | `ShipPickItemGoal` | Check scheduler/flag blocking |
| `EntityAIShipRevengeTarget` | `ShipRevengeTargetGoal` | |
| `EntityAIShipRangeTarget` | `ShipRangeTargetGoal` | |
| `EntityAIShipRangeAttack` | `ShipRangeAttackGoal` | |
| `EntityAIShipAttackOnCollide` | `ShipAttackOnCollideGoal` | |
| `EntityAIShipCarrierAttack` | `ShipCarrierAttackGoal` | |
| `EntityAIShipAircraftAttack` | `ShipAircraftAttackGoal` | |
| `EntityAIShipSkillAttack` | `ShipSkillAttackGoal` | |
| `EntityAIShipOpenDoor` | `ShipOpenDoorGoal` | |
| `EntityAIShipWatchClosest` | current look-at Goal setup in `BasicEntityShip` | Verify exact behavior |
| `EntityAIShipLookIdle` | current random-look Goal setup | Verify exact behavior |
| `ShipMoveHelper` | `ShipMoveControl` | Semantic/feel comparison required |
| `ShipPathNavigate` | `ShipNavigation` | Semantic/path comparison required |
| `ShipPathFinder` | `ShipPathFinderCore` + modern pathfinder integration | |
| `ShipPath` / `ShipPathPoint` / `ShipPathHeap` | modern `Path` / `Node` plus custom integration | Do not force structural similarity |
| none | `ShipBrain` | Current-only transitional architecture |
| none | `ShipHostileWanderGoal` | Current-only; check whether it alters intended hostile UX |

## BasicEntityShip scheduling

Legacy `BasicEntityShip` registers task priorities resembling the current port, including:

- sit
- flee
- guard
- follow owner
- open door
- attack-on-collide where applicable
- floating
- wander
- watch/look

Current `BasicEntityShip` similarly registers multiple Goals and target Goals.

This structural similarity is useful for tracing, but **does not establish parity** because modern GoalSelector scheduling, flags, navigation, target handling, and tick semantics differ from 1.10.2.
