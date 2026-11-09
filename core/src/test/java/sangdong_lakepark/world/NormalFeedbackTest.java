package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;
import static org.junit.Assert.*;
import static sangdong_lakepark.world.NormalRunTest.*;

public class NormalFeedbackTest {
    @Test public void evenScooterJumpsMustLandOnTheDogRoadBeforeReachingTheOppositeStairs(){
        for(boolean fromLeft:new boolean[]{true,false}){
            NormalMap map=new NormalMap();map.rocks.clear();boolean roadLanding=false;
            float right=NormalMap.DOG_RIGHT+72;
            for(Rectangle hill:map.ground){
                boolean side=fromLeft?hill.x>=7808&&hill.x+hill.width<=NormalMap.DOG_WALL:hill.x>=right;
                if(!side||hill.y!=PlatformMap.GROUND_Y)continue;
                for(float x=hill.x-48;x<=hill.x+hill.width-30;x+=16){
                    NormalRun run=quiet(map);run.dog.phase=NormalDog.Phase.HIDDEN;run.bicycle=true;
                    place(run,x,hill.y+hill.height); // Full cap height grants extra reach at rounded edges.
                    run.update(DT,fromLeft?1:-1,true);
                    for(int i=0;i<220&&!run.isGrounded();i++){
                        assertTrue("Cannot touch the opposite stairs in one jump",fromLeft?run.player.x+run.player.width<right:run.player.x>NormalMap.DOG_WALL);
                        run.update(DT,fromLeft?1:-1,false);
                    }
                    assertTrue(run.isGrounded());
                    assertTrue("Must land before the opposite stairs",fromLeft?run.player.x+run.player.width<right:run.player.x>NormalMap.DOG_WALL);
                    roadLanding|=run.player.y==PlatformMap.GROUND_Y&&run.player.x>NormalMap.DOG_WALL&&run.player.x<right;
                }
            }
            assertTrue("The dog road must be traversed on the ground",roadLanding);
        }
    }
    @Test public void raisedSquareHillKeepsItsTopAtTheEdgeAndReachesTheFirstItemBlock(){
        NormalMap map=new NormalMap();Rectangle hill=null;
        for(Rectangle ground:map.ground)if(PlatformMap.cornerRadius(ground)==0)hill=ground;
        assertNotNull(hill);
        NormalRun edge=quiet(map);place(edge,hill.x+hill.width-36,hill.y+hill.height);
        frames(edge,60,0);assertEquals(hill.y+hill.height,edge.player.y,0);
        for(float x=hill.x+64;x<=hill.x+112;x+=8){
            NormalRun run=quiet(map);place(run,x,hill.y+hill.height);
            for(int i=0;i<100&&!run.isBicycleAvailable();i++)run.update(DT,1,i==0);
            assertTrue("Head-hit first item from "+x,run.isBicycleAvailable());
            assertEquals(1,run.getPickupCount());
        }
    }
    @Test public void departureRequiresAJumpAndTheFirstLongBeltCanBeLandedOn(){
        for(boolean jump:new boolean[]{false,true}){
            NormalMap map=new NormalMap();NormalRun run=quiet(map);place(run,1676,184);
            boolean landed=false;
            for(int i=0;i<240&&run.getFallCount()==0;i++){
                run.update(DT,1,jump&&i==0);
                if(run.isGrounded()&&run.player.x>=map.belts[0].x-20){landed=true;break;}
            }
            assertEquals(jump,landed);
            if(jump)assertEquals(0,run.getFallCount());else assertTrue(run.getFallCount()>0);
        }
    }
    @Test public void theReturningDogCanAlsoBeAvoidedWithANormalJump(){
        for(boolean jump:new boolean[]{false,true}){
            NormalRun run=quiet(new NormalMap());place(run,8314,184);
            run.dog.phase=NormalDog.Phase.RETURNING;run.dog.bounds.x=NormalMap.DOG_WALL;
            for(int i=0;i<65;i++)run.update(DT,0,jump&&i==0);
            assertEquals(jump?5:4,run.getLives());
        }
    }
    @Test public void dartsPassThroughTheItemRowWithoutFollowingAMovingPlayer(){
        NormalMap map=new NormalMap();NormalRun run=quiet(map);place(run,3776,184);
        run.stings.projectiles.add(new NormalStings.Sting(3808,560,0,-1));
        for(int i=0;i<40;i++)run.stings.update(DT,run);
        assertEquals(1,run.stings.projectiles.size);
        NormalStings.Sting dart=run.stings.projectiles.first();
        assertTrue(dart.tip.y>430&&dart.tip.y<494);
        run.player.x+=120;
        for(int i=0;i<20;i++)run.stings.update(DT,run);
        assertEquals(0,dart.velocity.x,0);assertEquals(-380,dart.velocity.y,.01f);
        assertEquals(3808,dart.tip.x,.01f);
        for(int i=0;i<250;i++)run.stings.update(DT,run);
        assertFalse(run.isReversed());assertEquals(5,run.getLives());
        assertEquals(0,run.stings.projectiles.size);
    }
    @Test public void firedDartsKeepTheirInitialAimEvenWhenThePlayerMovesAboveThem(){
        NormalRun run=new NormalRun(new NormalMap());place(run,3114,184);
        for(int i=0;i<(int)Math.ceil(NormalMap.STING_PERIOD/DT)+1;i++)run.stings.update(DT,run);
        NormalStings.Sting dart=run.stings.projectiles.first();
        float vx=dart.velocity.x,vy=dart.velocity.y,x=dart.tip.x,y=dart.tip.y;
        assertTrue(vy<0);assertTrue(Math.abs(vx)<=-vy);assertEquals(380,dart.velocity.len(),.01f);
        run.player.setPosition(3600,650);
        for(int i=0;i<20;i++)run.stings.update(DT,run);
        assertEquals(vx,dart.velocity.x,0);assertEquals(vy,dart.velocity.y,0);
        assertEquals(x+vx*20*DT,dart.tip.x,.02f);assertEquals(y+vy*20*DT,dart.tip.y,.02f);
        assertFalse(run.isReversed());
    }
    @Test public void secondSectionStartsSafelyOnTheRightMovingFirstBelt(){
        NormalRun run=new NormalRun(new NormalMap());run.jumpToSection(1);
        float x=run.player.x;frames(run,60,0);
        assertTrue(run.isGrounded());assertEquals(x+195*.5f,run.player.x,2);
        assertEquals(5,run.getLives());assertEquals(0,run.getFallCount());
    }
    @Test public void dartsAlsoPassThroughGroundAndExpireWithoutATargetHit(){
        NormalRun run=quiet(new NormalMap());place(run,200,0);
        run.stings.projectiles.add(new NormalStings.Sting(244,220,0,-1));
        for(int i=0;i<60&&!run.isReversed();i++)run.stings.update(DT,run);
        assertTrue(run.isReversed());
        run.stings.projectiles.add(new NormalStings.Sting(6000,700,1,0));
        for(int i=0;i<310;i++)run.stings.update(DT,run);
        assertEquals(0,run.stings.projectiles.size);
    }
    @Test public void normalRockBirdsKeepDroppingIndependentRocksAndStopOffscreen(){
        NormalMap map=new NormalMap();StageRocks rocks=new StageRocks(map);
        for(int i=0;i<1800;i++)rocks.update(DT,5416);
        StageRocks.Bird bird=rocks.birds.first();
        assertTrue(bird.drops()>=3);assertTrue(bird.visible());
        assertEquals(bird.drops(),rocks.getFallingSoundCount());
        assertTrue(rocks.getRollingSoundCount()>=2);assertTrue(rocks.all.size<8);
        int drops=bird.drops();for(int i=0;i<1000;i++)rocks.update(DT,100);
        assertEquals(drops,bird.drops());assertFalse(bird.visible());
        rocks.reset();assertEquals(map.rocks.size,rocks.all.size);assertEquals(0,rocks.getFallingSoundCount());
    }
    @Test public void normalBirdsUseTheirCourseSpeedsAndStillWarnBeforeDropping(){
        NormalMap map=new NormalMap();StageRocks rocks=new StageRocks(map);
        for(int index=0;index<rocks.birds.size;index++){
            StageRocks.Bird bird=rocks.birds.get(index);
            bird.flight=StageRocks.Flight.ENTERING;bird.x=bird.spawn.x+500;
            float before=bird.x;rocks.update(.1f,bird.spawn.x);
            assertEquals(index==0?21:28,before-bird.x,.01f);
        }
        StageRocks.Bird red=rocks.birds.first();
        red.flight=StageRocks.Flight.ENTERING;red.x=red.spawn.x+210*PlatformMap.ROCK_WARNING;
        assertTrue(red.warning());
        assertEquals(140,new EasyMap().birdFlightSpeed(map.rocks.first()),0);
    }
    @Test public void startFitsAllThreeStepsWithoutMudAndTheRemovedBridgeLeavesAnOpenPit(){
        NormalMap map=new NormalMap();int steps=0;
        for(Rectangle ground:map.ground)if(ground.y==184&&ground.x<1280){
            assertTrue("Entire staircase fits the initial 1280px camera",ground.x+ground.width<=1280);
            steps++;
        }
        assertEquals(3,steps);
        assertFalse(map.onStrip(map.mud,new Rectangle(760,184,88,120)));
        assertTrue(map.onStrip(map.mud,new Rectangle(3136,184,88,120)));
        for(Rectangle block:map.blocks)assertFalse("No low bridge in the 320px pit",
            block.x>=4416&&block.x<4736);
        assertEquals(520,map.coins.peek().y+map.coins.peek().height/2,0);
    }
    @Test public void surfacesTrapSpacingAndMiddleBridgeArePresent(){
        NormalMap map=new NormalMap();Rectangle feet=new Rectangle(1550,184,88,120);
        assertTrue(map.onStrip(map.puddles,feet));assertFalse(map.onStrip(map.mud,feet));
        feet.x=4844;assertTrue(map.onStrip(map.mud,feet));
        feet.x=5544;assertTrue(map.onStrip(map.puddles,feet));
        feet.x=6344;assertTrue(map.onStrip(map.mud,feet));assertFalse(map.onStrip(map.puddles,feet));
        assertEquals(224,map.traps.get(1).x-map.traps.first().x-64,.01f);
        assertEquals(3,map.blinking.size-4);
        for(int i=0;i<3;i++){
            assertEquals(7168+i*64,map.blinking.get(i).x,0);
            for(float t=0;t<8;t+=.1f)assertEquals(map.blinking.first().solid(t),map.blinking.get(i).solid(t));
        }
    }
    @Test public void fallingThroughTheFakeRowStillLandsOnTheLowerRoad(){
        NormalMap map=new NormalMap();map.rocks.clear();map.blinking.clear();NormalRun run=quiet(map);
        place(run,7200,400);run.grounded=false;
        boolean passedFake=false;
        for(int i=0;i<180;i++){
            run.update(DT,0,false);
            if(run.player.y<344)passedFake=true;
            assertFalse("The decorative row must not support the feet",run.isGrounded()&&run.player.y==344);
        }
        assertTrue(passedFake);assertTrue(run.isGrounded());assertEquals(184,run.player.y,.01f);
        assertEquals(0,run.getFallCount());assertEquals(5,run.getLives());
    }
    @Test public void footPlayerCanClimbBothWidenedStaircasesFromTheDogCorridor(){
        for(boolean left:new boolean[]{true,false}) {
            NormalMap map=new NormalMap();map.rocks.clear();NormalRun run=quiet(map);
            run.dog.phase=NormalDog.Phase.HIDDEN;place(run,left?NormalMap.DOG_WALL+80:NormalMap.DOG_RIGHT-56,184);
            float[][] targets=left?new float[][]{{8080,248},{7952,344},{7824,440}}
                :new float[][]{{9168,248},{9296,312},{9424,376}};
            for(float[] target:targets) {
                boolean reached=false;
                for(int i=0;i<300;i++) {
                    float dx=target[0]-run.player.x;
                    run.update(DT,Math.abs(dx)>3?Math.signum(dx):0,run.isGrounded()&&run.player.y<target[1]-1);
                    if(Math.abs(dx)<5&&Math.abs(run.player.y-target[1])<1){reached=true;break;}
                }
                assertTrue("Reachable stair "+target[0]+","+target[1]+" from corridor: "+run.player,reached);
            }
            assertEquals(5,run.getLives());assertEquals(0,run.getFallCount());
        }
    }
}
