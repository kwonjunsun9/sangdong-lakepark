package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import sangdong_lakepark.world.HardBoxes.Vehicle;

/** One complete Hard attempt, including station ownership and resettable environmental hazards. */
public class HardRun extends SurfaceRun {
    public final HardBoxes boxes;
    public final HardWind wind=new HardWind();
    public final HardLaser laser=new HardLaser();
    public final HardThunder thunder;
    public final HardBoulder boulder=new HardBoulder();
    private final HardMap hard;
    private float darkness,stunTime,jumpLock,dismountFlash,previousCenter;
    private float caveAge=-1,ropeFall=-1,caveFall=-1,ropeWarning=-1;
    private int collapses;
    public HardRun(HardMap map){this(map,new HardThunder());reset();}
    /** Subclasses reset after their own state has been initialized. */
    protected HardRun(HardMap map,HardThunder thunder){
        super(map,new NormalDog(14880,14092,14384,14872,400),3);
        hard=map;boxes=new HardBoxes(map);this.thunder=thunder;
    }
    @Override protected void onReset(){
        super.onReset();boxes.reset();wind.reset();laser.reset();thunder.reset();boulder.reset();
        darkness=stunTime=jumpLock=dismountFlash=0;collapses=0;restoreBridges();
    }
    private void restoreBridges(){
        ropeWarning=-1;ropeFall=-1;restoreCave();
        for(PlatformMap.Block b:hard.ropeBridge)if(!hard.blocks.contains(b,true))hard.blocks.add(b);
    }
    private void restoreCave(){
        caveAge=caveFall=-1;
        for(PlatformMap.Block b:hard.caveBridge)if(!hard.blocks.contains(b,true))hard.blocks.add(b);
    }
    @Override public void jumpToSection(int section){
        super.jumpToSection(section);
        if(section>0){laser.resetAfter(player.x);boulder.resetAfter(player.x);dog.resetAfter(player.x);}
        if(section==3){
            for(HardBoxes.Box b:boxes.all)if(b.station==0&&b.vehicle==Vehicle.BICYCLE){
                boxes.open(b);boxes.update(1);boxes.collect(b.pickup);bicycle=true;break;
            }
        }
    }
    @Override protected void beforeStep(float dt){
        previousCenter=player.x+44;
        darkness=Math.max(0,darkness-dt);stunTime=Math.max(0,stunTime-dt);
        jumpLock=Math.max(0,jumpLock-dt);dismountFlash=Math.max(0,dismountFlash-dt);
        if(ropeFall>=0)ropeFall+=dt;if(caveFall>=0)caveFall+=dt;
        if(ropeWarning>=0&&ropeFall<0){ropeWarning+=dt;if(ropeWarning>=.4f)collapse(true);}
        boxes.update(dt);
        if(isArriving())return;
        super.beforeStep(dt);
        wind.update(dt,player.x+44);updateLaser(dt);thunder.update(dt,this);boulder.update(dt,this,hard);
        if(caveAge>=0&&caveFall<0){caveAge+=dt;if(caveAge>=1.6f)collapse(false);}
    }
    protected void updateLaser(float dt){laser.update(dt,this);}
    @Override protected float adjustedDirection(float direction){return stunTime>0?0:super.adjustedDirection(direction);}
    @Override protected boolean allowsJump(){return stunTime<=0&&jumpLock<=0;}
    @Override protected int maximumJumps(){return vehicle()==Vehicle.BICYCLE||vehicle()==Vehicle.MOTORCYCLE?2:1;}
    @Override protected float travelSpeed(){return vehicle()==Vehicle.SCOOTER||vehicle()==Vehicle.MOTORCYCLE?520:260;}
    @Override protected float movementVelocity(float dt,float direction){
        return super.movementVelocity(dt,direction)+wind.velocity(player.x+44);
    }
    @Override protected boolean onHeadHit(Rectangle block){
        if(block==hard.ceiling){
            if(invulnerability<=0&&!failed){damageCount++;loseLife();}
            return true;
        }
        if(vehicle()==Vehicle.MOTORCYCLE&&hard.ropeBridge.contains((block instanceof PlatformMap.Block)?(PlatformMap.Block)block:null,true))collapse(true);
        if(hard.isEquipmentRow(block))for(HardBoxes.Box box:boxes.all)
            if(box.station==2&&player.x+66>box.x&&player.x+22<box.x+box.width)return boxes.open(box);
        return boxes.open(block);
    }
    @Override protected boolean headOverlaps(Rectangle block){
        // Joined wooden cells must use the same head width as their neighboring equipment boxes.
        if(block instanceof HardBoxes.Box||hard.isEquipmentRow(block))return player.x+66>block.x&&player.x+22<block.x+block.width;
        return super.headOverlaps(block);
    }
    @Override protected void afterMovement(float dt){
        if(isCleared()||isFailed())return;
        super.afterMovement(dt);
        boxes.collect(player,grounded);bicycle=vehicle()!=Vehicle.NONE;
        if(bicycle&&(vehicle()==Vehicle.BICYCLE&&previousCenter<HardMap.BICYCLE_EXIT&&player.x+44>=HardMap.BICYCLE_EXIT
            ||vehicle()==Vehicle.SCOOTER&&previousCenter<HardMap.SCOOTER_EXIT&&player.x+44>=HardMap.SCOOTER_EXIT))dismount();
        if(grounded){
            if(vehicle()==Vehicle.BICYCLE&&ropeWarning<0&&ropeFall<0&&player.y==408
                &&player.x+66>4320&&player.x+22<4576)ropeWarning=0;
            if(onBridge(hard.ropeBridge)&&vehicle()==Vehicle.MOTORCYCLE)collapse(true);
            if(onBridge(hard.caveBridge)){
                if(vehicle()==Vehicle.MOTORCYCLE)collapse(false);
                else if(caveAge<0)caveAge=0;
            }
        }
    }
    private boolean onBridge(com.badlogic.gdx.utils.Array<PlatformMap.Block> bridge){
        float foot=player.x+22;
        for(PlatformMap.Block b:bridge)if(hard.blocks.contains(b,true)&&Math.abs(player.y-b.y-b.height)<1
            &&Math.min(foot+44,b.x+b.width)-Math.max(foot,b.x)>=8)return true;
        return false;
    }
    private void collapse(boolean rope){
        if(rope?ropeFall>=0:caveFall>=0)return;
        for(PlatformMap.Block b:rope?hard.ropeBridge:hard.caveBridge)hard.blocks.removeValue(b,true);
        if(rope)ropeFall=0;else caveFall=0;
        collapses++;grounded=false;
        if(vehicle()==Vehicle.MOTORCYCLE)jumpLock=.5f;
    }
    public Vehicle vehicle(){return boxes.vehicle();}
    @Override protected boolean canCollectCoin(int index){
        return index==4?vehicle()==Vehicle.BICYCLE:index==5?vehicle()==Vehicle.SCOOTER:index!=8||vehicle()==Vehicle.MOTORCYCLE;
    }
    public void dismount(){boxes.release();bicycle=false;dismountFlash=.3f;}
    @Override protected void hit(){boolean riding=bicycle;super.hit();if(riding&&!bicycle){boxes.release();dismountFlash=.3f;}}
    @Override protected void fall(PlatformMap.Gap gap){
        HardBoxes.Box source=boxes.restartMountedStation();
        if(source!=null){
            bicycle=false;dismountFlash=.3f;restoreBridges();
            gap=new PlatformMap.Gap(gap.x,gap.x+gap.width,source.x-12,source.floorY,gap.lake);
        }else if((ropeFall>=0||ropeWarning>=0)&&gap.x>=3744&&gap.x<5600)
            gap=new PlatformMap.Gap(gap.x,gap.x+gap.width,2990,24,false);
        super.fall(gap);
    }
    @Override protected void afterFall(float x){
        super.afterFall(x);restoreCave();if(x==2990)restoreBridges();
        laser.resetAfter(x);thunder.rearm();boulder.resetAfter(x);wind.reset();
        darkness=stunTime=jumpLock=0;
    }
    public void darken(){darkness=2;}
    public boolean isDark(){return darkness>0;}
    public float darkRadius(){return 220;}
    public float getDarkTime(){return darkness;}
    public void stun(){stunTime=1;}
    public float getStunTime(){return stunTime;}
    public float getDismountFlash(){return dismountFlash;}
    public float caveAge(){return caveAge;}
    public float ropeFall(){return ropeFall;}
    public float ropeWarning(){return ropeWarning;}
    public float caveFall(){return caveFall;}
    public int getCollapseCount(){return collapses;}
    public void caughtByBoulder(){
        dismount();damageCount++;loseLife();
        if(!isFailed())respawn(new Vector2(10884,360));
    }
    @Override public int getPickupCount(){return boxes.getOpeningCount();}
    @Override public int getBicycleMountCount(){return boxes.getMountCount();}
}
