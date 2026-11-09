package sangdong_lakepark.world;

import com.badlogic.gdx.math.MathUtils;

/** Hard physics and hazards, with permanent darkness interrupted by blue beacons. */
public final class ExtremeRun extends HardRun {
    public final ExtremeBeacons beacons=new ExtremeBeacons();
    private float lightTime,introSpot,lightFade,flash,returnDark;
    private boolean introFinished;
    public ExtremeRun(ExtremeMap map){
        super(map,new HardThunder(new HardThunder.Cloud(6912,6816,7008,248),
            new HardThunder.Cloud(15696,15648,15744,328)));
        reset();
    }
    @Override protected void onReset(){
        super.onReset();beacons.reset();lightTime=lightFade=flash=0;returnDark=1;
        introSpot=0;introFinished=false;
    }
    /** Called by the common READY/GO lifecycle; no simulation advances during the overlay. */
    public void finishIntro(){introFinished=true;}
    @Override protected void beforeStep(float dt){
        if(!isArriving()&&introFinished){
            introSpot=Math.min(1,introSpot+dt);
            lightTime=Math.max(0,lightTime-dt);flash=Math.max(0,flash-dt);
            if(lightTime>0)lightFade=Math.min(1,lightFade+dt/.3f);
            else returnDark=Math.min(1,returnDark+dt/.5f);
        }
        super.beforeStep(dt);
        if(!isArriving()&&!isCleared()&&!isFailed()&&introFinished)beacons.update(dt,this);
    }
    @Override protected void updateLaser(float dt){ }
    public void illuminate(float seconds){
        if(lightTime<=0)lightFade=1-darknessAlpha()/.92f;
        lightTime=seconds;returnDark=0;flash=.2f;
    }
    private void restoreDarkness(){
        lightTime=lightFade=flash=0;returnDark=1;introSpot=1;introFinished=true;beacons.rearm();
    }
    @Override protected void afterFall(float x){super.afterFall(x);restoreDarkness();}
    @Override protected void fall(PlatformMap.Gap gap){super.fall(gap);restoreDarkness();}
    @Override public void jumpToSection(int section){super.jumpToSection(section);restoreDarkness();}
    @Override public void darken(){ }
    @Override public boolean isDark(){return lightTime<=0;}
    @Override public float getDarkTime(){return lightTime<=0?Float.POSITIVE_INFINITY:0;}
    @Override public float darkRadius(){return 1500+(220-1500)*introEase();}
    private float introEase(){float t=1-introSpot;return 1-t*t*t;}
    public float darknessAlpha(){
        if(!introFinished)return 0;
        float light=lightTime>0?1-lightFade:returnDark;
        return .92f*introEase()*light;
    }
    public float getLightTime(){return lightTime;}
    public float getIntroSpot(){return introSpot;}
    public float getLightFlash(){return .35f*flash/.2f;}
    public boolean lightWarning(){return lightTime>0&&lightTime<=1&&((int)(lightTime*8))%2==0;}
    public float cameraLeft(){return MathUtils.clamp(player.x+44,640,map.worldWidth()-640)-640;}
}
