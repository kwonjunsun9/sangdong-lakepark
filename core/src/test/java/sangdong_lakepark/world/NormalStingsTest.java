package sangdong_lakepark.world;

import org.junit.Test;
import static org.junit.Assert.*;
import static sangdong_lakepark.world.NormalRunTest.*;

public class NormalStingsTest {
    private int source(NormalRun run){
        float x=run.stings.projectiles.peek().tip.x;
        return Math.abs(x-run.stings.birds.first().position.x)<Math.abs(x-run.stings.birds.peek().position.x)?0:1;
    }
    private void center(NormalRun run,float x){run.player.setPosition(x-44,184);}
    private int ticks(float seconds){return (int)Math.ceil(seconds/DT);}

    @Test public void bothBirdsAlternateEveryHalfPeriodWithOnlyOneWarningAtATime(){
        NormalMap map=new NormalMap();NormalRun run=new NormalRun(map);
        center(run,(map.stingBirds[0].x+map.stingBirds[1].x)/2);
        int count=0;float previous=0;
        for(int frame=1;frame<=1300;frame++){
            run.stings.update(DT,run);
            assertFalse(run.stings.birds.first().warning()&&run.stings.birds.peek().warning());
            int shots=run.stings.getShotCount();
            if(shots==count)continue;
            assertEquals(count+1,shots);assertEquals(count%2,source(run));
            float now=frame*DT;
            assertEquals(count==0?NormalMap.STING_PERIOD:NormalMap.STING_PERIOD/2,now-previous,DT*2);
            previous=now;count=shots;
        }
        assertTrue(count>=7);
    }

    @Test public void loneBirdRetainsItsFullPeriodAndRangeChangesNeverCreateABurst(){
        NormalMap map=new NormalMap();NormalRun run=new NormalRun(map);
        int count=0;float last=-100;float[] ownLast={-100,-100};
        for(int frame=1;frame<=1800;frame++){
            if(frame<600)center(run,map.stingBirds[0].x-NormalMap.STING_RANGE+20);
            else if(frame<1000)center(run,(map.stingBirds[0].x+map.stingBirds[1].x)/2);
            else if(frame<1400)center(run,map.stingBirds[1].x+NormalMap.STING_RANGE-20);
            else center(run,(map.stingBirds[0].x+map.stingBirds[1].x)/2);
            run.stings.update(DT,run);
            int shots=run.stings.getShotCount();if(shots==count)continue;
            assertEquals(count+1,shots);int bird=source(run);float now=frame*DT;
            if(frame<600)assertEquals(0,bird);
            assertTrue("No simultaneous/catch-up shots",now-last>=NormalMap.STING_PERIOD/2-DT);
            assertTrue("Each bird retains its full cooldown",now-ownLast[bird]>=NormalMap.STING_PERIOD-DT);
            ownLast[bird]=last=now;count=shots;
        }
        assertTrue(count>=7);
    }

    @Test public void leavingRangeAndClearingAfterAFallRestartTheWarningAndFirstTurn(){
        NormalMap map=new NormalMap();NormalRun run=new NormalRun(map);
        float middle=(map.stingBirds[0].x+map.stingBirds[1].x)/2;
        center(run,middle);for(int i=0;i<ticks(NormalMap.STING_PERIOD+.1f);i++)run.stings.update(DT,run);
        assertEquals(1,run.stings.getShotCount());
        center(run,200);for(int i=0;i<400;i++)run.stings.update(DT,run);
        assertFalse(run.stings.birds.first().warning());assertFalse(run.stings.birds.peek().warning());
        assertEquals(0,run.stings.projectiles.size);
        center(run,middle);for(int i=0;i<ticks(NormalMap.STING_PERIOD-NormalMap.STING_WARNING/2);i++)run.stings.update(DT,run);
        assertEquals(1,run.stings.getShotCount());assertTrue(run.stings.birds.first().warning());
        run.stings.clearProjectiles();
        for(int i=0;i<ticks(NormalMap.STING_PERIOD-NormalMap.STING_WARNING/2);i++)run.stings.update(DT,run);
        assertEquals(1,run.stings.getShotCount());
        for(int i=0;i<ticks(NormalMap.STING_WARNING/2)+3;i++)run.stings.update(DT,run);
        assertEquals(2,run.stings.getShotCount());assertEquals(0,source(run));
        run.reset();assertEquals(0,run.stings.getShotCount());assertEquals(0,run.stings.projectiles.size);
    }
    @Test public void switchingThePendingBirdAlwaysGivesANewHalfSecondWarning(){
        NormalMap map=new NormalMap();NormalRun run=new NormalRun(map);
        center(run,(map.stingBirds[0].x+map.stingBirds[1].x)/2);
        for(int i=0;i<ticks(NormalMap.STING_PERIOD-.025f);i++)run.stings.update(DT,run);
        assertTrue(run.stings.birds.first().warning());
        center(run,map.stingBirds[1].x+NormalMap.STING_RANGE-20);
        for(int i=0;i<30;i++)run.stings.update(DT,run);
        assertEquals(0,run.stings.getShotCount());
        assertFalse(run.stings.birds.first().warning());assertTrue(run.stings.birds.peek().warning());
        for(int i=0;i<35;i++)run.stings.update(DT,run);
        assertEquals(1,run.stings.getShotCount());assertEquals(1,source(run));
    }
}
