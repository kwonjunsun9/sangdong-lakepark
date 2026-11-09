package sangdong_lakepark.world;

import org.junit.Test;
import static org.junit.Assert.*;
import static sangdong_lakepark.world.NormalRunTest.*;

public class NormalHazardsTest {
    @Test public void dogWarnsOnceThenPatrolsBothWaysWithoutHiding(){
        NormalDog dog=new NormalDog();dog.reset();dog.update(DT,NormalMap.DOG_TRIGGER+1);
        assertEquals(NormalDog.Phase.WARNING,dog.phase);
        for(int i=0;i<73;i++)dog.update(DT,7344);
        assertEquals(NormalDog.Phase.RUNNING,dog.phase);float x=dog.bounds.x;
        dog.update(.1f,7344);assertEquals(x-NormalMap.DOG_SPEED*.1f,dog.bounds.x,.01f);
        while(dog.phase==NormalDog.Phase.RUNNING)dog.update(DT,7344);
        assertEquals(NormalDog.Phase.BARKING,dog.phase);assertFalse(dog.dangerous());
        dog.update(.31f,7344);assertEquals(NormalDog.Phase.RETURNING,dog.phase);assertTrue(dog.dangerous());
        int turns=0;NormalDog.Phase old=dog.phase;
        float cycle=2*(NormalMap.DOG_RIGHT-NormalMap.DOG_WALL)/NormalMap.DOG_SPEED+NormalMap.DOG_BARK;
        for(int i=0;i<(int)Math.ceil(cycle*3.5f/DT);i++){
            dog.update(DT,7344);
            assertTrue(dog.bounds.x>=NormalMap.DOG_WALL&&dog.bounds.x<=NormalMap.DOG_RIGHT);
            assertTrue(dog.visible());if(old!=dog.phase)turns++;old=dog.phase;
        }
        assertTrue(turns>=6);assertEquals(1,dog.getLaunchCount());
        dog.resetAfter(4624);assertEquals(NormalDog.Phase.READY,dog.phase);
    }
    @Test public void jumpingAvoidsTheFirstChargeAndStandingStillGetsHit(){
        for(boolean jump:new boolean[]{false,true}){
            NormalRun run=quiet(new NormalMap());place(run,NormalMap.DOG_X-124,184);
            for(int i=0;i<90;i++)run.update(DT,0,jump&&i==50);
            assertEquals(jump?5:4,run.getLives());
        }
    }
    @Test public void dogCollisionDismountsScooterAndLaterPassesCanHurtAgain(){
        NormalRun run=quiet(new NormalMap());run.bicycle=true;place(run,NormalMap.DOG_X-124,184);frames(run,90,0);
        assertEquals(5,run.getLives());assertFalse(run.isRiding());
        frames(run,(int)Math.ceil((2*(NormalMap.DOG_RIGHT-NormalMap.DOG_WALL)/NormalMap.DOG_SPEED+NormalMap.DOG_BARK)/DT),0);
        assertTrue(run.getLives()<5);
    }
    @Test public void poisonReversesForOneSecondWithoutTakingLifeOrScooter(){
        NormalRun run=quiet(new NormalMap());run.bicycle=true;place(run,200,184);run.poison();
        assertEquals(1,run.getReverseTime(),.01f);frames(run,70,1);
        assertTrue(run.player.x<200);assertTrue(run.isReversed());assertEquals(5,run.getLives());assertTrue(run.isRiding());
        run.poison();assertEquals(1,run.getReverseTime(),.01f);frames(run,370,0);assertFalse(run.isReversed());
    }
    @Test public void stationaryBirdsWarnAndKeepFiringOnlyWithinRange(){
        NormalMap map=new NormalMap();map.traps.clear();NormalRun run=new NormalRun(map);place(run,3114,184);
        frames(run,(int)Math.ceil((NormalMap.STING_PERIOD-NormalMap.STING_WARNING/2)/DT),0);
        assertTrue(run.stings.birds.first().warning());assertEquals(0,run.stings.getShotCount());
        frames(run,(int)Math.ceil(NormalMap.STING_WARNING/2/DT)+2,0);assertEquals(1,run.stings.getShotCount());
        frames(run,(int)Math.ceil(NormalMap.STING_PERIOD/2/DT)+1,0);assertEquals(2,run.stings.getShotCount());
        for(NormalStings.Sting sting:run.stings.projectiles){assertTrue(sting.velocity.y<0);assertTrue(Math.abs(sting.velocity.x)<=-sting.velocity.y+.01f);}
        frames(run,500,0);assertTrue(run.stings.getShotCount()>=4);assertTrue(run.isReversed());assertEquals(5,run.getLives());
        place(run,1928,184);int shots=run.stings.getShotCount();frames(run,400,0);assertEquals(shots,run.stings.getShotCount());
    }
    @Test public void projectilesExpireWhenTheirLifetimeEnds(){
        NormalRun run=new NormalRun(new NormalMap());place(run,3776,494);frames(run,365,0);
        assertTrue(run.stings.getShotCount()>0);
        place(run,1928,184);frames(run,350,0);assertEquals(0,run.stings.projectiles.size);
    }
    @Test public void poisonCanStillKnockThePlayerOntoAnActualNearbyTrap(){
        NormalRun run=quiet(new NormalMap());place(run,3114,184);run.poison();frames(run,120,0);
        assertTrue(run.getDamageCount()>0);assertTrue(run.getLives()<5);
    }
    @Test public void frozenUpdatesDoNotAdvanceDogProjectilesOrPoison(){
        NormalRun run=new NormalRun(new NormalMap());place(run,8152,184);run.update(DT,0,false);run.poison();
        float reverse=run.getReverseTime();NormalDog.Phase phase=run.dog.phase;
        for(int i=0;i<500;i++)run.update(0,1,true);
        assertEquals(reverse,run.getReverseTime(),0);assertEquals(phase,run.dog.phase);
    }
    @Test public void bothWaterStreamsShareAClockAndBlinkingTilesRetainTheirStagger(){
        NormalMap map=new NormalMap();
        for(float t=0;t<8;t+=.05f)assertEquals(map.waterStreams.first().active(t),map.waterStreams.peek().active(t));
        for(int i=1;i<3;i++)assertEquals(map.blinking.first().solid(3.5),map.blinking.get(i).solid(3.5));
        assertFalse(map.blinking.get(3).solid(3.5));assertTrue(map.blinking.get(4).solid(3.5));
        assertEquals(5376,map.rocks.first().x,0);assertTrue(map.rocks.first().rolls);
        assertFalse(map.rocks.peek().rolls);
    }
}
