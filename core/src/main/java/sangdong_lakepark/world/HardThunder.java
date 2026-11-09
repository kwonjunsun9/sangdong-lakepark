package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.MathUtils;

/** The lake cloud repeats warning, strike and rest after entering the camera. */
public final class HardThunder {
    public static final class Cloud {
        public final float x,from,to,top;
        public Cloud(float x,float from,float to,float top){this.x=x;this.from=from;this.to=to;this.top=top;}
        public float age=-1;
        private boolean struck,hit;
        public boolean warning(){return age>=0&&age<1;}
        public boolean active(){return age>=1&&age<1.3f;}
        public boolean visible(float left,float right){return x+68>=left&&x-68<=right;}
    }
    public final Cloud[] clouds;
    public HardThunder(){this(new Cloud(6912,6816,7008,248));}
    public HardThunder(Cloud... clouds){this.clouds=clouds;}
    private int strikes;
    private int visibleStrikes;
    private Cloud lastVisibleStrike;
    public void reset(){strikes=visibleStrikes=0;lastVisibleStrike=null;rearm();}
    public void rearm(){for(Cloud c:clouds){c.age=-1;c.struck=c.hit=false;}}
    public void update(float dt,HardRun run){
        float center=run.player.x+44;
        float half=PlatformMap.VIEW_WIDTH/2;
        float left=MathUtils.clamp(center,half,run.map.worldWidth()-half)-half;
        for(Cloud c:clouds){
            if(c.age<0){if(center+640<c.x-68||center-640>c.x+68)continue;c.age=0;}
            c.age+=dt;
            if(c.age>=3){c.age%=3;c.struck=c.hit=false;}
            if(c.active()&&!c.struck){
                strikes++;c.struck=true;
                if(c.visible(left,left+PlatformMap.VIEW_WIDTH)){visibleStrikes++;lastVisibleStrike=c;}
            }
            if(c.active()&&!c.hit&&run.player.overlaps(new Rectangle(c.x-32,0,64,600))){run.stun();c.hit=true;}
        }
    }
    public int getStrikeCount(){return strikes;}
    public int getVisibleStrikeCount(){return visibleStrikes;}
    public Cloud getLastVisibleStrike(){return lastVisibleStrike;}
}
