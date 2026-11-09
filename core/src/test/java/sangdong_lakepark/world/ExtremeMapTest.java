package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExtremeMapTest {
    @Test public void commonCourseAndTenCoinsMatchHardBeforeTheFinish(){
        HardMap hard=new HardMap();ExtremeMap extreme=new ExtremeMap();
        for(float x=0;x<15520;x+=8)assertEquals("Ground "+x,hard.groundTop(x),extreme.groundTop(x),0);
        assertEquals(10,extreme.coins.size);
        for(int i=0;i<10;i++){assertEquals(hard.coins.get(i),extreme.coins.get(i));assertEquals(hard.sectionStarts[i],extreme.sectionStarts[i]);}
        assertEquals(hard.boxes.size,extreme.boxes.size);assertEquals(hard.blocks.size,extreme.blocks.size);
        for(int i=0;i<hard.blocks.size;i++)assertEquals(hard.blocks.get(i),extreme.blocks.get(i));
        assertEquals(hard.traps.toString(),extreme.traps.toString());
        assertEquals(16560,hard.worldWidth(),0);assertEquals(16160,hard.stopX(),0);assertEquals(16320,hard.arch.x,0);
    }
    @Test public void extendedFinishHasTwoRealBlinkRowsAndNonCollidingDecoys(){
        ExtremeMap map=new ExtremeMap();assertEquals(17104,map.worldWidth(),0);assertEquals(16704,map.stopX(),0);
        assertEquals(16864,map.arch.x,0);assertEquals(328,map.groundTop(15696),0);assertEquals(232,map.groundTop(16176),0);
        assertEquals(184,map.groundTop(16608),0);
        for(float x:new float[]{15872,15936,16352,16416}){
            PlatformMap.BlinkPlatform found=null;
            for(PlatformMap.BlinkPlatform b:map.blinking)if(b.x==x)found=b;
            assertNotNull(found);assertEquals(x<16000?280:328,found.y+found.height,0);
            assertTrue(found.solid(1.5));assertFalse(found.solid(4.1));
            assertEquals(0,map.groundTop(x),0);
        }
        for(PlatformMap.Block b:map.fakePlatforms){assertFalse(map.blocks.contains(b,true));assertFalse(map.ground.contains(b,true));}
        float[][] checkpoints={{15521,15236,376},{15900,15652,328},{16400,16132,232}};
        for(float[] p:checkpoints){PlatformMap.Gap gap=map.gapAt(p[0]);assertEquals(p[1],gap.respawn.x,0);assertEquals(p[2],gap.respawn.y,0);}
    }
}
