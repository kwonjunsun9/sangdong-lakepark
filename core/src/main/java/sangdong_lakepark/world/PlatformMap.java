package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/** Shared terrain types, collision tuning and geometry helpers. */
public class PlatformMap {
    public static final float BORDER_FLASH = .5f;
    public static final float VIEW_WIDTH = 1280, VIEW_HEIGHT = 720, WORLD_WIDTH = 7680;
    public static final float GROUND_Y = 184, START_X = 260, STOP_X = 7280, GOAL_X = 7440;
    public static final float MOVE_SPEED = 260, JUMP_SPEED = 650, GRAVITY = 1700, EXIT_SPEED = MOVE_SPEED * 1.5f;
    public static final float PLAYER_WIDTH = 88, PLAYER_HEIGHT = 120, FOOT_WIDTH = 44, MIN_FOOT_SUPPORT = 8;
    public static final float BLOCK_SIZE = 64, GRASS_HEIGHT = 24, ITEM_BOTTOM = 398, ITEM_HIT_MARGIN = 16;
    public static final float HIT_SPEED = 220, HIT_JUMP = 280, HIT_LOCK = .2f, INVULNERABILITY = 1;
    public static final float HIT_FLASH = .3f, FALL_Y = -100, COIN_SIZE = 36, BIKE_POP_TIME = .3f;
    public static final float BLINK_PERIOD = 4, BLINK_SOLID = 2.6f, BLINK_FADE_OUT = 2, BLINK_FADE_IN = 3.6f;
    public static final float WATER_WARNING = .6f, WATER_ACTIVE = 1.5f, WATER_REST = 2, WATER_CLOUD_Y = 600;
    public static final float RIVER_LEVEL = GROUND_Y - 64, RIVER_AMPLITUDE = 5;
    public static final float ROCK_WARNING = .8f, ROCK_SIZE = 64, ROCK_START_Y = 545, ROCK_BREAK_TIME = .4f;
    public static final float ROCK_FALL_TIME_SCALE = 2, ROCK_ROLL_SPEED = 480, ROCK_ROLL_LIFETIME = 6;
    public static final float BIRD_Y = 648, BIRD_SPEED = 140, BIRD_MARGIN = 96, BIRD_ENTRY_INSET = 112, BIRD_DRAW_SIZE = 160;
    public final Array<Rectangle> ground = new Array<>(), coins = new Array<>(), traps = new Array<>();
    public final Array<Rectangle> ceilings = new Array<>();
    public final Array<Block> blocks = new Array<>();
    public final Array<Gap> gaps = new Array<>();
    public final Array<BlinkPlatform> blinking = new Array<>();
    public final Array<RockSpawn> rocks = new Array<>();
    public final Block itemBlock = new Block(2784, ITEM_BOTTOM, BlockKind.ITEM);
    public final Rectangle bicycle = new Rectangle(2784, ITEM_BOTTOM + BLOCK_SIZE, 96, 72);
    public final Rectangle arch = new Rectangle(7440, GROUND_Y, 200, 230);
    public final WaterStream water = new WaterStream(4104, 4152);
    public final Array<WaterStream> waterStreams = new Array<>();

    public PlatformMap() { waterStreams.add(water); }
    public float worldWidth() { return WORLD_WIDTH; }
    public float stopX() { return STOP_X; }
    public boolean usesScooter() { return false; }
    public boolean keepsVehicleInPits() { return false; }
    public float rockRepeatPeriod() { return 0; }
    public float rockEntryMargin() { return BIRD_MARGIN; }
    public float birdFlightSpeed(RockSpawn spawn) { return BIRD_SPEED; }
    public boolean breaksRollingRock(Rectangle bounds) { return false; }
    public float rockRollVelocity(RockSpawn spawn) { return -ROCK_ROLL_SPEED; }
    public Iterable<? extends Rectangle> rockSurfaces(RockSpawn spawn) { return ground; }
    public Vector2[] sectionStarts = {
        new Vector2(START_X, VIEW_HEIGHT + 24), new Vector2(1288, GROUND_Y), new Vector2(2560, GROUND_Y),
        new Vector2(4032, GROUND_Y), new Vector2(5376, GROUND_Y), new Vector2(6528, GROUND_Y)
    };

