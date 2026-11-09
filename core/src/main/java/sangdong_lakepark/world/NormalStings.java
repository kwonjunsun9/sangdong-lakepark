package sangdong_lakepark.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/** Darts keep their firing direction and pass through terrain; only the 16px tip can poison the player. */
public final class NormalStings {
    public static final class Bird {
        public final Vector2 position;
        public float clock;
        private float readyAt;
        private boolean inRange;
        Bird(Vector2 position) { this.position=new Vector2(position); }
        public boolean warning() { return clock>=NormalMap.STING_PERIOD-NormalMap.STING_WARNING; }
    }
    public static final class Sting {
        public final Vector2 tip=new Vector2(),velocity=new Vector2();
        public final Rectangle hitbox=new Rectangle(0,0,16,16);
        public float age;
        Sting(float x,float y,float dx,float dy) {tip.set(x,y);velocity.set(dx,dy).nor().scl(NormalMap.STING_SPEED);}
    }
    public final Array<Bird> birds=new Array<>();
    public final Array<Sting> projectiles=new Array<>();
    private final float period;
    private int shots;
    private float time,nextShot=NormalMap.STING_PERIOD;
    private int nextBird,warningBird=-1;
    private boolean engaged;
    public NormalStings(NormalMap map) { this(map.stingBirds,NormalMap.STING_PERIOD); }
    public NormalStings(Vector2[] positions,float period) {this.period=period;for(Vector2 position:positions)birds.add(new Bird(position));}
    public void reset() {clearProjectiles();shots=0;}
    public void clearProjectiles() {projectiles.clear();resetSchedule();}
    private void resetSchedule() {
        time=0;nextShot=period;nextBird=0;warningBird=-1;engaged=false;
        for(Bird bird:birds){bird.clock=bird.readyAt=0;bird.inRange=false;}
    }
    public void update(float dt,StageRun run) {
        if(dt<=0)return;
        float center=run.player.x+run.player.width/2;
        int first=-1;
        time+=dt;
        for(int i=0;i<birds.size;i++) {
            Bird bird=birds.get(i);bird.clock=0;
            boolean inRange=Math.abs(center-bird.position.x)<=NormalMap.STING_RANGE;
            if(inRange&&!bird.inRange)bird.readyAt=Math.max(bird.readyAt,time+NormalMap.STING_WARNING);
            bird.inRange=inRange;
            if(inRange&&first<0)first=i;
        }
        if(first<0)resetSchedule();
        else {
            if(!engaged){nextBird=first;nextShot=time+period-dt;engaged=true;}
            int index=nextBird<birds.size&&birds.get(nextBird).inRange?nextBird:first;
            Bird bird=birds.get(index);
            if(index!=warningBird){bird.readyAt=Math.max(bird.readyAt,time+NormalMap.STING_WARNING);warningBird=index;}
            // One shared turn clock prevents simultaneous shots after range changes.
            // A lone bird waits the full period; two birds alternate every half-period.
            float due=Math.max(nextShot,bird.readyAt);
            bird.clock=Math.max(0,NormalMap.STING_PERIOD-(due-time));
            if(time>=due) {
                float y=bird.position.y-40,dy=Math.min(-1,run.player.y+run.player.height/2-y);
                float dx=MathUtils.clamp(center-bird.position.x,-Math.abs(dy),Math.abs(dy));
                projectiles.add(new Sting(bird.position.x,y,dx,dy));shots++;
                bird.clock=0;bird.readyAt=time+period;
                nextShot=time+period/2;nextBird=(index+1)%birds.size;warningBird=-1;
            }
        }
        for(int i=projectiles.size-1;i>=0;i--) {
            Sting sting=projectiles.get(i);sting.age+=dt;
            sting.tip.mulAdd(sting.velocity,dt);
            sting.hitbox.setPosition(sting.tip.x-8,sting.tip.y-8);
            if(sting.age>=NormalMap.STING_LIFE){projectiles.removeIndex(i);continue;}
            if(sting.hitbox.overlaps(run.player)){run.poison();projectiles.removeIndex(i);}
        }
    }
    public int getShotCount() {return shots;}
}
