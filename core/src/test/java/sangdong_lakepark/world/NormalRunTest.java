package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;
import static org.junit.Assert.*;

public class NormalRunTest {
    @Test public void fourthFifthAndTenthCoinsRequireAnActiveScooterEvenOnDirectContact(){
        for(int index:new int[]{3,4,9}){
            NormalMap map=new NormalMap();NormalRun run=quiet(map);Rectangle coin=map.coins.get(index);
            place(run,coin.x+coin.width/2-run.player.width/2,coin.y);
            run.update(DT,0,false);
            assertFalse("Foot contact must leave coin "+(index+1),run.isCollected(index));assertEquals(0,run.getScore());
            run.bicycle=true;run.update(DT,0,false);
            assertTrue(run.isCollected(index));assertEquals(1,run.getScore());
            run.bicycle=false;run.update(DT,0,false);
            assertTrue("Earned coins survive dismount",run.isCollected(index));assertEquals(1,run.getScore());
        }
        NormalMap map=new NormalMap();NormalRun foot=quiet(map);Rectangle ordinary=map.coins.first();
        place(foot,ordinary.x,ordinary.y);foot.update(DT,0,false);
        assertTrue("Ordinary coins remain collectible on foot",foot.isCollected(0));
    }
    static final float DT=1f/120f;
    static void frames(NormalRun run,int count,float direction){for(int i=0;i<count;i++)run.update(DT,direction,false);}
    static void place(NormalRun run,float x,float y){
        run.player.setPosition(x,y);run.velocityY=0;run.knockbackTime=0;run.invulnerability=0;
        run.arriving=false;run.grounded=true;
    }
    static NormalRun quiet(NormalMap map){
        NormalRun run=new NormalRun(map);run.stings.birds.clear();return run;
    }
    @Test public void initialFallLocksMovementAndLandsWithFiveLives(){
        NormalRun run=new NormalRun(new NormalMap());frames(run,80,1);
        assertTrue(run.isArriving());assertEquals(260,run.player.x,.01f);frames(run,100,0);
        assertFalse(run.isArriving());assertTrue(run.isGrounded());assertEquals(260,run.player.x,.01f);
        assertEquals(184,run.player.y,.01f);assertEquals(5,run.getLives());
    }
    @Test public void scooterDoublesSpeedAndStillAllowsOnlyOneJump(){
        NormalRun run=quiet(new NormalMap());place(run,400,184);frames(run,12,1);
        assertEquals(26,run.player.x-400,.02f);
        run.bicycle=true;place(run,400,184);frames(run,12,1);assertEquals(52,run.player.x-400,.02f);
        place(run,400,184);run.update(DT,0,true);frames(run,25,0);float velocity=run.velocityY;
        run.update(DT,0,true);assertTrue(run.velocityY<velocity);assertEquals(1,run.getJumpCount());
        float peak=run.player.y;
        for(int i=0;i<100;i++){run.update(DT,0,false);peak=Math.max(peak,run.player.y);}
        assertTrue(peak-184>120&&peak-184<125);assertTrue(run.isGrounded());
    }
    @Test public void directThreeHundredTwentyPixelJumpNeedsScooterSpeed(){
        for(boolean riding:new boolean[]{false,true}) {
            NormalRun run=quiet(new NormalMap());run.bicycle=riding;place(run,4332,184);
            run.update(DT,1,true);
            for(int frame=0;frame<200&&run.getFallCount()==0;frame++) {
                run.update(DT,1,false);
                if(run.player.x>=4736&&run.isGrounded())break;
            }
            if(riding){assertEquals(0,run.getFallCount());assertTrue(run.player.x>=4736);assertTrue(run.isRiding());}
            else{assertEquals(1,run.getFallCount());assertEquals(4332,run.player.x,.1f);}
        }
    }
    static void hitItemFromLowHill(NormalRun run){
        place(run,3616,280);frames(run,60,0);
        for(int i=0;i<90&&!run.isBicycleAvailable();i++)run.update(DT,1,i==0);
    }
    @Test public void itemSpawnsOnceAndActualCollisionPicksUpScooter(){
        NormalRun run=quiet(new NormalMap());hitItemFromLowHill(run);
        assertTrue(run.isBicycleAvailable());assertEquals(1,run.getPickupCount());
        hitItemFromLowHill(run);assertEquals(1,run.getPickupCount());
        place(run,3712,494);run.update(DT,0,false);
        assertTrue(run.isRiding());assertFalse(run.isBicycleAvailable());assertEquals(1,run.getBicycleMountCount());
    }
    @Test public void ordinaryPitAndLakeBothRemoveScooterAndClearPoison(){
        NormalRun run=quiet(new NormalMap());run.bicycle=true;place(run,4554,-105);run.poison();
        run.player.y=-105;run.update(DT,0,false);
        assertEquals(4,run.getLives());assertFalse(run.isRiding());assertFalse(run.isItemUsed());assertFalse(run.isReversed());
        assertEquals(4332,run.player.x,.01f);assertEquals(184,run.player.y,.01f);
        run.bicycle=true;
        place(run,5718,110);run.update(DT,0,false);
        assertEquals(3,run.getLives());assertFalse(run.isRiding());assertEquals(4960,run.player.x,.01f);
        assertEquals(1,run.getWaterEntryCount());
    }
    @Test public void trapDismountsBeforeTakingLivesAndItemCanRespawn(){
        NormalRun run=quiet(new NormalMap());run.bicycle=true;place(run,3014,184);run.update(DT,0,false);
        assertFalse(run.isRiding());assertEquals(5,run.getLives());assertTrue(run.isInvulnerable());
        assertTrue(run.getBorderFeedback()>0);place(run,3014,184);run.update(DT,0,false);
        assertEquals(4,run.getLives());
        hitItemFromLowHill(run);assertTrue(run.isBicycleAvailable());
    }
    @Test public void mudHalvesGroundSpeedButAirControlAndJumpHeightStayNormal(){
        NormalRun run=quiet(new NormalMap());place(run,3136,184);frames(run,12,1);
        assertEquals(13,run.player.x-3136,.03f);
        run.bicycle=true;place(run,3136,184);frames(run,12,1);assertEquals(26,run.player.x-3136,.03f);
        place(run,3136,184);run.update(DT,1,true);float x=run.player.x;frames(run,12,1);
        assertEquals(52,run.player.x-x,.04f);assertTrue(run.player.y>184);
    }
    @Test public void finalPitCanBeJumpedOnFootOrScooterAndFallingDismountsAtTheSafeCheckpoint(){
        for(boolean riding:new boolean[]{false,true}){
            NormalMap map=new NormalMap();map.rocks.clear();NormalRun run=quiet(map);
            run.bicycle=riding;place(run,9668,184);run.update(DT,1,true);
            for(int i=0;i<240&&run.getFallCount()==0&&!run.isCleared();i++){
                run.update(DT,run.player.x<9952?1:0,false);
                if(run.player.x>9920&&run.isGrounded())break;
            }
            assertEquals("Jump across final pit",0,run.getFallCount());
            assertTrue(run.player.x>9920);assertTrue(run.isGrounded());
            assertFalse("Landing must precede the clear trigger",run.isCleared());
        }
        NormalRun run=quiet(new NormalMap());run.bicycle=true;place(run,9780,-105);run.update(DT,0,false);
        assertEquals(1,run.getFallCount());assertEquals(4,run.getLives());assertFalse(run.isRiding());
        assertEquals(9592,run.player.x,.01f);assertEquals(184,run.player.y,.01f);assertFalse(run.isCleared());
    }
    @Test public void puddlesTripleSpeedAndKeepInertiaWhenTheKeyIsReleased(){
        for(boolean scooter:new boolean[]{false,true}){
            NormalMap map=new NormalMap();map.ground.clear();map.floor(0,7680);map.puddles.clear();
            map.puddles.add(new Rectangle(100,184,7400,1));map.mud.clear();map.traps.clear();map.waterStreams.clear();
            NormalRun run=quiet(map);run.bicycle=scooter;place(run,200,184);frames(run,150,1);
            assertEquals(scooter?1560:780,run.getHorizontalVelocity(),.1f);
            float x=run.player.x,v=run.getHorizontalVelocity();frames(run,12,0);
            assertEquals(v-150,run.getHorizontalVelocity(),.1f);assertTrue(run.player.x>x+50);
            frames(run,160,0);assertEquals(0,run.getHorizontalVelocity(),.01f);
        }
        NormalRun run=quiet(new NormalMap());place(run,6528,184);frames(run,1,1);
        float x=run.player.x;frames(run,30,0);assertEquals(x,run.player.x,.01f);
    }
    @Test public void fastScooterSlidesOffPuddleIntoLakeWhenInputIsReleased(){
        NormalRun run=quiet(new NormalMap());run.bicycle=true;place(run,5488,184);frames(run,50,1);
        frames(run,180,0);
        assertTrue(run.getFallCount()>0);assertFalse(run.isRiding());assertEquals(4,run.getLives());
    }
    @Test public void threeConveyorsPushRightLeftAndRight(){
        NormalMap map=new NormalMap();
        for(int belt=0;belt<3;belt++){
            NormalRun run=quiet(map);float x=map.belts[belt].x+64,y=map.belts[belt].y+64,push=map.conveyorSpeed(belt);
            assertEquals(belt==1?-195:195,push,0);
            place(run,x,y);frames(run,12,0);assertEquals(x+push*.1f,run.player.x,.04f);
            place(run,x,y);frames(run,12,1);assertEquals(x+26+push*.1f,run.player.x,.04f);
            run.bicycle=true;place(run,x,y);frames(run,12,1);assertEquals(x+52+push*.1f,run.player.x,.04f);
        }
    }
    @Test public void lowerBranchHitsTheWallAndCannotJumpThroughBridgeUndersides(){
        NormalRun run=quiet(new NormalMap());place(run,6628,296);frames(run,240,1);
        assertTrue("Can step off stump and enter the lower road",run.player.x>6848);
        assertEquals(184,run.player.y,.01f);frames(run,1700,1);
        assertEquals(7808-88,run.player.x,.02f);assertEquals(5,run.getLives());
        place(run,6858,184);run.update(DT,0,true);float peak=run.player.y;
        for(int i=0;i<80;i++){run.update(DT,0,false);peak=Math.max(peak,run.player.y);}
        assertTrue("Bridge underside prevents climbing from below",peak<225);
        frames(run,700,-1);assertTrue(run.player.x<6728);
    }
    @Test public void respawnsDoNotIntersectSolidGeometryAndAllGapsAreActuallyEmpty(){
        NormalMap map=new NormalMap();
        for(PlatformMap.Gap gap:map.gaps){
            Rectangle body=new Rectangle(gap.respawn.x,gap.respawn.y,88,120);
            for(Rectangle ground:map.ground)assertFalse("Safe checkpoint "+gap.respawn,body.overlaps(ground));
            for(Rectangle block:map.blocks)assertFalse(body.overlaps(block));
            assertEquals(0,map.groundTop(gap.x+gap.width/2),0);
        }
        assertEquals(10,map.coins.size);
    }
    @Test public void clearStopsTimerAndThenWalksThroughArchWithoutFurtherDamage(){
        NormalRun run=quiet(new NormalMap());place(run,NormalMap.RUN_END,184);run.update(DT,0,false);
        assertTrue(run.isCleared());double time=run.getElapsed();frames(run,200,0);
        assertEquals(time,run.getElapsed(),0);assertTrue(run.isFinished());assertEquals(5,run.getLives());
        assertEquals("Bad",run.getGrade());
    }
}
