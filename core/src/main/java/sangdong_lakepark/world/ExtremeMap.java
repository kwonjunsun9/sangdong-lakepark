package sangdong_lakepark.world;

/** Hard's ten sections, with the longer, timed final stepping stones. */
public final class ExtremeMap extends HardMap {
    public static final float WORLD_END=17104, RUN_END=16704;
    public ExtremeMap(){super();arch.setPosition(16864,184);}
    @Override protected float[][] finishFloors(){
        return new float[][]{{15648,15744,184},{16128,16224,184},{16608,WORLD_END,184}};
    }
    @Override protected float[][] finishHills(){return new float[][]{{15648,96,144},{16128,96,48}};}
    @Override protected void addFinishGaps(){
        gap(15520,15648,15236,376,false);gap(15744,16128,15652,328,false);gap(16224,16608,16132,232,false);
    }
    @Override protected void addFinishPlatforms(){
        blinkRow(15872,280,.8f);blinkRow(16352,328,.8f);fakeRow(15872,408);fakeRow(16352,184);
    }
    @Override public float worldWidth(){return WORLD_END;}
    @Override public float stopX(){return RUN_END;}
    @Override public float birdFlightSpeed(RockSpawn spawn){return spawn.x==15312?BIRD_SPEED:super.birdFlightSpeed(spawn);}
}
