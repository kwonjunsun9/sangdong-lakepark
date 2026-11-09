package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Intersector;

/** Each warning locks an aim point; the ray continues through terrain beyond that point. */
public final class HardLaser {
    public final Vector2 origin=new Vector2(7504,527),target=new Vector2();
    private float age;
    private boolean enabled,passed,aimed,hitThisShot;
    private int shots;
    public void reset(){age=0;enabled=passed=aimed=hitThisShot=false;shots=0;}
    public void resetAfter(float x){int count=shots;reset();shots=count;passed=x+44>origin.x;}
    public void update(float dt,HardRun run){
        float center=run.player.x+44;
        if(center>origin.x){enabled=false;passed=true;}
        if(!passed&&!enabled&&run.isGrounded()&&Math.abs(run.player.y-248)<1&&center>=6240&&center<=6304){enabled=true;age=0;aimed=false;}
        if(!enabled)return;
        if(!aimed){target.set(center,run.player.y);aimed=true;hitThisShot=false;}
        boolean before=firing();age+=dt;
        if(!before&&firing())shots++;
        if(age>=4){age-=4;target.set(center,run.player.y);hitThisShot=false;}
        if(firing()&&!hitThisShot&&intersects(run.player)){run.darken();hitThisShot=true;}
    }
    public boolean intersects(Rectangle body){
        Vector2 direction=new Vector2(target).sub(origin).nor();
        Rectangle expanded=new Rectangle(body.x-12,body.y-12,body.width+24,body.height+24);
        return Intersector.intersectSegmentRectangle(origin.x,origin.y,origin.x+direction.x*18000,origin.y+direction.y*18000,expanded);
    }
    public boolean warning(){return enabled&&age<.8f;}
    public boolean firing(){return enabled&&age>=.8f&&age<1.1f;}
    public boolean enabled(){return enabled;}
    public int getShotCount(){return shots;}
}
