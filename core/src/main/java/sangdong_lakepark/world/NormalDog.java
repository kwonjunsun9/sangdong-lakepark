package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;

/** Warn once on emergence, then patrol continuously between both ends of the corridor. */
public final class NormalDog {
    public enum Phase { READY, WARNING, RUNNING, BARKING, RETURNING, HIDDEN }
    public final Rectangle bounds=new Rectangle(NormalMap.DOG_X-36,PlatformMap.GROUND_Y,72,40);
    public Phase phase=Phase.READY;
    public final float home, trigger, left, right, speed;
    public NormalDog() { this(NormalMap.DOG_X,NormalMap.DOG_TRIGGER,NormalMap.DOG_WALL,NormalMap.DOG_RIGHT,NormalMap.DOG_SPEED); }
    public NormalDog(float home,float trigger,float left,float right,float speed) {
        this.home=home;this.trigger=trigger;this.left=left;this.right=right;this.speed=speed;reset();
    }
    private float age;
    private int launches;
    public void reset() { phase=Phase.READY;age=0;launches=0;bounds.setPosition(home-36,PlatformMap.GROUND_Y); }
    public void resetAfter(float checkpoint) {
        if(checkpoint<home) { int count=launches;reset();launches=count; }
    }
    public void update(float dt,float playerCenter) {
        switch(phase) {
            case READY: if(playerCenter>trigger){phase=Phase.WARNING;age=0;} break;
            case WARNING:
                age+=dt;if(age>=NormalMap.DOG_WARNING){phase=Phase.RUNNING;launches++;age=0;}break;
            case RUNNING:
                bounds.x-=speed*dt;
                if(bounds.x<=left){bounds.x=left;phase=Phase.BARKING;age=0;}break;
            case BARKING: age+=dt;if(age>=NormalMap.DOG_BARK){phase=Phase.RETURNING;age=0;}break;
            case RETURNING:
                bounds.x+=speed*dt;
                if(bounds.x>=right){bounds.x=right;phase=Phase.RUNNING;}break;
            default: break;
        }
    }
    public boolean dangerous() { return phase==Phase.RUNNING||phase==Phase.RETURNING; }
    public boolean visible() { return phase==Phase.RUNNING||phase==Phase.BARKING||phase==Phase.RETURNING; }
    public int getLaunchCount() { return launches; }
    public float warningAge() { return age; }
}
