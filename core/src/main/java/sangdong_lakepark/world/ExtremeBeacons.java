package sangdong_lakepark.world;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** Harmless blue rays lock the feet at warning time and grant light once per shot. */
public final class ExtremeBeacons {
    public static final float WARNING=.8f, FIRE=.3f, PERIOD=3;
    public static final class Beacon {
        public final Vector2 origin,target=new Vector2();
        public final float baseY,lightSeconds;
        private final Rectangle expanded=new Rectangle();
        private float age;
        private boolean enabled,hit;
        private Beacon(float x,float baseY,float lightSeconds){
            origin=new Vector2(x,527);this.baseY=baseY;this.lightSeconds=lightSeconds;
        }
        private void aim(ExtremeRun run){target.set(run.player.x+44,run.player.y);hit=false;}
        public boolean enabled(){return enabled;}
        public boolean warning(){return enabled&&age<WARNING;}
        public boolean firing(){return enabled&&age>=WARNING&&age<WARNING+FIRE;}
        public float age(){return age;}
        public boolean intersects(Rectangle body){
            float dx=target.x-origin.x,dy=target.y-origin.y,length=(float)Math.sqrt(dx*dx+dy*dy);
            if(length<.001f){dx=0;dy=-1;length=1;}
            expanded.set(body.x-12,body.y-12,body.width+24,body.height+24);
            return Intersector.intersectSegmentRectangle(origin.x,origin.y,
                origin.x+dx/length*18000,origin.y+dy/length*18000,expanded);
        }
    }
    public final Beacon[] all={new Beacon(4000,0,5),new Beacon(7504,184,4),new Beacon(10160,0,4),
        new Beacon(12640,312,4),new Beacon(14992,184,3)};
    private boolean lakeTriggered,lakePassed;
    private int warnings,shots,hits;
    public void reset(){warnings=shots=hits=0;rearm();}
    public void rearm(){
        lakeTriggered=lakePassed=false;
        for(Beacon b:all){b.enabled=b.hit=false;b.age=0;b.target.setZero();}
    }
    public void update(float dt,ExtremeRun run){
        float center=run.player.x+44,left=run.cameraLeft();
        if(!lakePassed&&run.isGrounded()&&Math.abs(run.player.y-248)<1&&center>=6240&&center<=6304)lakeTriggered=true;
        if(lakeTriggered&&center>7504)lakePassed=true;
        for(int i=0;i<all.length;i++){
            Beacon b=all[i];
            boolean visible=b.origin.x>=left-40&&b.origin.x<=left+PlatformMap.VIEW_WIDTH+40;
            if(!visible&&!(i==1&&lakeTriggered&&!lakePassed)){b.enabled=false;b.age=0;continue;}
            if(!b.enabled){b.enabled=true;b.age=0;b.aim(run);warnings++;}
            boolean wasFiring=b.firing();b.age+=dt;
            if(b.age>=PERIOD){b.age%=PERIOD;b.aim(run);warnings++;}
            if(!wasFiring&&b.firing())shots++;
            if(b.firing()&&!b.hit&&b.intersects(run.player)){
                b.hit=true;hits++;run.illuminate(b.lightSeconds);
            }
        }
    }
    public int getWarningCount(){return warnings;}
    public int getShotCount(){return shots;}
    public int getHitCount(){return hits;}
}
