package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/** All Normal coordinates and tuning from NORMAL_STAGE_SPEC.md. */
public final class NormalMap extends SurfaceMap {
    public static final float SCOOTER_SPEED = MOVE_SPEED * 2, MUD_FACTOR = .5f;
    public static final float SLIDE_ACCELERATION = 1500, SLIDE_FACTOR = 3, CONVEYOR_SPEED = 195;
    public static final float STING_PERIOD = 2.44f, STING_WARNING = .5f, STING_SPEED = 380;
    public static final float STING_LIFE = 2.5f, STING_RANGE = 560, REVERSE_TIME = 1;
    public static final float DOG_X = 8672, DOG_TRIGGER = 8152, DOG_WALL = 8192, DOG_RIGHT = 9152 - 72;
    public static final float DOG_WARNING = .6f, DOG_BARK = .3f, DOG_SPEED = 320;
    public static final float FINISH_APPROACH = 672, GOAL_X = 9536 + FINISH_APPROACH;
    public static final float WORLD_END = GOAL_X + 240, RUN_END = GOAL_X - 160;

    public NormalMap() {
        belts=new Rectangle[]{new Rectangle(1888,120,256,64),new Rectangle(2240,184,256,64),new Rectangle(2592,248,256,64)};
        stingBirds=new Vector2[]{new Vector2(3136,600),new Vector2(3360,600)};
        floor(0,1760); floor(2944,4416); floor(4736,5696); floor(6240,9728); floor(9920,WORLD_END);
        hill(864,128,64); hill(992,128,128); hill(1120,128,192);
        squareHill(3552,160,96); hill(3912,88,90); hill(4000,136,198); hill(4136,160,90);
        hill(5056,128,64); hill(5184,128,128); hill(5312,128,192);
        hill(6624,96,112);
        hill(7808,128,256); hill(7936,128,160); hill(8064,128,64);
        hill(9152,128,64); hill(9280,128,128); hill(9408,128,192);
        gaps.add(new Gap(1760,2944,1440,184,false));
        gaps.add(new Gap(4416,4736,4332,184,false));
        gaps.add(new Gap(5696,6240,4960,184,true));
        gaps.add(new Gap(9728,9920,9592,184,false));
        row(6816,7072,408,BlockKind.WOOD);
        for(float x=7168;x<7360;x+=BLOCK_SIZE) blinking.add(new BlinkPlatform(x,472,.8f));
        row(7456,7712,472,BlockKind.WOOD);
        fakeRow(7168,344,3);
        itemBlock.setPosition(3712,430); bicycle.setPosition(3712,494);
        for(float x=3712;x<3904;x+=BLOCK_SIZE)blocks.add(x==itemBlock.x?itemBlock:new Block(x,430,BlockKind.WOOD));
        for(Rectangle belt:belts)conveyor(belt.x,belt.x+belt.width,belt.y+belt.height);
        blinkRow(5792,248,.8f); blinkRow(6016,296,1.6f);
        trap(3040,3104); trap(3328,3392); trap(4896,4960);
        waterStreams.clear(); waterStreams.add(new WaterStream(1608,1656)); waterStreams.add(new WaterStream(6432,6480));
        rocks.add(new RockSpawn(5376,true)); rocks.add(new RockSpawn(9488,false));
        strip(mud,3104,3328); strip(mud,4800,5056); strip(mud,6304,6560);
        strip(puddles,1504,1760); strip(puddles,5440,5696);
        coin(840,384); coin(1824,354); coin(2544,464); coin(4408,432); coin(4576,352);
        coin(4928,334); coin(5548,484); coin(5968,480); coin(7584,564); coin(8268,520);
        sectionStarts[0].set(START_X,VIEW_HEIGHT+24); sectionStarts[1].set(1928,184);
        sectionStarts[2].set(3184,184); sectionStarts[3].set(4768,184);
        sectionStarts[4].set(6624,296); sectionStarts[5].set(7968,344);
        arch.setPosition(GOAL_X,GROUND_Y);
    }
    @Override public float worldWidth() { return WORLD_END; }
    @Override public float stopX() { return RUN_END; }
    @Override public boolean usesScooter() { return true; }
    @Override public float birdFlightSpeed(RockSpawn spawn) { return spawn.rolls ? BIRD_SPEED * 1.5f : spawn.x==9488 ? BIRD_SPEED * 2 : BIRD_SPEED; }
    @Override public float conveyorSpeed(int index) { return index==1 ? -CONVEYOR_SPEED : CONVEYOR_SPEED; }
    @Override public float rockRepeatPeriod() { return 3; }
    @Override public float rockEntryMargin() { return 320; }
}
