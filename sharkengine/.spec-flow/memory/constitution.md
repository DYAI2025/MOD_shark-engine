# Project Constitution - Shark Engine

## Project Overview
**Name:** Shark Engine  
**Description:** Minecraft mod for moving ship contraptions using Fabric API  
**Version:** 0.0.1 (Realm-Ready Ship MVP)  
**Minecraft Version:** 1.21.1  
**Mod Loader:** Fabric  
**Java Version:** 21  

## Technical Stack
- **Language:** Java 21
- **Build Tool:** Gradle 8.8
- **Mod Loader:** Fabric Loader 0.16.5
- **API:** Fabric API 0.102.0+1.21.1
- **Mappings:** Mojang (1.21.1)
- **Testing:** JUnit 5, RPA Testing (planned)

## Architecture
- **Entity-Based Movement:** Ships are custom entities with physics
- **Blueprint System:** Block structures stored as NBT data
- **Client-Server Sync:** S2C payloads for multiplayer support
- **Block Rendering:** BlockRenderDispatcher for visual representation

## Key Features
1. **Ship Assembly:** BFS algorithm detects connected blocks via steering wheel
2. **Controllable Movement:** WASD controls for ship navigation
3. **Anchor System:** Shift-click to anchor/detach ships
4. **NBT Persistence:** Ships survive world saves/reloads
5. **Disassembly:** Shift-click anchored ships to place blocks back
6. **Multiplayer Support:** Blueprint synchronization to clients

## Development Principles
- **Test-Driven Development:** Write tests before implementation
- **Performance First:** Efficient algorithms for block detection and rendering
- **Clean Code:** Follow Java conventions and Fabric best practices
- **Documentation:** Comprehensive Javadoc and inline comments
- **Modularity:** Separate concerns (networking, rendering, logic)

## Project Structure
```
sharkengine/
├── src/main/java/dev/sharkengine/
│   ├── SharkEngineMod.java          # Main mod class
│   ├── SharkEngineModEntrypoint.java # Fabric entry point
│   ├── content/                      # Blocks, entities, tags
│   ├── net/                          # Network payloads
│   └── ship/                         # Ship logic and entity
├── src/client/java/dev/sharkengine/client/
│   ├── SharkEngineClient.java       # Client entry point
│   ├── HelmInputClient.java         # Client input handling
│   └── render/                       # Entity rendering
└── src/main/resources/
    ├── fabric.mod.json              # Mod metadata
    └── assets/                       # Models, textures, lang
```

## Testing Strategy
- **Unit Tests:** JUnit for logic validation
- **Integration Tests:** In-game testing with RPA
- **Performance Tests:** Benchmark ship assembly and rendering
- **Multiplayer Tests:** Client-server synchronization

## Git Workflow
- **Branch:** main (stable)
- **Feature Branches:** feature/{feature-name}
- **Commit Convention:** Conventional Commits
- **Code Review:** Required for all changes

## Quality Gates
- **Build:** Must compile without errors
- **Tests:** All tests must pass
- **Performance:** No significant FPS impact
- **Compatibility:** Must work in multiplayer

## Future Roadmap
1. Vertical movement (diving/flying ships)
2. Different ship types with unique properties
3. Ship upgrades and modules
4. Advanced physics (water currents, wind)
5. Ship inventory and storage
6. Multiple steering wheels with control handover
