package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import java.util.Random;

/** Purple boxes drop equipment; brown boxes place it on their solid top surface. */
public final class HardBoxes {
    public enum Vehicle { NONE, BICYCLE, SCOOTER, MOTORCYCLE }
    public enum Kind { PURPLE, BROWN }
    public static final class Box extends PlatformMap.Block {
        public final int station;
        public final Kind kind;
        public final Rectangle pickup;
        public final float homeY,floorY;
        public Vehicle vehicle=Vehicle.NONE;
        public boolean opened;
        private float velocityY;
        private boolean moving;
        Box(float x,int station,float y,float floorY,Kind kind){
            super(x,y,PlatformMap.BlockKind.ITEM);this.station=station;this.kind=kind;this.floorY=floorY;
            homeY=kind==Kind.BROWN?y+64:floorY;pickup=new Rectangle(x,homeY,64,64);
        }
        public boolean falling(){return moving&&kind==Kind.PURPLE;}
        public boolean moving(){return moving;}
        private void returnHome(){pickup.setPosition(x,homeY);moving=false;velocityY=0;}
    }
    public final Array<Box> all;
    private Box mounted;
    private final Random random;
    private int openings,mounts;
    public HardBoxes(HardMap map){this(map,new Random());}
    public HardBoxes(HardMap map,Random random){all=map.boxes;this.random=random;}
    public void reset(){
        mounted=null;openings=mounts=0;
        for(int station=0;station<3;station++){
            Vehicle[] order=station==0?new Vehicle[]{Vehicle.BICYCLE,Vehicle.SCOOTER}
                :new Vehicle[]{Vehicle.BICYCLE,Vehicle.SCOOTER,Vehicle.MOTORCYCLE};
            if(station!=1)for(int i=order.length-1;i>0;i--){int j=random.nextInt(i+1);Vehicle v=order[i];order[i]=order[j];order[j]=v;}
            int i=0;
            for(Box b:all)if(b.station==station){
                b.vehicle=station==1?(b.kind==Kind.BROWN?Vehicle.SCOOTER:Vehicle.BICYCLE):order[i++];
                b.opened=false;b.returnHome();
            }
        }
    }
    public boolean open(Rectangle block){
        if(!(block instanceof Box)||!all.contains((Box)block,true))return false;
        Box b=(Box)block;
        if(!b.opened){
            b.opened=true;b.pickup.setPosition(b.x,b.kind==Kind.BROWN?b.homeY:b.y-64);
            b.velocityY=b.kind==Kind.BROWN?235:0;b.moving=true;openings++;
        }
        return true;
    }
    public void update(float dt){
        for(Box b:all)if(b.moving){
            b.velocityY-=PlatformMap.GRAVITY*dt;b.pickup.y+=b.velocityY*dt;
            if(b.velocityY<=0&&b.pickup.y<=b.homeY)b.returnHome();
        }
    }
    public Vehicle collect(Rectangle player){return collect(player,true);}
    public Vehicle collect(Rectangle player,boolean grounded){
        if(mounted!=null&&player.overlaps(mounted.pickup))return vehicle();
        Box closest=null;float distance=Float.MAX_VALUE;
        for(int i=0;i<all.size;i++){
            Box b=all.get(i);if(!available(b)||b.moving||!player.overlaps(b.pickup))continue;
            if(b.kind==Kind.BROWN&&(!grounded||Math.abs(player.y-b.homeY)>1))continue;
            float d=Math.abs(player.x+player.width/2-b.x-32);
            if(d<distance){closest=b;distance=d;}
        }
        if(closest!=null){if(mounted!=null)mounted.returnHome();mounted=closest;mounts++;}
        return vehicle();
    }
    public boolean available(Box b){return b.opened&&b!=mounted;}
    public Vehicle vehicle(){return mounted==null?Vehicle.NONE:mounted.vehicle;}
    public void release(){if(mounted!=null)mounted.returnHome();mounted=null;}
    /** A fall restarts the source station without reshuffling its equipment. */
    public Box restartMountedStation(){
        Box source=mounted;if(source==null)return null;
        mounted=null;
        for(Box b:all)if(b.station==source.station){b.opened=false;b.returnHome();}
        return source;
    }
    public int getOpeningCount(){return openings;}
    public int getMountCount(){return mounts;}
}
