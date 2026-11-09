package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import java.util.TreeSet;

/** Ten-section sunset course with distinct purple and brown equipment stations. */
public class HardMap extends SurfaceMap {
    public static final float WORLD_END=16560, RUN_END=16160;
    public static final float BICYCLE_EXIT=5632, SCOOTER_EXIT=9472, LAKE_BIRD_X=6080;
    public final Array<Block> ropeBridge=new Array<>(), caveBridge=new Array<>(), bicycleStep=new Array<>();
    public final Array<HardBoxes.Box> boxes=new Array<>();
    private final Array<Rectangle> rockTerrain=new Array<>();
    public final Rectangle ceiling=new Rectangle(8640,320,768,400);
    public final Gap motorcyclePit;

    public HardMap(){
        float[][] floors={{0,448,184},{864,1472,184},{1632,1888,184},{2656,2848,184},{2848,3648,24},
            {3648,3744,184},{4320,4576,184},{4832,4928,184},{5184,5280,184},{5600,6112,184},
            {7392,7552,184},{7680,8704,184},{9344,9632,184},{9760,9856,184},{10208,10304,184},
            {10656,10752,184},{10880,11872,184},{12544,13920,184},{14048,15520,184}};
        floors=concat(floors,finishFloors());
        float[][] hills={{960,96,48},{1056,96,96},{1152,128,144},{4320,256,224},{4832,96,240},
            {5184,96,256},{5600,128,160},{5728,128,80},{9760,96,48},{10208,96,32},{10656,96,64},
            {10880,128,176},{11008,96,112},{12544,1024,128},{13568,96,264},{13664,256,400},
            {14048,96,90},{15040,96,64},{15136,96,128},{15232,288,192}};
        hills=concat(hills,finishHills());
        TreeSet<Float> edges=new TreeSet<>();
        for(float[] f:floors){edges.add(f[0]);edges.add(f[1]);}
        for(float[] h:hills){edges.add(h[0]);edges.add(h[0]+h[1]);}
        Float previous=null;
        for(float edge:edges){
            if(previous!=null){
                float mid=(previous+edge)/2,top=0;
                for(float[] f:floors)if(mid>=f[0]&&mid<f[1])top=f[2];
                for(float[] h:hills)if(mid>=h[0]&&mid<h[0]+h[1])top=Math.max(top,GROUND_Y+h[2]);
                if(top>0)ground.add(new Rectangle(previous,0,edge-previous,top));
            }
            previous=edge;
        }
        ground.add(ceiling);ceilings.add(ceiling);
        gap(448,864,340,184,false);gap(1472,1632,1172,328,false);gap(1888,2656,1648,184,false);
        gap(3744,4320,3656,184,false);gap(4576,4832,4342,408,false);gap(4928,5184,4836,424,false);
        gap(5280,5600,5188,440,false);gap(8704,9344,8264,184,false);gap(6112,7392,5920,184,true);
        gap(7552,7680,7420,184,false);gap(9632,9760,9472,184,false);gap(9856,10208,9764,232,false);
        gap(10304,10656,10212,216,false);gap(10752,10880,10660,248,false);
        motorcyclePit=gap(11872,12544,12576,312,false);gap(13920,14048,13676,584,false);
        addFinishGaps();
        row(576,768,248,BlockKind.WOOD);row(6240,6432,248,BlockKind.WOOD);row(6816,7008,248,BlockKind.WOOD);
        for(float x=7872;x<8000;x+=64){Block b=new Block(x,312,BlockKind.WOOD);blocks.add(b);bicycleStep.add(b);}
        belts=new Rectangle[]{new Rectangle(1952,216,192,64),new Rectangle(2208,280,192,64),new Rectangle(2464,216,192,64)};
        for(Rectangle belt:belts)conveyor(belt.x,belt.x+belt.width,belt.y+belt.height);
        for(float x=3680;x<4320;x+=32){Block b=new Block(x,344,BlockKind.WOOD);b.setWidth(32);blocks.add(b);ropeBridge.add(b);}
        for(float x=8704;x<9344;x+=64){Block b=new Block(x,120,BlockKind.STONE);blocks.add(b);caveBridge.add(b);}
        box(2976,0,208,24,HardBoxes.Kind.PURPLE);box(3424,0,208,24,HardBoxes.Kind.PURPLE);
        box(7744,1,384,184,HardBoxes.Kind.BROWN);box(8320,1,334,184,HardBoxes.Kind.PURPLE);
        for(float x=11232;x<11936;x+=64){
            if(x==11360||x==11552||x==11744)box(x,2,334,184,HardBoxes.Kind.BROWN);
            else blocks.add(new Block(x,334,BlockKind.WOOD));
        }
        itemBlock.setPosition(-10000,-10000);bicycle.setPosition(-10000,-10000);
        blinkCells(6528,216,.8f,3);blinkCells(7104,312,1.6f,3);
        blinkRow(9968,216,.8f);blinkRow(10416,296,.8f);fakeRow(9968,328);fakeRow(10416,184);
        trap(2720,2784);trap(14256,14296);trap(14296,14336);
        strip(mud,1632,1888);strip(mud,14336,14720);
        strip(puddles,1280,1472);puddles.add(new Rectangle(6240,248,192,1));
        waterStreams.clear();waterStreams.add(new WaterStream(1776,1824));
        rocks.add(new RockSpawn(LAKE_BIRD_X,false));rocks.add(new RockSpawn(11392,true));rocks.add(new RockSpawn(15312,false));
        stingBirds=new Vector2[]{new Vector2(3872,660),new Vector2(4880,600)};
        coin(704,414);coin(1552,354);coin(2176,474);coin(2752,384);coin(4032,584);
        coin(9024,244);coin(6368,414);coin(6768,464);coin(12416,514);coin(13440,514);
        sectionStarts=new Vector2[]{new Vector2(260,744),new Vector2(1648,184),new Vector2(2652,184),
            new Vector2(4342,408),new Vector2(5920,184),new Vector2(7456,184),new Vector2(9472,184),
            new Vector2(10884,360),new Vector2(12612,312),new Vector2(14052,274)};
        arch.setPosition(16320,184);
        addFinishPlatforms();
        rockTerrain.addAll(ground);
        for(Block b:blocks)if(isEquipmentRow(b))rockTerrain.add(b);
    }
    private void box(float x,int station,float y,float floor,HardBoxes.Kind kind){HardBoxes.Box b=new HardBoxes.Box(x,station,y,floor,kind);boxes.add(b);blocks.add(b);}
    private void blinkCells(float x,float top,float delay,int count){for(int i=0;i<count;i++)blinking.add(new BlinkPlatform(x+i*64,top,delay+i*.4f));}
    protected Gap gap(float from,float to,float x,float y,boolean lake){Gap g=new Gap(from,to,x,y,lake);gaps.add(g);return g;}
    protected float[][] finishFloors(){return new float[][]{{15648,15744,184},{15856,15952,184},{16064,WORLD_END,184}};}
    protected float[][] finishHills(){return new float[][]{{15648,96,144},{15856,96,48}};}
    protected void addFinishGaps(){
        gap(15520,15648,15236,376,false);gap(15744,15856,15652,328,false);gap(15952,16064,15860,232,false);
    }
    protected void addFinishPlatforms(){ }
    private static float[][] concat(float[][] a,float[][] b){
        float[][] result=java.util.Arrays.copyOf(a,a.length+b.length);System.arraycopy(b,0,result,a.length,b.length);return result;
    }
    @Override public float groundTop(float x){float top=0;for(Rectangle r:ground)if(r!=ceiling&&x>=r.x&&x<r.x+r.width)top=Math.max(top,r.y+r.height);return top;}
    public boolean isEquipmentRow(Rectangle surface){return surface.y==334&&surface.x>=11232&&surface.x<11936;}
    @Override public float conveyorSpeed(int i){return i==1?-195:195;}
    @Override public float worldWidth(){return WORLD_END;}
    @Override public float stopX(){return RUN_END;}
    @Override public float rockRepeatPeriod(){return 3;}
    @Override public float birdFlightSpeed(RockSpawn spawn){return spawn.x==LAKE_BIRD_X?400:spawn.rolls?BIRD_SPEED*1.5f:spawn.x==15312?BIRD_SPEED*2:BIRD_SPEED;}
    @Override public float rockEntryMargin(){return 320;}
    @Override public float rockRollVelocity(RockSpawn spawn){return spawn.rolls?ROCK_ROLL_SPEED:super.rockRollVelocity(spawn);}
    @Override public Iterable<? extends Rectangle> rockSurfaces(RockSpawn spawn){return spawn.rolls?rockTerrain:ground;}
}
