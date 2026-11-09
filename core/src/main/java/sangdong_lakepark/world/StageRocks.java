package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/** Shared bird flight, hovering drops and independently moving rocks. */
public class StageRocks {
    public enum Phase { READY, CARRIED, FALLING, ROLLING, SHATTERING, GONE }
    public enum Flight { WAITING, ENTERING, HOVERING, LEAVING, GONE }
    public static final class Rock {
        public final PlatformMap.RockSpawn spawn;
        public final Rectangle bounds;
        public Phase phase = Phase.READY;
        private float age, velocityY;
        public float rotation;
        Rock(PlatformMap.RockSpawn spawn) {
            this.spawn = spawn;
            bounds = new Rectangle(spawn.x - PlatformMap.ROCK_SIZE / 2, PlatformMap.ROCK_START_Y,
                PlatformMap.ROCK_SIZE, PlatformMap.ROCK_SIZE);
        }
        private void reset() {
            phase = Phase.READY; age = velocityY = rotation = 0;
            bounds.setPosition(spawn.x - bounds.width / 2, PlatformMap.ROCK_START_Y);
        }
        public void shatter() { phase = Phase.SHATTERING; age = velocityY = 0; }
        public float breakAge() { return age; }
        public boolean dangerous() { return phase == Phase.FALLING || phase == Phase.ROLLING; }
    }
    public static final class Bird {
        public final PlatformMap.RockSpawn spawn;
        private final Rock rock;
        public Flight flight = Flight.WAITING;
        public float x;
        private float flightTime, dropClock;
        private final float repeatPeriod, flightSpeed;
        private int drops;
        Bird(PlatformMap.RockSpawn spawn,float repeatPeriod,float flightSpeed) {
            this.spawn = spawn; this.repeatPeriod=repeatPeriod; this.flightSpeed=flightSpeed; rock = new Rock(spawn);
        }
        private void reset() { rock.reset(); flight = Flight.WAITING; x = flightTime = dropClock = 0; drops = 0; }
        public int drops() { return drops; }
        public int wingFrame() { int frame = (int) (flightTime * 10) % 4; return frame == 3 ? 1 : frame; }
        public boolean visible() { return flight == Flight.ENTERING || flight == Flight.LEAVING || flight == Flight.HOVERING; }
        public boolean warning() {
            return flight == Flight.ENTERING && x - spawn.x <= flightSpeed * PlatformMap.ROCK_WARNING
                || flight == Flight.HOVERING && dropClock >= repeatPeriod-PlatformMap.ROCK_WARNING;
        }
    }
    public final Array<Rock> all = new Array<>();
    public final Array<Bird> birds = new Array<>();
    private final PlatformMap map;
    private int fallingSounds, rollingSounds;
    public StageRocks(PlatformMap map) {
        this.map = map;
        for (PlatformMap.RockSpawn spawn : map.rocks) {
            Bird bird = new Bird(spawn,map.rockRepeatPeriod(),map.birdFlightSpeed(spawn)); birds.add(bird); all.add(bird.rock);
        }
    }
    public void reset() {
        all.clear();
        for (Bird bird : birds) { bird.reset(); all.add(bird.rock); }
        fallingSounds = rollingSounds = 0;
    }
    public void resetAfter(float x) {
        for (Bird bird : birds) if (bird.spawn.x > x) bird.reset();
        for(int i=all.size-1;i>=0;i--) if(all.get(i).spawn.x>x&&!isOriginal(all.get(i)))all.removeIndex(i);
    }
    public void update(float dt, float playerCenter) {
        float cameraLeft = MathUtils.clamp(playerCenter, PlatformMap.VIEW_WIDTH / 2,
            map.worldWidth() - PlatformMap.VIEW_WIDTH / 2) - PlatformMap.VIEW_WIDTH / 2;
        float cameraRight = cameraLeft + PlatformMap.VIEW_WIDTH;
        for (Bird bird : birds) {
            if(bird.repeatPeriod>0) {
                if(bird.spawn.x<cameraLeft-PlatformMap.BIRD_DRAW_SIZE/2||bird.spawn.x>cameraRight+PlatformMap.BIRD_DRAW_SIZE/2) {
                    if(bird.flight!=Flight.WAITING) {
                        bird.flight=Flight.GONE;
                        if(bird.rock.phase==Phase.CARRIED)bird.rock.phase=Phase.GONE;
                    }
                    continue;
                }
                if(bird.flight==Flight.GONE)bird.flight=Flight.WAITING;
            }
            if (bird.flight == Flight.WAITING) {
                if (bird.repeatPeriod==0&&playerCenter > bird.spawn.x) { bird.flight = Flight.GONE; bird.rock.phase = Phase.GONE; continue; }
                if (cameraRight < bird.spawn.x + PlatformMap.BIRD_ENTRY_INSET) continue;
                bird.x = Math.max(cameraRight + map.rockEntryMargin(),
                    bird.spawn.x + bird.flightSpeed * PlatformMap.ROCK_WARNING);
                bird.flight = Flight.ENTERING;
                if(bird.drops==0) {
                    bird.rock.phase = Phase.CARRIED;
                    bird.rock.bounds.setPosition(bird.x - PlatformMap.ROCK_SIZE / 2, PlatformMap.ROCK_START_Y);
                }
                continue;
            }
            if (!bird.visible()) continue;
            bird.flightTime += dt;
            if(bird.flight==Flight.HOVERING) {
                bird.dropClock+=dt;
                if(bird.dropClock>=bird.repeatPeriod) {bird.dropClock-=bird.repeatPeriod;drop(bird,cameraLeft);}
                continue;
            }
            bird.x -= bird.flightSpeed * dt;
            if (bird.flight == Flight.ENTERING) {
                if(bird.drops==0)bird.rock.bounds.x = bird.x - PlatformMap.ROCK_SIZE / 2;
                if (bird.x <= bird.spawn.x) {
                    bird.x=bird.spawn.x;bird.dropClock=0;
                    drop(bird,cameraLeft);
                    bird.flight=bird.repeatPeriod>0?Flight.HOVERING:Flight.LEAVING;
                }
            }
            if (bird.flight == Flight.LEAVING && bird.x + PlatformMap.BIRD_DRAW_SIZE / 2 < cameraLeft)
                bird.flight = Flight.GONE;
        }
        for (Rock rock : all) {
            if (rock.phase == Phase.READY || rock.phase == Phase.CARRIED || rock.phase == Phase.GONE) continue;
            rock.age += dt;
            if (rock.phase == Phase.SHATTERING) {
                if (rock.age >= PlatformMap.ROCK_BREAK_TIME) rock.phase = Phase.GONE;
                continue;
            }
            if (rock.phase == Phase.ROLLING) {
                float velocity=map.rockRollVelocity(rock.spawn);
                rock.bounds.x += velocity * dt;
                if(map.breaksRollingRock(rock.bounds)){rock.shatter();continue;}
                rock.rotation -= velocity / (PlatformMap.ROCK_SIZE / 2) * dt;
                if (rock.age >= PlatformMap.ROCK_ROLL_LIFETIME || rock.bounds.x + rock.bounds.width < cameraLeft
                    || rock.bounds.x > cameraLeft + PlatformMap.VIEW_WIDTH) {
                    rock.phase = Phase.GONE; continue;
                }
            }
            float previous = rock.bounds.y;
            float fallDt = dt * PlatformMap.ROCK_FALL_TIME_SCALE;
            rock.velocityY -= PlatformMap.GRAVITY * fallDt;
            rock.bounds.y += rock.velocityY * fallDt;
            for (Rectangle ground : map.rockSurfaces(rock.spawn)) {
                float top = ground.y + ground.height;
                if (previous >= top - .1f && rock.bounds.y <= top
                    && rock.bounds.x + rock.bounds.width > ground.x && rock.bounds.x < ground.x + ground.width) {
                    rock.bounds.y = top; rock.velocityY = 0;
                    if (!rock.spawn.rolls) rock.shatter();
                    else if (rock.phase != Phase.ROLLING) {
                        rock.phase = Phase.ROLLING; rock.age = 0;
                        if (onScreen(rock, cameraLeft)) rollingSounds++;
                    }
                }
            }
            if (rock.bounds.y < PlatformMap.FALL_Y) rock.phase = Phase.GONE;
        }
        if(map.rockRepeatPeriod()>0)for(int i=all.size-1;i>=0;i--)
            if(all.get(i).phase==Phase.GONE&&!isOriginal(all.get(i)))all.removeIndex(i);
    }
    private void drop(Bird bird,float cameraLeft) {
        Rock rock=bird.drops==0?bird.rock:new Rock(bird.spawn);
        if(bird.drops>0)all.add(rock);
        rock.bounds.setPosition(bird.spawn.x-PlatformMap.ROCK_SIZE/2,PlatformMap.ROCK_START_Y);
        rock.phase=Phase.FALLING;bird.drops++;
        if(onScreen(rock,cameraLeft))fallingSounds++;
    }
    private boolean isOriginal(Rock rock) {for(Bird bird:birds)if(bird.rock==rock)return true;return false;}
    private boolean onScreen(Rock rock, float left) {
        return rock.bounds.x + rock.bounds.width >= left && rock.bounds.x <= left + PlatformMap.VIEW_WIDTH;
    }
    public int getFallingSoundCount() { return fallingSounds; }
    public int getRollingSoundCount() { return rollingSounds; }
}
