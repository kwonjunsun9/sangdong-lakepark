package sangdong_lakepark.world;

import com.badlogic.gdx.math.MathUtils;

/** Normal's scooter, ground effects, poison and ambushes use the shared Easy collision engine. */
public class SurfaceRun extends StageRun {
    public final NormalDog dog;
    public final NormalStings stings;
    protected final SurfaceMap normal;
    protected float horizontalVelocity,reverseTime;
    protected boolean inMud,inPuddle;
    private int mudEntries,puddleEntries;

    protected SurfaceRun(SurfaceMap map, NormalDog dog, float stingPeriod) {
        super(map); normal=map; this.dog=dog; stings=new NormalStings(map.stingBirds,stingPeriod);
    }
    @Override protected void onReset() {
        horizontalVelocity=reverseTime=0;inMud=inPuddle=false;mudEntries=puddleEntries=0;
        dog.reset();stings.reset();
    }
    @Override protected void beforeStep(float dt) {
        reverseTime=Math.max(0,reverseTime-dt);
        stings.update(dt,this);dog.update(dt,player.x+player.width/2);
        if(dog.dangerous()&&player.overlaps(dog.bounds))hit();
    }
    @Override protected float adjustedDirection(float direction) { return reverseTime>0?-direction:direction; }
    @Override protected float movementVelocity(float dt,float direction) {
        float speed=travelSpeed();
        if(grounded&&normal.onStrip(normal.mud,player))speed*=NormalMap.MUD_FACTOR;
        boolean sliding=onPuddleSurface();
        float desired=direction*speed*(sliding?NormalMap.SLIDE_FACTOR:1);
        if(sliding) {
            float change=NormalMap.SLIDE_ACCELERATION*dt;
            horizontalVelocity+=MathUtils.clamp(desired-horizontalVelocity,-change,change);
        } else horizontalVelocity=desired;
        return horizontalVelocity+(grounded?normal.conveyorVelocity(player):0);
    }
    @Override protected void onHorizontalBlock() {horizontalVelocity=0;}
    @Override protected void afterMovement(float dt) {
        boolean mud=grounded&&normal.onStrip(normal.mud,player);
        boolean puddle=onPuddleSurface();
        if(mud&&!inMud)mudEntries++;
        if(puddle&&!inPuddle)puddleEntries++;
        inMud=mud;inPuddle=puddle;
    }
    protected float travelSpeed() { return isRiding()?NormalMap.SCOOTER_SPEED:NormalMap.MOVE_SPEED; }
    private boolean onPuddleSurface() {
        // Walking down a rounded cap can briefly lose vertical contact by a fraction of a pixel.
        // Preserve the surface's inertia there; jumping and leaving foot support restore air control.
        return (grounded||inPuddle&&velocityY<=0)&&normal.onStrip(normal.puddles,player);
    }
    @Override protected void afterFall(float checkpointX) {
        reverseTime=horizontalVelocity=0;inMud=inPuddle=false;
        stings.clearProjectiles();dog.resetAfter(checkpointX);
    }
    public void poison() {
        reverseTime=NormalMap.REVERSE_TIME;
        velocityY=NormalMap.HIT_JUMP;grounded=false;knockbackTime=NormalMap.HIT_LOCK;
        knockbackDirection=facingLeft?1:-1;
    }
    public boolean isReversed() {return reverseTime>0;}
    public float getReverseTime() {return reverseTime;}
    public float getHorizontalVelocity() {return horizontalVelocity;}
    public boolean isSliding() {return inPuddle&&Math.abs(horizontalVelocity)>20;}
    public int getMudEntryCount() {return mudEntries;}
    public int getPuddleEntryCount() {return puddleEntries;}
}
