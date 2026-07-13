# SharkEngine dev tool - gives every mod item/block to the executing player.
# Placeable parts (Block + BlockItem)
give @s sharkengine:steering_wheel 1
give @s sharkengine:thruster 1
give @s sharkengine:bug 1
give @s sharkengine:airframe_panel 1
give @s sharkengine:fuselage_frame 1
give @s sharkengine:helicopter_engine 1
give @s sharkengine:rotor_hub 1
give @s sharkengine:rotor_blade 1
give @s sharkengine:landing_skid 1
# Crafting-only intermediates (plain items, not placeable by design)
give @s sharkengine:metal_sheet 1
give @s sharkengine:rotor_shaft 1
give @s sharkengine:engine_core 1
give @s sharkengine:bearing_assembly 1
tellraw @s {"text":"[SharkEngine] Gave all 13 mod items.","color":"aqua"}
