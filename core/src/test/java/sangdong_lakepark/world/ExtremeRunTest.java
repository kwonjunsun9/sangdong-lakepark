package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExtremeRunTest {
    private static final float DT=1f/120;
    private ExtremeMap map;
    private ExtremeRun run;
    private void init(float x,float y){
        map=new ExtremeMap();map.traps.clear();map.rocks.clear();map.waterStreams.clear();map.stingBirds=new Vector2[0];
        run=new ExtremeRun(map);run.jumpToSection(9);run.player.setPosition(x,y);run.grounded=true;
    }
    private void advance(float seconds){for(int i=0;i<Math.round(seconds/DT);i++)run.update(DT,0,false);}
    @Test public void introStaysBrightUntilGoEndsThenConvergesOverOneSecond(){
        run=new ExtremeRun(new ExtremeMap());run.arriving=false;run.player.setPosition(260,184);
        assertTrue(run.isDark());assertEquals(0,run.darknessAlpha(),0);
        advance(.5f);assertEquals(0,run.getIntroSpot(),0);assertEquals(0,run.darknessAlpha(),0);
        run.finishIntro();advance(.5f);
        assertEquals(.5f,run.getIntroSpot(),.001f);assertEquals(380,run.darkRadius(),.1f);
        assertEquals(.805f,run.darknessAlpha(),.001f);
        advance(.5f);assertEquals(220,run.darkRadius(),.01f);assertEquals(.92f,run.darknessAlpha(),.001f);
    }
    @Test public void blueLightFadesOutDarknessRefreshesAndWarnsBeforeReturning(){
        init(260,184);run.illuminate(3);
        assertFalse(run.isDark());assertEquals(.35f,run.getLightFlash(),.001f);
        advance(.15f);assertEquals(.46f,run.darknessAlpha(),.01f);
        advance(.15f);assertEquals(0,run.darknessAlpha(),.001f);assertEquals(0,run.getLightFlash(),0);
        advance(1);run.illuminate(4);assertEquals(4,run.getLightTime(),0);assertEquals(0,run.darknessAlpha(),.001f);
        advance(3);boolean on=false,off=false;
        for(int i=0;i<120;i++){on|=run.lightWarning();off|=!run.lightWarning();run.update(DT,0,false);}
        assertTrue(on&&off);advance(DT);assertTrue(run.isDark());assertTrue(run.darknessAlpha()<.04f);
        advance(.25f);assertEquals(.46f,run.darknessAlpha(),.04f);
        advance(.25f);assertEquals(.92f,run.darknessAlpha(),.001f);
    }
    @Test public void fallsAndEveryDebugSectionReturnToDarknessWithoutIntro(){
        init(15652,328);run.illuminate(5);run.fall(map.gapAt(15900));
        assertEquals(15652,run.player.x,0);assertEquals(328,run.player.y,0);assertEquals(4,run.getLives());
        assertEquals(0,run.getLightTime(),0);assertEquals(.92f,run.darknessAlpha(),0);
        for(int i=0;i<10;i++){
            run.illuminate(4);run.jumpToSection(i);
            assertEquals(map.sectionStarts[i],new Vector2(run.player.x,run.player.y));
            assertEquals(0,run.getLightTime(),0);assertEquals(1,run.getIntroSpot(),0);
        }
    }
    @Test public void everyRetryClearsTheLightAndReplaysTheBrightToDarkIntro(){
        init(260,184);
        for(int attempt=0;attempt<3;attempt++){
            run.illuminate(5);run.reset();
            assertTrue(run.isArriving());assertEquals(0,run.getLightTime(),0);
            assertEquals(0,run.getIntroSpot(),0);assertEquals(0,run.darknessAlpha(),0);
            run.arriving=false;run.player.setPosition(260,184);run.grounded=true;
            advance(.5f);assertEquals(0,run.getIntroSpot(),0);assertEquals(0,run.darknessAlpha(),0);
            run.finishIntro();advance(.5f);
            assertEquals(380,run.darkRadius(),.1f);assertEquals(.805f,run.darknessAlpha(),.001f);
            advance(.5f);assertEquals(220,run.darkRadius(),.01f);assertEquals(.92f,run.darknessAlpha(),.001f);
        }
    }
    @Test public void blueShotIsCountedAtFireTimeOncePerCycleAndResetOnRetry(){
        init(3896,408);advance(.75f);
        assertEquals(1,run.beacons.getWarningCount());assertEquals(0,run.beacons.getShotCount());
        advance(.1f);assertEquals(1,run.beacons.getShotCount());
        advance(.2f);assertEquals(1,run.beacons.getShotCount());
        advance(2.9f);assertEquals(2,run.beacons.getShotCount());
        run.reset();assertEquals(0,run.beacons.getShotCount());
    }
    @Test public void allBeaconsHitAStationaryPlayerWithinThreePointSevenSecondsAtEveryPhase(){
        float[] centers={3940,6272,10256,12900,15376},tops={408,248,216,312,376};
        for(int index=0;index<5;index++)for(int phase=0;phase<30;phase++){
            init(centers[index]-44,tops[index]);
            run.beacons.update(DT,run); // Activate, including the offscreen lake exception.
            run.player.y+=300;run.beacons.update(phase*.1f,run);run.player.y=tops[index];
            int before=run.beacons.getHitCount();float time=0;
            while(time<3.7f&&run.beacons.getHitCount()==before){run.beacons.update(DT,run);time+=DT;}
            assertTrue("Beacon "+index+" phase "+phase,time<3.7f);
            assertEquals(run.beacons.all[index].lightSeconds,run.getLightTime(),0);
        }
    }
    @Test public void warningLocksFeetAndRayContinuesThroughTerrainWithoutDamage(){
        init(3896,408);ExtremeBeacons.Beacon b=run.beacons.all[0];run.beacons.update(DT,run);
        Vector2 target=new Vector2(b.target);run.player.x+=200;run.beacons.update(.4f,run);
        assertEquals(target,b.target);assertTrue(b.warning());
        assertTrue(b.intersects(new Rectangle(target.x-12,target.y-12,24,24)));
        Vector2 beyond=new Vector2(target).sub(b.origin).scl(2).add(b.origin);
        assertTrue(b.intersects(new Rectangle(beyond.x-12,beyond.y-12,24,24)));
        run.player.setPosition(3896,408);int lives=run.getLives();
        run.beacons.update(.4f,run);int hits=run.beacons.getHitCount();
        run.beacons.update(.1f,run);assertEquals(hits,run.beacons.getHitCount());assertEquals(lives,run.getLives());
        assertEquals(0,run.getDamageCount());assertEquals(0,run.getStunTime(),0);
    }
    @Test public void visibleBeaconsRepeatAndRearmAfterLeavingTheCamera(){
        init(3896,408);run.beacons.update(DT,run);assertTrue(run.beacons.all[0].warning());
        for(int i=0;i<720;i++)run.beacons.update(DT,run);
        assertEquals(3,run.beacons.getWarningCount());
        run.player.x=100;run.beacons.update(DT,run);assertFalse(run.beacons.all[0].enabled());assertEquals(0,run.beacons.all[0].age(),0);
        run.player.x=3896;run.beacons.update(DT,run);assertTrue(run.beacons.all[0].warning());
    }
    @Test public void lakeExceptionActivatesOffscreenAndRedLaserNeverRuns(){
        init(6228,248);assertTrue(run.cameraLeft()+1280+40<7504);
        run.beacons.update(DT,run);assertTrue(run.beacons.all[1].enabled());
        advance(.9f);assertTrue(run.getLightTime()>0);assertFalse(run.laser.enabled());assertEquals(0,run.laser.getShotCount());
        run.player.x=8100;run.beacons.update(DT,run);assertTrue(run.beacons.all[1].enabled()); // Still in camera.
        run.player.x=8300;run.beacons.update(DT,run);assertFalse(run.beacons.all[1].enabled());
    }
    @Test public void additionalThunderRepeatsAndStunsForOneSecondOnSafeGround(){
        init(15652,328);int lives=run.getLives();
        advance(1.05f);assertTrue(run.getStunTime()>.8f);assertEquals(lives,run.getLives());
        assertEquals(328,run.player.y,.01f);assertEquals(1,run.thunder.getStrikeCount());
        advance(3);assertEquals(2,run.thunder.getStrikeCount());assertEquals(0,run.getFallCount());
    }
}
