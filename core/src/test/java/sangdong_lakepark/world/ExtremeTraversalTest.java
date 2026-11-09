package sangdong_lakepark.world;

import com.badlogic.gdx.math.Vector2;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercise the final route in the production collision engine at 120 Hz. */
public class ExtremeTraversalTest {
    private static final float DT=1f/120;
    private ExtremeMap map;
    private ExtremeRun run;
    private void init(float x,float y){
        map=new ExtremeMap();map.rocks.clear();map.traps.clear();map.waterStreams.clear();map.stingBirds=new Vector2[0];
        run=new ExtremeRun(map);run.jumpToSection(9);run.player.setPosition(x,y);run.grounded=true;
    }
    private void go(float x,float y,boolean jump){
        if(jump)run.update(DT,Math.signum(x-run.player.x),true);
        for(int i=0;i<900;i++){
            run.update(DT,Math.abs(x-run.player.x)<2?0:Math.signum(x-run.player.x),false);
            if(Math.abs(x-run.player.x)<3&&run.isGrounded()&&(Float.isNaN(y)||Math.abs(run.player.y-y)<1))return;
            assertEquals("Fell toward "+x+" at "+run.player,0,run.getFallCount());
        }
        fail("Unreachable "+x+","+y+" at "+run.player);
    }
    private void waitWindow(float blinkX){
        PlatformMap.BlinkPlatform first=null;
        for(PlatformMap.BlinkPlatform b:map.blinking)if(b.x==blinkX)first=b;
        assertNotNull(first);
        for(int i=0;i<1800;i++){
            if(first.phase(run.getElapsed())<.04f&&run.getStunTime()<=0)return;
            run.update(DT,0,false);
        }
        fail("No safe blink window");
    }
    @Test public void onFootRouteUsesBothBlinkRowsAndReachesTheExtendedArch(){
        init(15448,376);go(15472,376,false);go(15652,328,true);go(15696,328,false);
        waitWindow(15872);go(15880,280,true);go(15940,280,false);go(16132,232,true);go(16180,Float.NaN,false);
        waitWindow(16352);go(16352,328,true);go(16420,328,false);go(16612,184,true);
        go(16704,184,false);run.update(DT,1,false);
        assertTrue(run.isCleared());assertEquals(0,run.getFallCount());assertEquals(5,run.getLives());
    }
    @Test public void removingBlinkingRowsMakesBothLongGapsUncrossableOnFoot(){
        for(float[] start:new float[][]{{15696,328},{16180,232}}){
            init(start[0],start[1]);map.blinking.clear();run.update(DT,1,true);
            for(int i=0;i<240&&run.getFallCount()==0;i++)run.update(DT,1,false);
            assertEquals("Gap from "+start[0],1,run.getFallCount());
            assertEquals(start[0]<16000?15652:16132,run.player.x,3);
        }
    }
    @Test public void upperFakePlatformIsPassedThroughAndLowerFakeDoesNotCatchFalls(){
        init(15652,328);for(int i=0;i<120;i++)run.update(DT,0,false);
        run.player.setPosition(15880,440);run.grounded=false;run.velocityY=0;
        for(int i=0;i<90&&!run.isGrounded();i++)run.update(DT,0,false);
        assertEquals(280,run.player.y,.01f);assertEquals(0,run.getFallCount());
        init(16370,210);map.blinking.clear();run.grounded=false;run.velocityY=-100;
        for(int i=0;i<120&&run.getFallCount()==0;i++)run.update(DT,0,false);
        assertEquals(1,run.getFallCount());assertEquals(232,run.player.y,.01f);
    }
}
