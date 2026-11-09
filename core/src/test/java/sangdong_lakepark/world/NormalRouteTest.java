package sangdong_lakepark.world;

import org.junit.Test;
import static org.junit.Assert.*;

public class NormalRouteTest {
    @Test public void completeCourseWithScooterCollectsAllTenCoins(){play(false);}
    @Test public void completeCourseAfterPitDismountAndRemountCollectsAllTenCoins(){play(true);}
    private void play(boolean pitRecovery){
        NormalMap map=new NormalMap();NormalRun run=new NormalRun(map);
        NormalCourseRoute.play(map,run,pitRecovery,new NormalCourseRoute.Driver(){
            public void frame(float direction,boolean jump){run.update(1f/120f,direction,jump);}
            public void visit(String name){System.out.println(name+" "+run.player+" score="+run.getScore()+" lives="+run.getLives()+" ride="+run.isRiding());}
        });
        assertTrue(run.isCleared());assertEquals(10,run.getScore());assertTrue(run.isRiding());
        assertEquals(pitRecovery?2:1,run.getBicycleMountCount());
        assertEquals(pitRecovery?1:0,run.getFallCount());
    }
}
