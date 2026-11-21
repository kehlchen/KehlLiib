# World, Location, Gameplay, and Effects

## World and chunks

`WorldService` provides world lookup and operations, while `ChunkService` provides chunk lookup/loading operations.

```java
WorldService worlds = runtime.services().getRequired(WorldService.class);
World world = worlds.find("world").orElseThrow();
worlds.setTime(world, 6_000);
```

Chunk operations must respect server ownership. Use the scheduler's location API for region-owned work on Folia.

## Blocks and locations

`BlockService` exposes bounded block operations. `LocationService` can search for safe locations and provides location utilities.

```java
LocationService locations =
    runtime.services().getRequired(LocationService.class);
SafeLocation safe = locations.findSafe(player.getLocation(), 16);
safe.location(); // teleport only if safe.safe() is true
```

`Result` failures should be inspected instead of assuming operations succeeded.

## Entities

`EntityService` provides lifecycle-managed entity operations and lookups. Bukkit entity objects are not safe to access from arbitrary network or async threads.

## Regions

`Region`, `CuboidRegion`, `SphereRegion`, and `CylinderRegion` provide contains and player lookup operations.

```java
Region region =
    new CuboidRegion(world, -10, 64, -10, 10, 80, 10);
region.players(100).forEach(inside ->
    inside.sendMessage(Component.text("Inside region")));
```

`Region` implementations use Bukkit `World`; callers must execute player and world operations with the correct ownership.

## Particles and effects

`ParticleService` spawns particles. `ParticleShape` creates points for circles and other shapes. `EffectService` manages lifecycle-backed repeating effects.

```java
ParticleService particles =
    runtime.services().getRequired(ParticleService.class);
for (Location point :
     ParticleShape.circle(player.getLocation(), 3.0, 24)) {
    particles.spawn(point, Particle.END_ROD, 1, 0, 0, 0, 0);
}
```

`EffectHandle` represents a managed effect and can be closed or tracked according to its public contract.

## Cooldowns and combat

`CooldownManager` and `CombatTracker` are in-memory, lifecycle-independent gameplay abstractions. Use bounded keys and UUID identities where appropriate.
