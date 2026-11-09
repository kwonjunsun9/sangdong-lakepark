package sangdong_lakepark.world;

/** End-to-end fixture using only player inputs; both routes acquire a bike before the creek. */
public final class EasyCourseRoute {
    public interface Driver { void frame(float direction, boolean jump); default void visit(String name) {} }
    private final EasyMap map;
    private final EasyRun run;
    private final Driver driver;
    private EasyCourseRoute(EasyMap map, EasyRun run, Driver driver) { this.map = map; this.run = run; this.driver = driver; }
    public static void play(EasyMap map, EasyRun run, boolean keepBike, Driver driver) {
        new EasyCourseRoute(map, run, driver).play(keepBike);
    }
    private void play(boolean keepBike) {
        waitFrames(120);
        go("stump-one", 612, 232, false, false);
        go("trap-takeoff", 712, 184, false, false);
        go("first-trap", 880, 184, false, true);
        go("coin-one", 1048, 272, false, false);
        go("first-gap", 1300, 184, false, true);
        go("stepping-stone", 1440, 284, false, false);
        go("high-wall", 1700, 384, false, true);
        go("forest-bridge", 2016, 280, false, true);
        go("bridge-edge", 2120, 280, false, false);
        go("bridge-exit", 2320, 184, false, true);
        go("item-head", 2810, 184, false, false);
        driver.frame(0, true); waitFrames(60);
        require(run.isBicycleAvailable(), "Item must appear above the block");
        go("hill-approach", 2800, 184, false, false);
        go("pickup-step", 2972, 274, false, true);
        go("pickup-hill", 3032, 382, false, true);
        go("pickup", 2800, 462, false, true);
        require(run.isRiding(), "Pickup failed");
        go("walk-down", 3032, 382, false, false);
        go("creek-takeoff", 3450, 184, false, false);
        go("creek", 3848, 184, true, true);
        require(run.isRiding(), "Bike must survive the creek crossing");
        if (keepBike) go("water-wait", 4008, 184, false, true);
        else {
            go("dismount", 4008, 184, false, false);
            require(!run.isRiding(), "The post-creek trap should remove the bike");
        }
        waitWaterRest();
        go("lake-prep", 4332, 184, false, false);
        waitBlinkCycle();
        go("blink-one", 4464, 248, false, true);
        go("blink-two", 4688, 296, false, true);
        if (keepBike) {
            driver.frame(0,true);waitFrames(23);driver.frame(0,true);waitFrames(13);
            require(run.isCollected(5),"Lake coin must be reached by the double jump");driver.visit("exclusive-coin");
        }
        go("rest-post", 4912, 360, false, !keepBike);
        go("blink-three", 5136, 280, false, true);
        go("shore", 5344, 184, false, true);
        go("rock-one-wait", 5508, 232, false, false); waitFirstImpact(0);
        go("rolling-trigger", 5650, 232, false, false);
        go("hill-two", 5776, 280, false, false);
        go("rolling-wait", 5508, 232, false, false);waitRollingExit();
        go("hill-two-return", 5776, 280, false, false);
        go("hill-three", 6032, 328, false, false);
        go("stone-coin", 6180, 328, false, false);
        go("hill-edge", 6184, 328, false, false);
        go("hill-gap", 6384, 184, keepBike, true);
        go("stair-approach", 6532, 184, false, false);waitFirstImpact(2);
        go("stair-two", 6636, 280, false, false);
        go("stair-three", 6732, 328, false, false);
        go("stair-top", 6868, 376, false, false);
        go("last-gap", 7184, 184, keepBike, true);
        go("stop", 7300, 184, false, false);
    }
    private void waitRollingExit() {
        boolean dodged=false;
        for(int i=0;i<600 && run.rocks.all.get(1).phase!=EasyRocks.Phase.GONE;i++) {
            EasyRocks.Rock rock=run.rocks.all.get(1);
            if(!dodged && rock.phase==EasyRocks.Phase.ROLLING && rock.bounds.x-run.player.x-run.player.width<160) {
                driver.frame(1,true);for(int frame=0;frame<40;frame++)driver.frame(1,false);dodged=true;
            } else driver.frame(0,false);
        }
        require(run.rocks.all.get(1).phase==EasyRocks.Phase.GONE,"Rolling rock did not leave");
    }
    private void waitFirstImpact(int index) {
        for(int i=0;i<180;i++) {
            EasyRocks.Phase phase=run.rocks.all.get(index).phase;
            if(phase==EasyRocks.Phase.SHATTERING || phase==EasyRocks.Phase.GONE)return;
            driver.frame(0,false);
        }
        throw new AssertionError("Rock did not impact");
    }
    private void waitWaterRest() {
        for (int i = 0; i < 300; i++) {
            float p = map.water.phase(run.getElapsed()); if (p >= 2.2f && p < 2.24f) return;
            driver.frame(0, false);
        }
        throw new AssertionError("No water rest window");
    }
    private void waitBlinkCycle() {
        for (int i = 0; i < 300; i++) {
            float p = map.blinking.first().phase(run.getElapsed()); if (p >= .05f && p < .09f) return;
            driver.frame(0, false);
        }
        throw new AssertionError("No blink departure window");
    }
    private void waitFrames(int n) { for (int i = 0; i < n; i++) driver.frame(0, false); }
    private void go(String name, float x, float y, boolean doubleJump, boolean jumpNow) {
        int sinceJump = 1000;
        if (jumpNow) { driver.frame(Math.signum(x - run.player.x), true); sinceJump = 0; }
        for (int frame = 0; frame < 900 && !run.isCleared(); frame++) {
            float direction = Math.abs(x - run.player.x) <= 3 ? 0 : Math.signum(x - run.player.x);
            boolean jump = run.isGrounded() && y > run.player.y + 1;
            // Repeating rocks can reach the next step while the rider is already airborne.
            if(direction>0 && (run.isGrounded() || run.isRiding() && sinceJump>=18)) for(EasyRocks.Rock rock:run.rocks.all) {
                float distance=rock.bounds.x-run.player.x-run.player.width;
                if(rock.phase==EasyRocks.Phase.ROLLING && distance>0 && distance<150)jump=true;
            }
            if (!run.isGrounded() && sinceJump == 23 && doubleJump) jump = true;
            if (jump) sinceJump = 0; else sinceJump++;
            driver.frame(direction, jump);
            require(!run.isFailed(), "Died at " + name + "; position=" + run.player);
            if (Math.abs(x - run.player.x) <= 5 && Math.abs(y - run.player.y) < .1f) { driver.visit(name); return; }
        }
        require(run.isCleared(), "Unreachable " + name + " target=" + x + "," + y + "; position=" + run.player
            + "; lives=" + run.getLives() + "; bike=" + run.isRiding());
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
