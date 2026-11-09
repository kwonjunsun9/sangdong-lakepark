package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;
import static org.junit.Assert.*;
import static sangdong_lakepark.world.NormalRunTest.*;

/** Jump trajectories for the scooter coins, independent of the full-course driver. */
public class NormalReachabilityTest {
    @Test public void exitLedgeJumpCollectsBothPitCoinsOnScooter(){
        NormalMap map=new NormalMap();map.rocks.clear();NormalRun run=quiet(map);
        run.bicycle=true;place(run,4228,274);
        jumpTo(run,4752);
        assertTrue(run.isCollected(3));assertTrue(run.isCollected(4));
        assertTrue(run.isGrounded());assertEquals(184,run.player.y,.01f);assertEquals(0,run.getFallCount());
    }
    @Test public void middleDogStepJumpCollectsRaisedTenthCoinOnScooter(){
        NormalMap map=new NormalMap();map.rocks.clear();NormalRun run=quiet(map);
        run.bicycle=true;place(run,7976,344);
        jumpTo(run,8374);
        assertTrue(run.isCollected(9));assertTrue(run.isRiding());
        assertTrue(run.isGrounded());assertEquals(184,run.player.y,.01f);assertEquals(0,run.getFallCount());
    }
    @Test public void footJumpsCannotCollectScooterCoinsAndCannotReachTheRaisedTenthFromTheMiddleStep(){
        float[][] jumps={{3,4228,274},{4,4376,184},{9,7976,344}};
        for(float[] jump:jumps){
            int index=(int)jump[0];NormalMap map=new NormalMap();map.rocks.clear();NormalRun run=quiet(map);
            place(run,jump[1],jump[2]);Rectangle coin=map.coins.get(index);boolean touched=false;
            for(int frame=0;frame<150;frame++){
                touched|=run.player.overlaps(coin);
                float direction=run.player.x+run.player.width/2<coin.x?1:0;
                run.update(DT,direction,frame==0);
                touched|=run.player.overlaps(coin);
                assertFalse("Touching coin "+(index+1)+" on foot must not collect it",run.isCollected(index));
            }
            if(index==9)assertFalse("The raised tenth coin is beyond this foot jump",touched);
            else assertTrue("Pit coin "+(index+1)+" can be touched but still requires a scooter",touched);
        }
    }
    private void jumpTo(NormalRun run,float x){
        for(int frame=0;frame<240;frame++){
            float direction=Math.abs(x-run.player.x)<3?0:Math.signum(x-run.player.x);
            run.update(DT,direction,frame==0);
            if(frame>10&&run.isGrounded()&&Math.abs(x-run.player.x)<4)return;
        }
        fail("Did not land: "+run.player);
    }
}
