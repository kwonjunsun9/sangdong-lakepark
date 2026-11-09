package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/** Surface effects shared by Normal and Hard; coordinates and belt speeds belong to the course. */
public class SurfaceMap extends PlatformMap {
    public final Array<Rectangle> mud = new Array<>(), puddles = new Array<>();
    public final Array<Block> conveyors = new Array<>(), fakePlatforms = new Array<>();
    public Rectangle[] belts = new Rectangle[0];
    public Vector2[] stingBirds = new Vector2[0];
    protected void conveyor(float from,float to,float top) {
        for(float x=from;x<to;x+=BLOCK_SIZE) {
            Block block=new Block(x,top-BLOCK_SIZE,BlockKind.CONVEYOR);blocks.add(block);conveyors.add(block);
        }
    }
    protected void fakeRow(float from,float top){fakeRow(from,top,2);}
    protected void fakeRow(float from,float top,int count){
        for(int i=0;i<count;i++)fakePlatforms.add(new Block(from+i*BLOCK_SIZE,top-BLOCK_SIZE,BlockKind.WOOD));
    }
    protected void strip(Array<Rectangle> list,float from,float to) { list.add(new Rectangle(from,GROUND_Y,to-from,1)); }
    public boolean onStrip(Array<Rectangle> strips,Rectangle player) {
        float center=player.x+player.width/2;
        for(Rectangle strip:strips) {
            if(player.y>strip.y+.5f||player.y<strip.y-28.1f)continue;
            if(center>=strip.x&&center<strip.x+strip.width)return true;
            // The rounded lake edge still supports the feet after their center passes the shore.
            // Keep its slippery surface until support ends; normal ground beyond puddle 2 stops normally.
            float footLeft=center-FOOT_WIDTH/2;
            if(strips==puddles&&groundTop(center)<strip.y
                &&Math.min(footLeft+FOOT_WIDTH,strip.x+strip.width)-Math.max(footLeft,strip.x)>=MIN_FOOT_SUPPORT)return true;
        }
        return false;
    }
    public float conveyorVelocity(Rectangle player) {
        float footLeft=player.x+(player.width-FOOT_WIDTH)/2;
        for(int i=0;i<belts.length;i++) {
            Rectangle belt=belts[i];
            if(Math.abs(player.y-belt.y-belt.height)<1
                &&Math.min(footLeft+FOOT_WIDTH,belt.x+belt.width)-Math.max(footLeft,belt.x)>=MIN_FOOT_SUPPORT)
                return conveyorSpeed(i);
        }
        return 0;
    }
    public float conveyorSpeed(int index) { return index==0?0:index==1?-NormalMap.CONVEYOR_SPEED:NormalMap.CONVEYOR_SPEED; }
}
