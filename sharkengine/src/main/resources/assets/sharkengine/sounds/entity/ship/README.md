# Sound Files for Shark Engine

This directory should contain the following sound files:

## Required Sounds

### thruster_idle.ogg
- **Purpose:** Quiet thruster hum when hovering in place
- **Duration:** 2-3 seconds (looping)
- **Volume:** Quiet (30-40% max)
- **Format:** Mono, 44.1kHz, Ogg Vorbis
- **Size:** < 100KB
- **Suggested Source:** 
  - Freesound.org: Search for "jet idle", "engine hum", "thruster idle"
  - License: CC0 or CC-BY (check attribution requirements)

### thruster_active.ogg
- **Purpose:** Loud thruster noise during acceleration
- **Duration:** 3-5 seconds (looping)
- **Volume:** Loud (70-80% max)
- **Format:** Mono, 44.1kHz, Ogg Vorbis
- **Size:** < 150KB
- **Suggested Source:**
  - Freesound.org: Search for "jet flyby", "rocket thrust", "thruster boost"
  - License: CC0 or CC-BY (check attribution requirements)

## Temporary Placeholder (Development Only)

For development and testing, you can use placeholder sounds:

1. **Generate with FFmpeg:**
   ```bash
   # Generate thruster_idle.ogg (low hum)
   ffmpeg -f lavfi -i "sine=frequency=200:duration=3" -vol 0.3 thruster_idle.ogg
   
   # Generate thruster_active.ogg (higher pitch, louder)
   ffmpeg -f lavfi -i "sine=frequency=400:duration=5" -vol 0.7 thruster_active.ogg
   ```

2. **Or copy from Minecraft:**
   - Copy any existing sound from `.minecraft/assets/sounds/`
   - Rename to `thruster_idle.ogg` and `thruster_active.ogg`
   - Note: Only for local development, not for distribution!

## Adding Sounds to the Project

Once you have the .ogg files:

1. Place them in this directory:
   ```
   src/main/resources/assets/sharkengine/sounds/entity/ship/
   ├── thruster_idle.ogg
   └── thruster_active.ogg
   ```

2. Update `sounds.json` in `src/main/resources/assets/sharkengine/`:
   ```json
   {
     "entity.ship.thruster_idle": {
       "sounds": ["sharkengine:entity/ship/thruster_idle"]
     },
     "entity.ship.thruster_active": {
       "sounds": ["sharkengine:entity/ship/thruster_active"]
     }
   }
   ```

3. Test in-game with:
   ```
   /playsound sharkengine:entity.ship.thruster_idle @a
   /playsound sharkengine:entity.ship.thruster_active @a
   ```

## License Compliance

⚠️ **IMPORTANT:** Before distributing the mod:
- Ensure all sound files have appropriate licenses
- Add attribution to CREDITS.txt if required (CC-BY)
- Remove any placeholder sounds used during development

Recommended license for project sounds: **CC0** (public domain) or **CC-BY 4.0** (attribution required)
