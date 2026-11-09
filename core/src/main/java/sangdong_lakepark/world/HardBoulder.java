package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;

/** Chases along the raised straight, then shatters against the first tall stair wall. */
public final class HardBoulder {
    public static final float TRIGGER=13152, START=12576, WALL=13568;
    public final Rectangle bounds=new Rectangle(START,312,128,128);
    private boolean active,spent;
    private float rotation,shake,breakAge=-1;
    private int launches,breaks;
    public void reset(){active=spent=false;rotation=shake=0;breakAge=-1;launches=breaks=0;bounds.setPosition(START,312);}
    public void resetAfter(float x){int count=launches,broken=breaks;reset();launches=count;breaks=broken;spent=x+44>TRIGGER;}
    public void update(float dt,HardRun run,HardMap map){
        shake=Math.max(0,shake-dt);
        if(breakAge>=0){breakAge+=dt;if(breakAge>=.4f)active=false;return;}
        if(!active&&!spent&&run.player.x+44>=TRIGGER){active=true;launches++;shake=.5f;bounds.setPosition(START,312);}
        if(!active)return;
        bounds.x+=400*dt;rotation-=400f/64*dt;bounds.y=312;
        if(bounds.x+bounds.width>=WALL){bounds.x=WALL-bounds.width;spent=true;breakAge=0;breaks++;return;}
        if(bounds.overlaps(run.player))run.caughtByBoulder();
    }
    public boolean visible(){return active;}
    public boolean breaking(){return breakAge>=0;}
    public float breakAge(){return breakAge;}
    public float rotation(){return rotation;}
    public float shake(){return shake;}
    public int getLaunchCount(){return launches;}
    public int getBreakCount(){return breaks;}
}
