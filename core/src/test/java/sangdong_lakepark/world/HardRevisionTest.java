package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;
import static org.junit.Assert.*;
import sangdong_lakepark.world.HardBoxes.Vehicle;

/** Behavior introduced by the ten-section revision, exercised through the real physics engine. */
public class HardRevisionTest {
    private static final float DT=1f/120;
    private HardMap map=new HardMap();
    private HardRun run=new HardRun(map);
    private void place(float x,float y){run.player.setPosition(x,y);run.arriving=false;run.grounded=true;run.jumps=0;run.velocityY=0;}
    private void frames(int count,float direction){for(int i=0;i<count;i++)run.update(DT,direction,false);}
    private void mount(Vehicle vehicle){
        HardBoxes.Box selected=null;
        for(HardBoxes.Box b:run.boxes.all)if(b.vehicle==vehicle)selected=b;
        run.boxes.open(selected);run.boxes.update(1);run.boxes.collect(selected.pickup);run.bicycle=true;
    }
    @Test public void pickupFallsUnderTheBoxAndCannotBeCollectedAboveOrDuringItsFall(){
        for(int i=0;i<7;i++){
            run.reset();HardBoxes.Box b=run.boxes.all.get(i);run.boxes.open(b);
            assertTrue(b.moving());assertTrue(b.pickup.y>=b.homeY);
            assertEquals(Vehicle.NONE,run.boxes.collect(new Rectangle(b.x,b.y+64,88,120)));
            assertEquals(Vehicle.NONE,run.boxes.collect(new Rectangle(b.pickup)));
            for(int frame=0;frame<48;frame++)run.boxes.update(DT);
            assertFalse(b.moving());assertEquals(b.homeY,b.pickup.y,0);
            assertEquals(b.vehicle,run.boxes.collect(new Rectangle(b.pickup)));
            run.boxes.release();assertTrue(run.boxes.available(b));assertEquals(b.homeY,b.pickup.y,0);
        }
    }
    @Test public void deepStationExitNeedsTheAvailableBicycle(){
        for(Vehicle vehicle:new Vehicle[]{Vehicle.NONE,Vehicle.SCOOTER,Vehicle.BICYCLE}){
            map=new HardMap();run=new HardRun(map);if(vehicle!=Vehicle.NONE)mount(vehicle);
            place(3554,24);run.update(DT,1,true);boolean escaped=false;
            for(int i=0;i<180;i++){run.update(DT,1,i==40);if(run.player.x>=3648){escaped=true;break;}}
            assertEquals(vehicle+" at "+run.player,vehicle==Vehicle.BICYCLE,escaped);assertEquals(5,run.getLives());
        }
    }
    @Test public void brownEquipmentPopsUpAndRequiresFeetOnItsTopSurface(){
        for(int i=0;i<run.boxes.all.size;i++){
            HardBoxes.Box box=run.boxes.all.get(i);if(box.kind!=HardBoxes.Kind.BROWN)continue;
            run.boxes.open(box);run.boxes.update(.05f);assertTrue(box.pickup.y>box.homeY);
            run.boxes.update(1);assertEquals(box.y+64,box.pickup.y,0);
            assertEquals(Vehicle.NONE,run.boxes.collect(new Rectangle(box.x,box.y,88,120),true));
            assertEquals(Vehicle.NONE,run.boxes.collect(new Rectangle(box.pickup),false));
            assertEquals(box.vehicle,run.boxes.collect(new Rectangle(box.pickup),true));run.boxes.release();
        }
    }
    @Test public void mountedFallRestartsTheBoxPuzzleAndRestoresTheCollapsedBridges(){
        HardBoxes.Box source=null;for(HardBoxes.Box box:run.boxes.all)if(box.station==0&&box.vehicle==Vehicle.BICYCLE)source=box;
        run.boxes.open(source);run.boxes.update(1);run.boxes.collect(source.pickup);run.bicycle=true;
        place(4342,408);frames(1,0);
        assertTrue(run.ropeWarning()>=0);frames(40,0);assertEquals(-1,run.ropeFall(),0);
        frames(10,0);assertTrue(run.ropeFall()>=0);
        run.fall(map.gapAt(4684));assertEquals(source.x-12,run.player.x,0);assertEquals(24,run.player.y,0);
        assertEquals(Vehicle.NONE,run.vehicle());assertFalse(source.opened);assertEquals(4,run.getLives());
        assertEquals(-1,run.ropeFall(),0);assertTrue(map.blocks.contains(map.ropeBridge.first(),true));
        mount(Vehicle.SCOOTER);place(8740,184);frames(196,0);assertTrue(run.caveFall()>=0);
        run.fall(map.gapAt(8900));assertEquals(-1,run.caveFall(),0);assertTrue(map.blocks.contains(map.caveBridge.first(),true));
        assertEquals(Vehicle.NONE,run.vehicle());assertEquals(3,run.getLives());
        run.reset();mount(Vehicle.BICYCLE);place(4342,408);frames(1,0);run.dismount();run.fall(map.gapAt(4684));
        assertEquals("Losing the bike during the warning must not strand the player",2990,run.player.x,0);
    }
    @Test public void fakePlatformsDoNotCatchFeetOrBlockTheHead(){
        place(9980,120);run.update(DT,0,true);frames(20,0);
        assertTrue("Head passes through fake bottom264",run.player.y>164);
        run.reset();
        place(9980,350);run.grounded=false;frames(45,0);
        assertTrue("Falls through fake top328 onto real top216: "+run.player,run.player.y<300);
        assertFalse(map.blocks.contains(map.fakePlatforms.first(),true));
        run.reset();place(10428,184);run.grounded=false;frames(90,0);
        assertEquals(1,run.getFallCount());assertEquals(10212,run.player.x,0);
    }
    @Test public void theFinalRunHasOnlyTwoSolidStonesAndNoThunderOrBlinking(){
        assertEquals(0,map.groundTop(15582),0);assertEquals(328,map.groundTop(15692),0);
        assertEquals(0,map.groundTop(15792),0);assertEquals(232,map.groundTop(15892),0);
        assertEquals(0,map.groundTop(16002),0);assertEquals(184,map.groundTop(16092),0);
        for(PlatformMap.BlinkPlatform b:map.blinking)assertTrue(b.x<10880);
        assertEquals(1,run.thunder.clouds.length);assertEquals(6912,run.thunder.clouds[0].x,0);
    }
    @Test public void dogTurnsBeforeTheAttachedTrapsOnEveryPatrolLap(){
        Rectangle first=map.traps.get(1),second=map.traps.get(2);
        assertEquals(first.x+first.width+8,second.x-8,0);
        float trapEnd=second.x+second.width+8;int turns=0;
        NormalDog.Phase previous=run.dog.phase;
        for(int i=0;i<4800;i++){
            run.dog.update(DT,14100);
            if(run.dog.visible()){
                assertTrue(run.dog.bounds.x>=trapEnd);
                assertTrue("Even the drawn nose stays beyond the spikes",run.dog.bounds.x-39>=trapEnd);
            }
            if(previous==NormalDog.Phase.RUNNING&&run.dog.phase==NormalDog.Phase.BARKING)turns++;
            previous=run.dog.phase;
        }
        assertTrue("Patrol repeats without crossing the traps",turns>=3);
    }
    @Test public void aBoulderBreaksOnTheTallStairInsteadOfClimbingIt(){
        place(13672,584);
        for(int i=0;i<400;i++)run.boulder.update(DT,run,map);
        assertEquals(1,run.boulder.getLaunchCount());assertEquals(1,run.boulder.getBreakCount());
        assertFalse(run.boulder.visible());assertEquals(13568,run.boulder.bounds.x+128,0);assertEquals(5,run.getLives());
    }
    @Test public void theTallChaseStairCannotBeClimbedWithOneJump(){
        run.boulder.resetAfter(13832);place(13480,312);
        for(int i=0;i<200;i++)run.update(DT,1,run.isGrounded());
        assertTrue(run.player.x<=13480.1f);assertEquals(5,run.getLives());
    }
    @Test public void redRocksRollRightOnTheBoxRowAndFallFromItsOverhang(){
        StageRocks rocks=new StageRocks(map);boolean rolled=false,fell=false;
        java.util.IdentityHashMap<StageRocks.Rock,Float> previous=new java.util.IdentityHashMap<>();
        for(int i=0;i<2000;i++){
            rocks.update(DT,11600);
            for(StageRocks.Rock rock:rocks.all)if(rock.spawn.rolls&&rock.phase==StageRocks.Phase.ROLLING){
                Float before=previous.put(rock,rock.bounds.x);if(before!=null)assertTrue(rock.bounds.x>before);
                if(rock.bounds.y==398)rolled=true;
                if(rock.bounds.x>=11936&&rock.bounds.y<380)fell=true;
            }
        }
        assertTrue(rolled);assertTrue(fell);
        PlatformMap normal=new NormalMap();assertEquals(-480,normal.rockRollVelocity(normal.rocks.peek()),0);
    }
    @Test public void allTenDebugSectionsAreSafeAndTheFourthStartsOnABicycle(){
        for(int i=0;i<10;i++){
            run.jumpToSection(i);assertEquals(map.sectionStarts[i].x,run.player.x,0);
            assertEquals(i==3?Vehicle.BICYCLE:Vehicle.NONE,run.vehicle());
            assertFalse(run.isFailed());assertEquals(5,run.getLives());
        }
        frames(1,0);assertEquals("Debug section10 is already beyond the chase",0,run.boulder.getLaunchCount());
    }
    @Test public void thunderStunIgnoresInputButKeepsTheExistingSlipperyMomentum(){
        place(6288,248);run.horizontalVelocity=390;run.stun();float x=run.player.x;
        run.update(DT,1,true);
        assertTrue(run.player.x>x);assertTrue(run.getHorizontalVelocity()>0);assertEquals(0,run.getJumpCount());
        assertTrue(run.getStunTime()>.9f);assertEquals(5,run.getLives());
    }
    @Test public void joinedWoodenCellsDoNotStealAHeadHitOnTheQuestionBoxEdge(){
        for(int index=4;index<7;index++)for(int offset:new int[]{-42,-12,20}){
            run.reset();HardBoxes.Box box=run.boxes.all.get(index);place(box.x+offset,184);
            run.update(DT,0,true);frames(24,0);
            assertTrue("Box "+index+" offset "+offset,box.opened);
            assertEquals(Vehicle.NONE,run.vehicle());assertEquals(5,run.getLives());
        }
    }
}
