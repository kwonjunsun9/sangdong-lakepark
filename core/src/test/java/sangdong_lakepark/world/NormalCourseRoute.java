package sangdong_lakepark.world;

/** Input-only route, including pit recovery, the wrong branch, timed bridges and scooter-only coins. */
public final class NormalCourseRoute {
    public interface Driver {void frame(float direction,boolean jump);default void visit(String name) {}}
    private final NormalMap map;
    private final NormalRun run;
    private final Driver driver;
    private NormalCourseRoute(NormalMap map,NormalRun run,Driver driver){this.map=map;this.run=run;this.driver=driver;}
    public static void play(NormalMap map,NormalRun run,boolean pitRecovery,Driver driver){new NormalCourseRoute(map,run,driver).play(pitRecovery);}
    private void play(boolean pitRecovery){
        waitFrames(140);
        go("coin-1-takeoff",722,184,false);go("coin-1",840,248,true);
        go("start-stair-1",880,248,true);go("start-stair-2",1008,312,true);go("start-stair-3",1136,376,true);
        go("shore-wait",1424,184,false);waitWater();go("gap-takeoff",1676,184,false);
        go("conveyor-1",1920,184,true);go("conveyor-1-edge",2092,184,false);
        go("conveyor-2",2272,248,true);go("conveyor-2-edge",2444,248,false);
        go("conveyor-3",2624,312,true);go("conveyor-3-edge",2796,312,false);
        go("conveyor-exit",2952,184,true);
        go("landing-trap",3104,184,true);go("between-traps",3204,184,false);waitFrames(120);
        go("second-trap-takeoff",3240,184,false);go("scooter-trap",3400,184,true);
        pickupScooter();
        go("hill-return",4012,382,false);
        if(pitRecovery){
            go("pit-fall-takeoff",4332,184,false);
            for(int i=0;i<240&&run.getFallCount()==0;i++)frame(1,false);
            require(run.getFallCount()==1&&!run.isRiding(),"Pit fall must dismount and respawn");
            driver.visit("pit-dismount");
            go("return-exit-step",4210,274,true);go("return-high-step",4012,382,true);
            go("return-item-ground",3768,184,false);go("return-item-hill",3616,280,true);
            go("return-item-approach",3464,184,false);
            pickupScooter();go("hill-return-again",4012,382,false);
        }
        go("high-coin-takeoff",4228,274,false);go("coin-4-5-gap",4752,184,true);
        require(run.isCollected(3)&&run.isCollected(4),"Scooter jump must collect both coins without the removed bridge");
        go("mud-trap-takeoff",4776,184,false);go("mud-trap-coin-and-shore-step-1",5094,248,true);
        go("shore-step-2",5200,312,true);
        go("shore-step-3",5328,376,true);
        go("shore-stair-edge",5352,376,false);go("coin-7",5544,184,true);
        go("lake-wait",5484,184,false);waitBlink();
        go("lake-takeoff",5624,184,false);go("blink-1",5800,248,true);
        go("blink-1-edge",5858,248,false);go("blink-2-coin",6028,296,true);
        go("blink-2-edge",6108,296,false);go("shore",6258,184,true);
        waitWater();go("fork-approach",6520,184,false);go("fork-stump",6628,296,true);
        go("lower-road",6928,184,false);go("dead-end",7720,184,false);
        require(run.player.x+run.player.width<=7808+.1f,"Lower branch remains a dead end");
        go("return-to-stump",6728,184,false);go("stump-again",6628,296,true);
        go("stump-edge",6664,296,false);go("bridge-1",6824,408,true);go("bridge-1-edge",6998,408,false);
        waitBlink();go("bridge-2",7144,472,true);go("bridge-2-edge",7318,472,false);
        go("bridge-3",7468,472,true);go("coin-9",7584,472,false);go("bridge-3-edge",7648,472,false);
        go("wall-top",7856,440,true);
        go("dog-coin-takeoff",7976,344,false);go("coin-10-and-dog",8374,184,true);
        go("dog-corridor",8984,184,false);
        go("last-step-1",9168,248,true);waitLastRock();go("last-step-2",9296,312,true);go("last-step-top",9424,376,true);
        go("finish-gap-takeoff",9668,184,false);go("finish-gap-landing",9952,184,true);
        go("goal",map.stopX()+30,184,false);require(run.isCleared(),"Normal should clear");
    }
    private void pickupScooter(){
        go("new-step-approach",3464,184,false);go("new-step",3568,280,true);
        go("item-head-takeoff",3648,Float.NaN,false);
        for(int i=0;i<90&&!run.isBicycleAvailable();i++)frame(1,i==0);
        require(run.isBicycleAvailable(),"Head bump must make the scooter available again");
        driver.visit("item-head");waitFrames(80);
        go("hill-approach",3824,184,false);go("pickup-low-step",3908,274,true);
        go("pickup-high-step",4012,382,true);go("pickup-high-edge",3984,Float.NaN,false);
        go("scooter-pickup",3744,494,true);
        require(run.isRiding(),"First block item must mount the scooter");
    }
    private void frame(float direction,boolean jump){
        if(run.isGrounded()){
            for(StageRocks.Rock rock:run.rocks.all){
                float distance=rock.bounds.x-run.player.x-run.player.width;
                if(rock.phase==StageRocks.Phase.ROLLING&&distance>0&&distance<230&&Math.abs(rock.bounds.y-run.player.y)<80)jump=true;
            }
            float dogDistance=run.dog.bounds.x-run.player.x-run.player.width;
            if((run.dog.dangerous()||run.dog.phase==NormalDog.Phase.WARNING&&run.dog.warningAge()>.32f)
                &&Math.abs(run.player.y-184)<1&&Math.abs(run.dog.bounds.x+36-run.player.x-44)<190)jump=true;
        }
        driver.frame(run.isReversed()?-direction:direction,jump);
    }
    private void waitFrames(int count){for(int i=0;i<count;i++)frame(0,false);}
    private void waitWater(){
        for(int i=0;i<600;i++){
            float phase=map.waterStreams.first().phase(run.getElapsed());if(phase>=2.2f&&phase<2.22f)return;
            frame(0,false);
        }throw new AssertionError("No stream rest window");
    }
    private void waitBlink(){
        for(int i=0;i<600;i++){
            float phase=map.blinking.first().phase(run.getElapsed());if(phase>=.05f&&phase<.07f)return;
            frame(0,false);
        }throw new AssertionError("No blink departure window");
    }
    private void waitFirstRock(){
        StageRocks.Rock rock=run.rocks.all.first();
        for(int i=0;i<1600;i++){
            if(rock.phase==StageRocks.Phase.GONE)return;
            float distance=rock.bounds.x-run.player.x-run.player.width;
            frame(0,run.isGrounded()&&rock.phase==StageRocks.Phase.ROLLING&&distance>0&&distance<150);
        }throw new AssertionError("First rock did not exit");
    }
    private void waitLastRock(){
        for(int i=0;i<700;i++){
            StageRocks.Phase phase=run.rocks.all.peek().phase;
            if(phase==StageRocks.Phase.SHATTERING||phase==StageRocks.Phase.GONE)return;
            frame(0,false);
        }throw new AssertionError("Last rock did not land");
    }
    private void go(String name,float x,float y,boolean jumpNow){
        if(jumpNow)frame(Math.signum(x-run.player.x),true);
        for(int i=0;i<1800&&!run.isCleared();i++){
            float direction=Math.abs(x-run.player.x)<=3?0:Math.signum(x-run.player.x);
            boolean slippery=run.isGrounded()&&map.onStrip(map.puddles,run.player);
            float velocity=run.getHorizontalVelocity(),distance=x-run.player.x;
            if(slippery&&Math.signum(velocity)==Math.signum(distance)
                &&Math.abs(distance)<velocity*velocity/(2*NormalMap.SLIDE_ACCELERATION)+2)direction=0;
            boolean jump=!Float.isNaN(y)&&run.isGrounded()&&y>run.player.y+1;
            frame(direction,jump);
            require(!run.isFailed(),"Died at "+name+" "+run.player);
            if(Math.abs(x-run.player.x)<=5&&run.isGrounded()&&(Float.isNaN(y)||Math.abs(y-run.player.y)<1)&&(!slippery||Math.abs(run.getHorizontalVelocity())<20)){driver.visit(name);return;}
        }
        require(run.isCleared(),"Unreachable "+name+" target="+x+","+y+" actual="+run.player+" lives="+run.getLives()+" riding="+run.isRiding());
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