    protected void floor(float from, float to) { ground.add(new Rectangle(from, 0, to - from, GROUND_Y)); }
    protected void hill(float x, float width, float height) { ground.add(new Rectangle(x, GROUND_Y, width, height)); }
    protected void squareHill(float x, float width, float height) { ground.add(new SquareGround(x, GROUND_Y, width, height)); }
    protected void row(float from, float to, float top, BlockKind kind) {
        for (float x = from; x < to; x += BLOCK_SIZE) blocks.add(new Block(x, top - BLOCK_SIZE, kind));
    }
    protected void blinkRow(float from, float top, float delay) {
        for (int cell = 0; cell < 2; cell++)
            blinking.add(new BlinkPlatform(from + cell * BLOCK_SIZE, top, delay + cell * .4f));
    }
    public static float cornerRadius(Rectangle surface) {
        if (surface instanceof SquareGround) return 0;
        return Math.min(28, Math.min(surface.width / 3, surface.height / 2));
    }
    private static final class SquareGround extends Rectangle {
        SquareGround(float x,float y,float width,float height) { super(x,y,width,height); }
    }
    /** Rendering and river entry use the same moving waterline. */
    public static float riverSurface(float x, double time) {
        double phase = x * Math.PI * 2 / 160 - time * Math.PI * 2 / 2.8;
        return RIVER_LEVEL + (float) (4 * Math.sin(phase) + Math.sin(2 * phase + .7));
    }
    protected void trap(float from, float to) { traps.add(new Rectangle(from + 8, groundTop(from), to - from - 16, 24)); }
    protected void coin(float x, float y) { coins.add(new Rectangle(x - COIN_SIZE / 2, y - COIN_SIZE / 2, COIN_SIZE, COIN_SIZE)); }
    public float groundTop(float x) {
        float top = 0;
        for (Rectangle surface : ground) if (x >= surface.x && x < surface.x + surface.width)
            top = Math.max(top, surface.y + surface.height);
        return top;
    }
    public Gap gapAt(float x) {
        Gap nearest = gaps.first(); float distance = Float.MAX_VALUE;
        for (int i = 0; i < gaps.size; i++) {
            Gap gap = gaps.get(i);
            if (x >= gap.x && x <= gap.x + gap.width) return gap;
            float d = Math.abs(x - gap.x - gap.width / 2);
            if (d < distance) { distance = d; nearest = gap; }
        }
        return nearest;
    }
    public enum BlockKind { WOOD, ITEM, STONE, CONVEYOR }
    public static class Block extends Rectangle {
        public final BlockKind kind;
        Block(float x, float y, BlockKind kind) { super(x, y, BLOCK_SIZE, BLOCK_SIZE); this.kind = kind; }
    }
    public static final class Gap extends Rectangle {
        public final Vector2 respawn;
        public final boolean lake;
        Gap(float from, float to, float x, float y, boolean lake) {
            super(from, 0, to - from, GROUND_Y); respawn = new Vector2(x, y); this.lake = lake;
        }
    }
    public static final class BlinkPlatform extends Rectangle {
        public final float delay;
        BlinkPlatform(float x, float top, float delay) {
            super(x, top - BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE); this.delay = delay;
        }
        public float phase(double time) { return (float) (((time - delay) % BLINK_PERIOD + BLINK_PERIOD) % BLINK_PERIOD); }
        public boolean solid(double time) { return phase(time) < BLINK_SOLID; }
        public float opacity(double time) {
            float p = phase(time);
            return p < BLINK_FADE_OUT ? 1 : p < BLINK_SOLID ? 1 - (p - BLINK_FADE_OUT) / .6f
                : p < BLINK_FADE_IN ? 0 : (p - BLINK_FADE_IN) / .4f;
        }
    }
    public static final class WaterStream extends Rectangle {
        WaterStream(float from, float to) { super(from, GROUND_Y, to - from, WATER_CLOUD_Y - GROUND_Y); }
        public float phase(double time) { return (float) (Math.max(0, time) % (WATER_WARNING + WATER_ACTIVE + WATER_REST)); }
        public boolean warning(double time) { return phase(time) < WATER_WARNING; }
        public boolean active(double time) { float p = phase(time); return p >= WATER_WARNING && p < WATER_WARNING + WATER_ACTIVE; }
    }
    public static final class RockSpawn {
        public final float x;
        public final boolean rolls;
        RockSpawn(float x, boolean rolls) {
            this.x = x; this.rolls = rolls;
        }
    }
}
