package sangdong_lakepark.world;

import org.junit.Test;
import static org.junit.Assert.*;
import sangdong_lakepark.world.HardBoxes.Vehicle;

/** Drive the actual120Hz engine, rather than a separate ballistic approximation. */
public class HardTraversalTest {
    static final float DT=1f/120;
    private HardMap map;
    private HardRun run;
    private boolean dodgeDog;
    private void init(float x,float y,Vehicle v,boolean hazards){
        map=new HardMap();if(!hazards){map.traps.clear();map.rocks.clear();map.waterStreams.clear();map.stingBirds=new com.badlogic.gdx.math.Vector2[0];}
        run=new HardRun(map);run.player.setPosition(x,y);run.arriving=false;run.grounded=true;
        if(v!=Vehicle.NONE){for(HardBoxes.Box b:run.boxes.all)if(b.vehicle==v){run.boxes.open(b);run.boxes.update(1);run.boxes.collect(b.pickup);run.bicycle=true;break;}}
    }
    private void frame(float d,boolean j){
        if(dodgeDog&&run.isGrounded()&&run.dog.dangerous()&&Math.abs(run.dog.bounds.x+36-run.player.x-44)<180&&run.player.y<250)j=true;
        run.update(DT,d,j);
    }
    private void waitFor(int n){for(int i=0;i<n;i++)frame(0,false);}
    private void go(float x,float y,boolean jump){
        if(jump)frame(Math.signum(x-run.player.x),true);
        for(int i=0;i<900;i++){
            float dir=Math.abs(x-run.player.x)<2?0:Math.signum(x-run.player.x);
            float distance=x-run.player.x,velocity=run.getHorizontalVelocity();
            if(run.isGrounded()&&map.onStrip(map.puddles,run.player)&&Math.signum(velocity)==Math.signum(distance)
                &&Math.abs(distance)<velocity*velocity/3000+2)dir=0;
            frame(dir,run.isGrounded()&&!Float.isNaN(y)&&run.player.y<y-1);
            if(Math.abs(x-run.player.x)<4&&run.isGrounded()&&(Float.isNaN(y)||Math.abs(run.player.y-y)<1))return;
            assertFalse("Failed toward "+x+" at "+run.player,run.isFailed());
        }
        fail("Unreachable "+x+","+y+" actual="+run.player+" lives="+run.getLives());
    }
    private void waitBlinkWindow(){
        for(int i=0;i<500;i++){double phase=run.getElapsed()%4;if(phase>=1&&phase<1.02)return;frame(0,false);}
        fail("No blinking window");
    }
    private void doubleGo(float x,float y,int secondJumpFrame){
        frame(Math.signum(x-run.player.x),true);
        for(int i=0;i<500;i++){
            float direction=Math.abs(x-run.player.x)<2?0:Math.signum(x-run.player.x);
            frame(direction,i==secondJumpFrame);
            if(Math.abs(x-run.player.x)<4&&run.isGrounded()&&Math.abs(run.player.y-y)<1)return;
            if(run.getFallCount()>0||run.getLives()<5)break;
        }
        fail("Double jump to "+x+","+y+" actual="+run.player+" lives="+run.getLives());
    }
    @Test public void eachBoxOpensByAHeadHitAndEquipmentComesOutOnTheCorrectSide(){
        for(int box=0;box<7;box++){
            init(100,184,Vehicle.NONE,false);HardBoxes.Box b=run.boxes.all.get(box);
            run.player.setPosition(b.x-12,b.kind==HardBoxes.Kind.PURPLE?b.homeY:184);
            frame(0,true);for(int i=0;i<100&&!b.opened;i++)frame(0,false);
            assertTrue("Head opens box"+box,b.opened);assertTrue(b.moving());assertFalse(run.isRiding());
            waitFor(100);assertEquals(b.homeY,b.pickup.y,0);
            if(b.kind==HardBoxes.Kind.BROWN){
                assertFalse("Walking below a brown box cannot collect it",run.isRiding());
                run.player.setPosition(b.x,b.homeY);frame(0,false);
            }
            assertEquals("Pickup at box"+box,b.vehicle,run.vehicle());
        }
    }
    @Test public void startPlatformsAndWideLandingAreJumpableOnFoot(){
        init(260,184,Vehicle.NONE,false);go(374,184,false);go(600,248,true);go(708,248,false);
        go(868,184,true);go(964,232,true);go(1060,280,true);go(1156,328,true);
        go(1172,328,false);go(1304,184,false);go(1412,184,false);go(1644,184,true);
        assertEquals(0,run.getFallCount());assertTrue(map.onStrip(map.mud,run.player));
        assertTrue(run.isCollected(0));assertTrue(run.isCollected(1));
        assertEquals(5,run.getLives());
    }
    @Test public void bicycleCrossesTheLongStationRopeAndAllThreePits(){
        init(3554,24,Vehicle.BICYCLE,false);doubleGo(3656,184,40);go(3604,Float.NaN,false);
        frame(-1,true);for(int i=0;i<7;i++)frame(-1,false);waitFor(35);frame(0,true);
        for(int i=0;i<50&&run.player.y<410;i++)frame(0,false);
        assertTrue(run.player.y>=408);go(3712,408,false);go(3912,408,false);go(4072,408,true);go(4332,408,false);
        assertEquals(Vehicle.BICYCLE,run.vehicle());go(4508,408,false);
        doubleGo(4836,424,36);go(4872,424,false);doubleGo(5188,440,36);go(5224,440,false);
        doubleGo(5600,344,36);go(5614,344,false);
        go(5636,344,false);go(5760,264,false);go(5920,184,false);
        assertEquals(5,run.getLives());assertEquals(0,run.getFallCount());assertEquals(Vehicle.NONE,run.vehicle());assertTrue(run.isCollected(4));
    }
    @Test public void caveCanOnlyBeCrossedByScooter(){
        for(Vehicle v:Vehicle.values()){
            init(8600,184,v,false);int lives=run.getLives();
            for(int i=0;i<500&&run.getLives()==lives&&run.player.x<9390;i++)frame(1,false);
            if(v==Vehicle.SCOOTER){assertTrue("Scooter exits cave",run.player.x>=9390);assertEquals(lives,run.getLives());assertTrue(run.isCollected(5));}
            else assertTrue(v+" must fail cave",run.getLives()<lives||run.player.x<9344);
        }
    }
    @Test public void onlyMotorcycleCrosses608pxPitFromTheOverhangingBoxRow(){
        for(Vehicle v:Vehicle.values()){
            init(11896,398,v,false);frame(1,true);
            for(int i=0;i<30;i++)frame(1,false);
            frame(1,true);
            for(int i=0;i<180&&run.getFallCount()==0&&run.player.x<12610;i++)frame(1,false);
            if(v==Vehicle.MOTORCYCLE){assertEquals(0,run.getFallCount());assertTrue(run.player.x>=12610);assertTrue("Motorcycle missed coin at "+run.player+" score="+run.getScore(),run.isCollected(8));}
            else assertTrue(v+" must fall",run.getFallCount()>0);
        }
    }
    @Test public void blinkingSteppingStonesAndTheirFakeDecoysAreTraversableOnFoot(){
        init(9572,184,Vehicle.NONE,false);go(9764,232,true);go(9796,232,false);
        waitBlinkWindow();go(9976,216,true);go(10024,216,false);go(10216,216,true);go(10236,216,false);
        waitBlinkWindow();go(10424,296,true);go(10472,296,false);go(10664,248,true);go(10696,248,false);
        go(10884,360,true);assertEquals(5,run.getLives());assertEquals(0,run.getFallCount());
    }
    @Test public void motorcycleEscapesTallChaseStairsAndTrapAfterOneSecondDelay(){
        for(int delay:new int[]{0,60,120}){
            init(13108,312,Vehicle.MOTORCYCLE,true);waitFor(delay);go(13372,312,false);
            doubleGo(13576,448,25);doubleGo(13680,584,40);go(13688,584,false);go(14056,274,true);
            assertEquals("Delay "+delay+" at "+run.player,5,run.getLives());assertEquals(Vehicle.MOTORCYCLE,run.vehicle());
            assertEquals(0,run.getFallCount());assertTrue(run.isCollected(9));assertTrue(run.boulder.getBreakCount()>0);
        }
    }
    @Test public void sevenSharedCoinsCanBeCollectedOnFootByActualJumps(){
        float[][] starts={{620,248},{1412,184},{2064,280},{2636,280},{6312,248},{6612,216},{13352,312}};
        int[] coins={0,1,2,3,6,7,9};
        for(int i=0;i<coins.length;i++){
            init(100,184,Vehicle.NONE,false);waitFor(120);
            run.player.setPosition(starts[i][0],starts[i][1]);run.grounded=true;run.velocityY=0;run.jumps=0;
            frame(1,true);for(int t=0;t<100&&!run.isCollected(coins[i]);t++)frame(1,false);
            assertTrue("On-foot coin"+(coins[i]+1)+" actual="+run.player,run.isCollected(coins[i]));
        }
    }
    private void holdUntilWindStops(float x){
        for(int i=0;i<700;i++){
            if(run.wind.phase()>=2.61f&&run.wind.phase()<2.65f)return;
            frame(run.player.x<x-2?1:run.player.x>x+2?-1:0,false);
        }
        fail("No wind-rest window");
    }
    @Test public void conveyorRouteAndTrapCanBePassedForEveryWindPhase(){
        for(float phase=0;phase<4.6f;phase+=.2f){
            init(2006,280,Vehicle.NONE,false);map.trap(2720,2784);run.wind.update(phase,2752);
            holdUntilWindStops(2006);go(2076,280,false);go(2216,344,true);
            go(2296,344,false);go(2486,280,true);
            holdUntilWindStops(2486);go(2586,280,false);go(2796,184,true);
            assertEquals("Wind phase"+phase+" at "+run.player,5,run.getLives());assertEquals(0,run.getFallCount());
        }
    }
    @Test public void dogCorridorWithLongerMudAndFinalTwoPlatformsReachTheGoalOnFoot(){
        init(14056,274,Vehicle.NONE,false);map.trap(14256,14296);map.trap(14296,14336);
        go(14160,184,false);
        for(int i=0;i<1000;i++){
            if(run.dog.phase==NormalDog.Phase.RETURNING&&run.dog.bounds.x>14500)break;
            frame(0,false);
        }
        go(14372,184,true);dodgeDog=true;go(14880,184,false);go(14936,184,false);
        go(15044,248,true);go(15140,312,true);go(15248,376,true);go(15452,376,false);
        go(15656,328,true);go(15684,Float.NaN,false);go(15864,232,true);go(15896,Float.NaN,false);
        go(16072,184,true);go(16160,184,false);frame(1,false);frame(1,false);
        assertTrue(run.isCleared());assertEquals(5,run.getLives());assertEquals(0,run.getFallCount());
    }
    @Test public void attachedTrapPairIsCrossedByOneOnFootJumpFromTheLowerPath(){
        for(int start:new int[]{14144,14152,14160}){
            init(start,184,Vehicle.NONE,false);run.dog.phase=NormalDog.Phase.HIDDEN;
            map.trap(14256,14296);map.trap(14296,14336);go(14372,184,true);
            assertEquals(1,run.getJumpCount());assertEquals(Vehicle.NONE,run.vehicle());
            assertEquals(5,run.getLives());assertEquals(0,run.getFallCount());
        }
    }
    @Test public void fourLakePlatformsSeparateIceFromOneSecondThunder(){
        init(6060,184,Vehicle.NONE,false);
        go(6252,248,true);go(6296,248,false);frame(0,true);waitFor(100);
        assertTrue(run.isCollected(6));go(6352,248,false);
        waitBlinkWindow();go(6536,216,true);go(6640,216,false);go(6824,248,true);go(6928,248,false);
        waitBlinkWindow();go(7112,312,true);waitFor(100);go(7216,312,false);go(7400,184,true);
        assertEquals(5,run.getLives());assertEquals(0,run.getFallCount());assertTrue(run.isCollected(7));
    }
    @Test public void chaseCannotBeOutrunOnFoot(){
        init(13108,312,Vehicle.NONE,false);int lives=run.getLives();
        for(int i=0;i<1000&&run.getLives()==lives;i++)frame(1,run.isGrounded());
        assertTrue(run.getLives()<lives);assertTrue("Caught/respawned at "+run.player,run.player.x<11872);
    }
    @Test public void theSecondStationRequiresReturningWithTheBicycleToGetTheScooter(){
        init(7700,184,Vehicle.NONE,false);go(7732,184,false);frame(0,true);waitFor(100);
        assertTrue(run.boxes.all.get(2).opened);assertEquals(Vehicle.NONE,run.vehicle());
        go(8308,184,false);frame(0,true);waitFor(100);assertEquals(Vehicle.BICYCLE,run.vehicle());
        go(8010,184,false);doubleGo(7910,376,40);go(7752,448,true);
        assertEquals(Vehicle.SCOOTER,run.vehicle());assertTrue(run.boxes.available(run.boxes.all.get(3)));
        go(7936,376,false);go(8600,184,false);assertEquals(5,run.getLives());assertEquals(0,run.getFallCount());
    }
    @Test public void brownBoxRowCanBeOpenedFromBelowAndReachedFromTheSecondStair(){
        init(10884,360,Vehicle.NONE,false);go(11150,184,false);
        for(int i=4;i<7;i++){HardBoxes.Box b=run.boxes.all.get(i);go(b.x-12,184,false);frame(0,true);waitFor(100);assertTrue(b.opened);assertFalse(run.isRiding());}
        go(11142,184,false);go(11020,296,true);go(11052,296,false);go(11240,398,true);
        assertEquals(5,run.getLives());assertEquals(0,run.getFallCount());assertTrue(run.player.y>=398);
    }
    @Test public void removingTheBicycleStepPreventsReachingTheFixedScooter(){
        for(int second:new int[]{20,40,60}){
            init(8010,184,Vehicle.BICYCLE,false);map.blocks.removeAll(map.bicycleStep,true);
            HardBoxes.Box scooter=run.boxes.all.get(2);run.boxes.open(scooter);run.boxes.update(1);
            frame(-1,true);
            for(int i=0;i<240;i++)frame(run.player.x>7754?-1:0,i==second);
            assertEquals(Vehicle.BICYCLE,run.vehicle());assertTrue(run.boxes.available(scooter));
            assertEquals(5,run.getLives());
        }
    }
    @Test public void everyMotorcyclePositionAndRedRockPhaseHasAWaitAndJumpRoute(){
        for(int motorIndex=0;motorIndex<3;motorIndex++)for(int phase=0;phase<360;phase+=120){
            init(10884,360,Vehicle.NONE,true);
            for(int i=0;i<3;i++)run.boxes.all.get(4+i).vehicle=i==motorIndex?Vehicle.MOTORCYCLE:i==(motorIndex+1)%3?Vehicle.BICYCLE:Vehicle.SCOOTER;
            go(11150,184,false);
            for(int i=4;i<7;i++){HardBoxes.Box b=run.boxes.all.get(i);go(b.x-12,184,false);frame(0,true);waitFor(100);assertTrue(b.opened);}
            go(11142,184,false);go(11020,296,true);go(11052,296,false);go(11240,398,true);waitFor(phase);
            int drops=run.rocks.birds.get(1).drops();
            for(int i=0;i<500&&run.rocks.birds.get(1).drops()==drops;i++)frame(0,false);
            waitFor(100);go(run.boxes.all.get(4+motorIndex).x,398,false);
            assertEquals(Vehicle.MOTORCYCLE,run.vehicle());doubleGo(11896,398,25);
            frame(1,true);for(int i=0;i<30;i++)frame(1,false);frame(1,true);
            for(int i=0;i<200&&run.player.x<12610&&run.getFallCount()==0;i++)frame(1,false);
            assertEquals("Motor index "+motorIndex+" phase "+phase+" at "+run.player,5,run.getLives());
            assertEquals(Vehicle.MOTORCYCLE,run.vehicle());assertEquals(0,run.getFallCount());assertTrue(run.player.x>=12610);
            assertTrue(run.isCollected(8));
        }
    }

}
