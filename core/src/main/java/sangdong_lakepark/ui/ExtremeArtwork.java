package sangdong_lakepark.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import sangdong_lakepark.world.*;

/** Keep ordinary scenery below darkness and luminous warnings above it. */
public final class ExtremeArtwork extends HardArtwork {
    private final GlyphLayout goalLabel=new GlyphLayout();
    public ExtremeArtwork(){super("sangdong/extreme/extreme-night-background.png");}
    /** Extreme keeps the original flower arch; other stages use the wooden sign. */
    @Override public void drawGoal(SpriteBatch batch,PlatformMap map,BitmapFont font){
        batch.end();begin(batch.getProjectionMatrix());
        Rectangle arch=map.arch;
        float center=arch.x+arch.width/2,spring=arch.y+arch.height-arch.width/2;
        shapes.setColor(Color.valueOf("5B8B42"));
        shapes.rect(arch.x,arch.y,30,spring-arch.y);shapes.rect(arch.x+arch.width-30,arch.y,30,spring-arch.y);
        for(int i=0;i<32;i++){
            float a=i*MathUtils.PI/32,b=(i+1)*MathUtils.PI/32;
            float x0=center+MathUtils.cos(a)*100,y0=spring+MathUtils.sin(a)*100;
            float x1=center+MathUtils.cos(b)*100,y1=spring+MathUtils.sin(b)*100;
            float x2=center+MathUtils.cos(a)*70,y2=spring+MathUtils.sin(a)*70;
            float x3=center+MathUtils.cos(b)*70,y3=spring+MathUtils.sin(b)*70;
            shapes.triangle(x0,y0,x1,y1,x2,y2);shapes.triangle(x1,y1,x2,y2,x3,y3);
        }
        for(int i=0;i<15;i++){
            float a=i*MathUtils.PI/14,x=center+MathUtils.cos(a)*85,y=spring+MathUtils.sin(a)*85;
            shapes.setColor(Color.valueOf("A1C45A"));shapes.ellipse(x-9,y-5,18,10,12);
            if(i%2==0)flower(x,y,i%4==0?Color.valueOf("F6A8B6"):MenuTheme.CREAM);
        }
        for(int i=0;i<4;i++){
            flower(arch.x+15,arch.y+20+i*30,MenuTheme.CREAM);
            flower(arch.x+arch.width-15,arch.y+20+i*30,Color.valueOf("F6A8B6"));
        }
        shapes.setColor(Color.valueOf("715237"));shapes.rect(arch.x+18,arch.y+arch.height-8,arch.width-36,34);
        shapes.setColor(MenuTheme.CREAM);shapes.rect(arch.x+22,arch.y+arch.height-4,arch.width-44,26);
        end();batch.begin();
        Color previous=new Color(font.getColor());font.setColor(MenuTheme.INK);goalLabel.setText(font,"상동호수공원");
        font.draw(batch,goalLabel,arch.x+(arch.width-goalLabel.width)/2,arch.y+arch.height+16);font.setColor(previous);
    }
    private void flower(float x,float y,Color color){
        shapes.setColor(color);
        for(int i=0;i<5;i++)shapes.circle(x+MathUtils.cos(i*MathUtils.PI2/5)*5,y+MathUtils.sin(i*MathUtils.PI2/5)*5,4,10);
        shapes.setColor(MenuTheme.GOLD);shapes.circle(x,y,3,10);
    }
    @Override public void drawBehindTerrain(Matrix4 projection,PlatformMap map,StageRun attempt){
        begin(projection);
        for(ExtremeBeacons.Beacon b:((ExtremeRun)attempt).beacons.all){
            float x=b.origin.x,y=b.baseY;
            shapes.setColor(Color.valueOf("E4EDF5"));shapes.rect(x-30,y,60,484-y);
            shapes.setColor(Color.valueOf("3270DC"));
            for(float top=y+32;top<484;top+=96)shapes.rect(x-30,top,60,Math.min(40,484-top));
            shapes.setColor(Color.valueOf("535D73"));shapes.rect(x-37,484,74,10);
            shapes.setColor(Color.valueOf("3270DC"));shapes.triangle(x-36,550,x+36,550,x,580);
        }
        end();
    }
    @Override protected void lighthouse(HardRun run,float time){ }
    @Override protected void thunder(HardRun run,float time){ }
    @Override public void drawScreenEffects(Matrix4 projection,StageRun attempt){
        ExtremeRun run=(ExtremeRun)attempt;float time=(float)run.getElapsed();begin(projection);
        float alpha=run.darknessAlpha();
        if(alpha>0)drawDarkness(run,run.darkRadius(),alpha);
        if(run.getLightFlash()>0){shapes.setColor(.59f,.86f,1,run.getLightFlash());shapes.rect(run.cameraLeft(),0,1280,720);}
        if(run.lightWarning()){
            float left=run.cameraLeft();shapes.setColor(.2f,.55f,1,.6f);
            shapes.rect(left,0,10,720);shapes.rect(left+1270,0,10,720);
            shapes.rect(left,0,1280,10);shapes.rect(left,710,1280,10);
        }
        for(ExtremeBeacons.Beacon b:run.beacons.all)drawLight(b,time);
        super.thunder(run,time);
        if(run.getStunTime()>.85f){shapes.setColor(1,1,.95f,(run.getStunTime()-.85f)*2);shapes.rect(run.cameraLeft(),0,1280,720);}
        end();
    }
    private void drawLight(ExtremeBeacons.Beacon b,float time){
        float x=b.origin.x,y=b.origin.y;
        for(int i=4;i>0;i--){shapes.setColor(.35f,.7f,1,.045f);shapes.circle(x,y,28+i*7,32);}
        shapes.setColor(Color.valueOf("96DCFF"));shapes.rect(x-24,494,48,56);
        shapes.setColor(.85f,.97f,1,1);shapes.circle(x,y,14,24);
        if(!b.warning()&&!b.firing())return;
        float dx=b.target.x-x,dy=b.target.y-y,length=(float)Math.sqrt(dx*dx+dy*dy);
        if(length<.001f){dx=0;dy=-1;length=1;}
        dx/=length;dy/=length;
        if(b.warning()){
            if(MathUtils.sin(time*28)>0){
                shapes.setColor(.35f,.75f,1,.85f);
                for(float d=0;d<length;d+=28)shapes.rectLine(x+dx*d,y+dy*d,x+dx*(d+14),y+dy*(d+14),3);
            }
        }else{
            shapes.setColor(.2f,.55f,1,.22f);shapes.rectLine(x,y,x+dx*18000,y+dy*18000,38);
            shapes.setColor(.35f,.75f,1,.8f);shapes.rectLine(x,y,x+dx*18000,y+dy*18000,24);
            shapes.setColor(.9f,.98f,1,.95f);shapes.rectLine(x,y,x+dx*18000,y+dy*18000,5);
        }
    }
}
