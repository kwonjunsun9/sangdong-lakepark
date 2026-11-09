package sangdong_lakepark.world;

/** Easy layout; common geometry and tuning live in PlatformMap. */
public final class EasyMap extends PlatformMap {
    @Override public float rockRepeatPeriod() { return 3; }

    public EasyMap() {
        floor(0, 1160); floor(1288, 1888); floor(2304, 3608); floor(3808, 4384);
        floor(5312, 6240); floor(6368, 6976); floor(7168, WORLD_WIDTH);
        hill(600, 128, 48); hill(1000, 128, 88); hill(1408, 128, 100);
        hill(1600, 160, 200); hill(1760, 128, 110);
        hill(2976, 88, 90); hill(3064, 136, 198); hill(3200, 160, 90);
        hill(5504, 256, 48); hill(5760, 256, 96); hill(6016, 224, 144);
        hill(6624, 96, 96); hill(6720, 96, 144); hill(6816, 160, 192);
        gaps.add(new Gap(1160, 1288, 1004, 272, false));
        gaps.add(new Gap(1888, 2304, 1640, 384, false));
        gaps.add(new Gap(3608, 3808, 3384, 184, false));
        gaps.add(new Gap(4384, 5312, 4250, 184, true));
        gaps.add(new Gap(6240, 6368, 6118, 328, false));
        gaps.add(new Gap(6976, 7168, 6868, 376, false));
        row(1984, 2176, 280, BlockKind.WOOD);
        for (float x = 2720; x < 2912; x += BLOCK_SIZE)
            blocks.add(x == itemBlock.x ? itemBlock : new Block(x, ITEM_BOTTOM, BlockKind.WOOD));
        row(4896, 5024, 360, BlockKind.STONE);
        blinkRow(4448, 248, .8f);
        blinkRow(4672, 296, 1.6f);
        blinkRow(5120, 280, 2.4f);
        trap(800, 864); trap(1776, 1840); trap(3544, 3608); trap(3936, 4000);
        rocks.add(new RockSpawn(5632, false)); rocks.add(new RockSpawn(6176, true));
        rocks.add(new RockSpawn(6896, false));
        coin(1048, 334); coin(1680, 444); coin(2080, 334); coin(3708, 484); coin(3968, 294);
        coin(4704, 624); coin(5072, 484); coin(6176, 384); coin(6304, 634); coin(7072, 679);
    }
}
