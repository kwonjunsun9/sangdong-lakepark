# Easy artwork

- `meadow-background.png`: distant painted park backdrop.
- `ground-texture.png`: exact copy of `docs/ground-texture.png`, supplied on `main`. The top 24 pixels form the grass cap. `EasyArtwork` mirrors horizontal repeats, repeats soil vertically, and rounds only the upper terrain corners.
- `grass-terrain.png`: previous terrain asset, retained for reference; Easy uses `ground-texture.png`.
- `character-hud-atlas.png`: transparent square, four equal cells. Top left is the standing character; top right is its upper-body circular portrait; bottom left is a full heart; bottom right is a broken heart. `EasySprites` finds each cell's visible bounds at load time. The standing character and portraits share the approved original-reference design.
- `../map/rider.png`: existing riding character reused for Easy bicycle equipment. Its visible bounds come from `map.json`.
- `bird-flight-atlas.png`: transparent 1536×1024 atlas generated from the approved `docs/easy-bird-preview.png`. Three equal columns contain wings up, level, and down; the top row is the brown bird and the bottom row adds its red headband. All frames face left. The game animates the sequence 0, 1, 2, 1 at 10 frames per second.

The background and character atlas were generated from the approved mockup. Text, progress movement, menu hit areas, and life counts are managed by the game. Blocks, traps, bicycle pickup, rocks, water stream, coins, and the flower arch use colored shapes until replacement artwork is supplied.
