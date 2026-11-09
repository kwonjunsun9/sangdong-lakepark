package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;
import sangdong_lakepark.world.HardBoxes.Vehicle;

public class HardRunTest {
    private static final float DT=1f/120;
    private HardRun run=new HardRun(new HardMap());
    private void place(float x,float y){run.player.setPosition(x,y);run.arriving=false;run.grounded=true;run.velocityY=0;run.jumps=0;}
    private void frames(int n,float direction){for(int i=0;i<n;i++)run.update(DT,direction,false);}
    private void mount(Vehicle v){
        HardBoxes.Box box=null;for(HardBoxes.Box b:run.boxes.all)if(b.vehicle==v){box=b;break;}
        assertNotNull(box);run.boxes.open(box);run.boxes.update(1);run.boxes.collect(box.pickup);run.bicycle=true;
    }
    @Test public void stationOneAndThreeShuffleWhileStationTwoKeepsItsFixedVehicles(){
        Set<String> layouts=new HashSet<>();
        for(int attempt=0;attempt<20;attempt++){
            StringBuilder layout=new StringBuilder();
            for(int station=0;station<3;station++){
                Set<Vehicle> vehicles=new HashSet<>();
                for(HardBoxes.Box box:run.boxes.all)if(box.station==station){
                    vehicles.add(box.vehicle);layout.append(box.vehicle);
                    if(station==1)assertEquals(box.kind==HardBoxes.Kind.BROWN?Vehicle.SCOOTER:Vehicle.BICYCLE,box.vehicle);
                }
                assertEquals(station==2?3:2,vehicles.size());assertFalse(vehicles.contains(Vehicle.NONE));
                if(station==0)assertFalse(vehicles.contains(Vehicle.MOTORCYCLE));
            }
            layouts.add(layout.toString());frames(10,0);run.reset();
        }
        assertTrue(layouts.size()>1);
    }
    @Test public void swappingReturnsOldEquipmentAndOpeningNeverRerolls(){
        HardBoxes.Box first=run.boxes.all.first(),second=run.boxes.all.get(1);Vehicle original=first.vehicle;
        run.boxes.open(first);run.boxes.open(second);run.boxes.update(1);run.boxes.collect(first.pickup);
        assertFalse(run.boxes.available(first));run.boxes.collect(second.pickup);
        assertTrue(run.boxes.available(first));assertFalse(run.boxes.available(second));
        assertEquals(original,first.vehicle);run.boxes.open(first);assertEquals(2,run.boxes.getOpeningCount());
    }
    @Test public void stationsCanBeOpenedWhileIteratingTheirBoxes(){
        for(HardBoxes.Box b:run.boxes.all)run.boxes.open(b);
        assertEquals(7,run.boxes.getOpeningCount());run.boxes.update(1);
        for(HardBoxes.Box b:run.boxes.all)run.boxes.collect(b.pickup);
        assertEquals(7,run.boxes.getMountCount());
    }
    @Test public void damageDismountsWithoutLosingLife(){
        mount(Vehicle.MOTORCYCLE);place(100,184);run.hit();assertEquals(5,run.getLives());assertEquals(Vehicle.NONE,run.vehicle());
    }
    @Test public void everyMountedBoxReturnsToItsFloorAndClosesItsStationAfterAnyFall(){
        for(int index=0;index<7;index++)for(float x:new float[]{4680,8870,12220,6456}){
            run.reset();HardBoxes.Box source=run.boxes.all.get(index);
            for(HardBoxes.Box box:run.boxes.all)run.boxes.open(box);
            run.boxes.update(1);run.boxes.collect(source.pickup);run.bicycle=true;
            Vehicle vehicle=source.vehicle;place(x,x==6456?100:-110);run.update(DT,0,false);
            assertEquals(4,run.getLives());assertEquals(1,run.getFallCount());
            assertEquals(Vehicle.NONE,run.vehicle());assertFalse(run.isRiding());assertTrue(run.isGrounded());
            assertEquals(source.x-12,run.player.x,0);assertEquals(source.floorY,run.player.y,0);
            assertEquals(vehicle,source.vehicle);
            for(HardBoxes.Box box:run.boxes.all){
                assertEquals(box.station!=source.station,box.opened);
                assertFalse(box.moving());assertEquals(box.homeY,box.pickup.y,0);
            }
            frames(5,0);assertEquals("Closed source must not remount automatically",Vehicle.NONE,run.vehicle());
            run.update(DT,0,true);frames(100,0);assertTrue(source.opened);
            if(source.kind==HardBoxes.Kind.BROWN){assertEquals(Vehicle.NONE,run.vehicle());place(source.x-12,source.homeY);frames(1,0);}
            assertEquals("The source can be reopened and collected again",vehicle,run.vehicle());
        }
    }
    @Test public void lastLifeFallAlsoDismountsAndClosesTheSourceStation(){
        mount(Vehicle.MOTORCYCLE);for(int i=0;i<4;i++)run.loseLife();place(12220,-120);frames(1,0);
        assertEquals(0,run.getLives());assertTrue(run.isFailed());assertTrue(run.isFinished());assertFalse(run.isRiding());
        assertEquals(Vehicle.NONE,run.vehicle());for(HardBoxes.Box box:run.boxes.all)if(box.station==2)assertFalse(box.opened);
    }
    @Test public void equipmentControlsSpeedAndJumpCount(){
        for(Vehicle v:Vehicle.values()){
            run.reset();if(v!=Vehicle.NONE)mount(v);place(100,184);frames(12,1);
            assertEquals(v==Vehicle.MOTORCYCLE||v==Vehicle.SCOOTER?52:26,run.player.x-100,.1f);
            run.update(DT,0,true);frames(10,0);float before=run.velocityY;run.update(DT,0,true);
            if(v==Vehicle.BICYCLE||v==Vehicle.MOTORCYCLE)assertTrue(run.velocityY>before);else assertTrue(run.velocityY<before);
        }
    }
    @Test public void bicycleAndScooterAutomaticallyDismountAtTheirOwnExit(){
        mount(Vehicle.BICYCLE);place(5576,344);frames(8,1);assertEquals(Vehicle.NONE,run.vehicle());assertEquals(5,run.getLives());
        run.reset();mount(Vehicle.SCOOTER);place(9420,184);frames(4,1);assertEquals(Vehicle.NONE,run.vehicle());
        run.reset();mount(Vehicle.MOTORCYCLE);place(9420,184);frames(4,1);assertEquals(Vehicle.MOTORCYCLE,run.vehicle());
    }
    @Test public void onlyMotorcycleInstantlyBreaksRopeAndLocksJump(){
        mount(Vehicle.MOTORCYCLE);place(3712,408);frames(1,0);assertTrue(run.ropeFall()>=0);
        float y=run.player.y;run.update(DT,1,true);assertTrue(run.player.y<=y);
        run.reset();mount(Vehicle.BICYCLE);place(3712,408);frames(1,0);assertEquals(-1,run.ropeFall(),0);
    }
    @Test public void caveTimerStartsOnContactAndBridgeRestoresOnRetry(){
        place(8740,184);frames(1,0);assertTrue(run.caveAge()>=0);frames(195,0);assertTrue(run.caveFall()>=0);
        run.reset();assertEquals(-1,run.caveAge(),0);place(8740,184);frames(1,0);assertTrue(run.isGrounded());
    }
    @Test public void caveCeilingPreventsJumpingOverCollapse(){
        place(8750,184);run.update(DT,1,true);float max=run.player.y;
        for(int i=0;i<80;i++){run.update(DT,1,i%8==0);max=Math.max(max,run.player.y);}
        assertTrue("Cave only has16px of headroom: "+max,max<=200.1f);
    }
    @Test public void ceilingSpikesCostOneLifeWithEveryVehicle(){
        for(Vehicle v:Vehicle.values()){
            run.reset();if(v!=Vehicle.NONE)mount(v);place(8642,184);
            run.update(DT,0,true);frames(12,0);
            assertEquals("Ceiling costs a life while riding "+v,4,run.getLives());
            assertEquals(1,run.getDamageCount());assertEquals(v,run.vehicle());
        }
    }
    @Test public void ceilingSpikesRespectTheExistingDamageCooldown(){
        mount(Vehicle.SCOOTER);place(8642,184);
        for(int i=0;i<95;i++)run.update(DT,0,run.isGrounded());
        assertEquals(4,run.getLives());assertEquals(1,run.getDamageCount());
        for(int i=0;i<60;i++)run.update(DT,0,run.isGrounded());
        assertEquals(3,run.getLives());assertEquals(2,run.getDamageCount());
    }
    @Test public void lastLifeLostToCeilingEndsTheAttempt(){
        mount(Vehicle.SCOOTER);for(int i=0;i<4;i++)run.loseLife();run.invulnerability=0;
        place(8642,184);run.update(DT,0,true);frames(12,0);
        assertEquals(0,run.getLives());assertTrue(run.isFailed());assertTrue(run.isFinished());
    }
    @Test public void darknessExpiresAndThunderIgnoresInputWhileGravityContinues(){
        place(100,184);run.darken();frames(239,0);assertTrue(run.isDark());frames(3,0);assertFalse(run.isDark());
        place(600,350);run.stun();float x=run.player.x,y=run.player.y;run.update(DT,1,true);frames(10,1);
        assertEquals(x,run.player.x,.01f);assertTrue(run.player.y<y);assertTrue(run.getStunTime()>.8f);
    }
    @Test public void laserLocksItsWarningAimAndStopsAfterLighthouse(){
        place(6240,248);run.laser.update(DT,run);assertTrue(run.laser.warning());float x=run.laser.target.x;
        run.player.x+=40;run.laser.update(.3f,run);assertEquals(x,run.laser.target.x,0);
        run.laser.update(.51f,run);assertTrue(run.laser.firing());assertEquals(1,run.laser.getShotCount());
        run.player.x=7508;run.laser.update(DT,run);assertFalse(run.laser.enabled());
    }
    @Test public void repeatedLaserShotsLeaveABrightWindowBetweenTwoSecondEffects(){
        place(6240,248);frames(100,0);assertTrue(run.isDark());
        assertTrue(run.getDarkTime()<=2);frames(245,0);assertFalse(run.isDark());
        frames(170,0);assertFalse(run.isDark());frames(70,0);assertTrue(run.isDark());
        assertEquals(2,run.laser.getShotCount());assertEquals(5,run.getLives());
    }
    private float firstDropTime(StageRocks rocks,float center){
        for(int i=0;i<1500;i++){
            rocks.update(DT,center);if(rocks.birds.first().drops()>0)return(i+1)*DT;
        }
        throw new AssertionError("Bird never arrived");
    }
    @Test public void lakeEntranceBirdArrivesAboutTwoSecondsEarlier(){
        HardMap map=new HardMap();float x=map.rocks.first().x;
        PlatformMap previous=new PlatformMap(){
            @Override public float rockRepeatPeriod(){return 3;}
            @Override public float rockEntryMargin(){return 320;}
        };
        previous.rocks.add(new PlatformMap.RockSpawn(x,false));
        float center=x+PlatformMap.BIRD_ENTRY_INSET-PlatformMap.VIEW_WIDTH/2;
        float before=firstDropTime(new StageRocks(previous),center);
        float after=firstDropTime(new StageRocks(map),center);
        assertEquals("Earlier arrival, not a shorter drop cycle",2,before-after,.03f);
    }
    @Test public void fastLakeBirdKeepsItsWarningAndThreeSecondDropCycle(){
        HardMap map=new HardMap();StageRocks rocks=new StageRocks(map);StageRocks.Bird bird=rocks.birds.first();
        float center=bird.spawn.x+PlatformMap.BIRD_ENTRY_INSET-640;int warningFrames=0;
        for(int i=0;i<600&&bird.drops()==0;i++){
            rocks.update(DT,center);if(bird.warning())warningFrames++;
        }
        assertEquals(1,bird.drops());assertTrue(warningFrames*DT>=.78f);
        for(int i=0;i<350;i++)rocks.update(DT,center);assertEquals(1,bird.drops());
        for(int i=0;i<15;i++)rocks.update(DT,center);assertEquals(2,bird.drops());
        assertEquals(210,map.birdFlightSpeed(map.rocks.get(1)),0);
        assertEquals(280,map.birdFlightSpeed(map.rocks.peek()),0);
        NormalMap normal=new NormalMap();assertEquals(280,normal.birdFlightSpeed(normal.rocks.peek()),0);
        ExtremeMap extreme=new ExtremeMap();assertEquals(140,extreme.birdFlightSpeed(extreme.rocks.peek()),0);
        EasyMap easy=new EasyMap();assertEquals(140,easy.birdFlightSpeed(easy.rocks.first()),0);
    }
    @Test public void lakeThunderRepeatsEveryThreeSecondsAndRearmsOnRespawn(){
        place(6908,248);for(int i=0;i<150;i++)run.thunder.update(DT,run);
        assertEquals(1,run.thunder.getStrikeCount());
        for(int i=0;i<360;i++)run.thunder.update(DT,run);assertEquals(2,run.thunder.getStrikeCount());
        for(int i=0;i<360;i++)run.thunder.update(DT,run);assertEquals(3,run.thunder.getStrikeCount());
        run.thunder.rearm();for(int i=0;i<150;i++)run.thunder.update(DT,run);assertEquals(4,run.thunder.getStrikeCount());
        assertEquals(1,run.thunder.clouds.length);
    }
    @Test public void boulderCaptureAlwaysCostsLifeAndReturnsToThirdStation(){
        mount(Vehicle.MOTORCYCLE);place(13108,312);run.caughtByBoulder();
        assertEquals(4,run.getLives());assertEquals(10884,run.player.x,0);assertEquals(Vehicle.NONE,run.vehicle());
    }
    @Test public void motorcyclePitReturnsToTheMotorcycleBoxInsteadOfSkippingToChase(){
        mount(Vehicle.MOTORCYCLE);place(12220,-120);frames(1,0);
        assertTrue(run.player.x>=11348&&run.player.x<=11732);assertEquals(184,run.player.y,0);
        assertEquals(4,run.getLives());assertEquals(Vehicle.NONE,run.vehicle());
    }
    @Test public void mudOnlySlowsGroundContact(){
        place(14348,184);frames(12,1);assertEquals(13,run.player.x-14348,.1f);
        place(14348,185);run.grounded=false;run.velocityY=200;frames(12,1);assertEquals(26,run.player.x-14348,.1f);
    }
    @Test public void landingHasRoomAndGapAndCourseKeepsTenCheckpoints(){
        HardMap map=new HardMap();assertEquals(10,map.sectionStarts.length);assertEquals(16560,map.worldWidth(),0);
        assertEquals(184,map.groundTop(864),0);assertEquals(184,map.groundTop(959),0);assertEquals(232,map.groundTop(960),0);
        assertEquals(576,map.blocks.first().x,0);assertEquals(768,map.blocks.get(2).x+map.blocks.get(2).width,0);
        assertEquals(128,map.blocks.first().x-448,0);assertEquals(96,864-768,0);
        assertEquals(1280,map.puddles.first().x,0);assertEquals(192,map.puddles.first().width,0);
        assertEquals(1632,map.mud.first().x,0);assertEquals(256,map.mud.first().width,0);
        assertEquals(160,map.mud.first().x-map.puddles.first().x-map.puddles.first().width,0);
        for(Rectangle g:map.ground)if(g!=map.ceiling)assertEquals(0,g.y,0);
    }
    @Test public void equipmentCoinsRequireTheMatchingVehicleEvenBeforeASectionFails(){
        int[] coins={4,5,8};Vehicle[] required={Vehicle.BICYCLE,Vehicle.SCOOTER,Vehicle.MOTORCYCLE};
        for(int c=0;c<coins.length;c++)for(Vehicle v:Vehicle.values()){
            run.reset();if(v!=Vehicle.NONE)mount(v);
            HardMap map=new HardMap();Rectangle coin=map.coins.get(coins[c]);place(coin.x,coin.y);run.grounded=false;
            frames(1,0);assertEquals(required[c]==v,run.isCollected(coins[c]));
        }
    }
    @Test public void goalClearsAndWalksThroughArch(){
        place(16154,184);frames(5,1);assertTrue(run.isCleared());frames(200,0);assertTrue(run.isFinished());
    }
}
