package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import sangdong_lakepark.world.*;

/** After-rain scenery, supplied blue birds and vector props from the Normal layout. */
public class SurfaceArtwork extends StageArtwork {
    private final Texture purpleBirds=new Texture(Gdx.files.internal("sangdong/normal/purple-headband-bird-atlas.png"),true);
    private final TextureRegion[] wings;
    private final Texture scooter;
    private final TextureRegion scooterRider;
    protected SurfaceArtwork(String background) {
        super(background);
        purpleBirds.setFilter(Texture.TextureFilter.MipMapLinearLinear,Texture.TextureFilter.Linear);
        wings=TextureRegion.split(purpleBirds,purpleBirds.getWidth()/3,purpleBirds.getHeight())[0];
        Pixmap pixels=new Pixmap(Gdx.files.internal("sangdong/normal/scooter-rider.png"));
        try {
            scooter=new Texture(pixels,true);
            scooter.setFilter(Texture.TextureFilter.MipMapLinearLinear,Texture.TextureFilter.Linear);
            scooterRider=SpriteRegions.crop(pixels,scooter,0,0,pixels.getWidth(),pixels.getHeight());
        } finally {pixels.dispose();}
    }
    @Override public void drawHazards(Matrix4 projection,PlatformMap source,StageRun attempt) {
        super.drawHazards(projection,source,attempt);
        SurfaceMap map=(SurfaceMap)source;SurfaceRun run=(SurfaceRun)attempt;float time=(float)run.getElapsed();
        begin(projection);
        for(Rectangle strip:map.mud)drawMud(strip);
        for(Rectangle strip:map.puddles) {
            shapes.setColor(.35f,.8f,.94f,.86f);shapes.rect(strip.x,strip.y-18,strip.width,18);
            shapes.setColor(.84f,.99f,1,.9f);
            for(float x=strip.x+8;x<strip.x+strip.width-22;x+=43) {
                float offset=(time*17)%16;shapes.rect(x+offset,strip.y-5-(int)(x/43)%3*3,17,1.5f);
            }
        }
        for(int i=0;i<map.belts.length;i++)drawConveyor(map.belts[i],time,map.conveyorSpeed(i));
        for(PlatformMap.Block b:map.fakePlatforms)drawBlock(b.x,b.y,"9C713E",1);
        drawDog(run,time);
        for(NormalStings.Sting sting:run.stings.projectiles) {
            float dx=sting.velocity.x/NormalMap.STING_SPEED,dy=sting.velocity.y/NormalMap.STING_SPEED;
            float bx=sting.tip.x-dx*60,by=sting.tip.y-dy*60;
            shapes.setColor(Color.valueOf("43245F"));
            shapes.triangle(sting.tip.x,sting.tip.y,bx-dy*9,by+dx*9,bx+dy*9,by-dx*9);
            shapes.setColor(Color.valueOf("A67FE1"));
            shapes.triangle(sting.tip.x,sting.tip.y,bx-dy*4,by+dx*4,bx+dy*4,by-dx*4);
            shapes.setColor(Color.valueOf("9CDB45"));shapes.circle(bx,by+3,4,12);
        }
        for(NormalStings.Bird bird:run.stings.birds)if(bird.warning()) {
            float x=bird.position.x,y=bird.position.y-40;
            shapes.setColor(Color.valueOf("B99AF3"));shapes.triangle(x,y-16,x-5,y+7,x+5,y+7);
            shapes.setColor(Color.valueOf("D6F487"));shapes.circle(x,y+3,3+MathUtils.sin(time*30),12);
        }
        if(run.isReversed()&&!run.isCleared()) {
            drawDizzyStars(run.player,time);
        }
        if(run.isSliding()) {
            shapes.setColor(.74f,.96f,1,.85f);
            float sign=Math.signum(run.getHorizontalVelocity());
            for(int i=0;i<7;i++) {
                float age=(time*2+i*.14f)%1;
                shapes.circle(run.player.x+44-sign*(10+age*42),run.player.y+MathUtils.sin(age*MathUtils.PI)*14,2*(1-age)+1,10);
            }
        }
        end();
    }
    protected void drawMud(Rectangle strip) {
        shapes.setColor(Color.valueOf("64412D"));shapes.rect(strip.x,strip.y-22,strip.width,22);
        shapes.setColor(Color.valueOf("8F6542"));
        for(float x=strip.x+12;x<strip.x+strip.width-6;x+=29)shapes.ellipse(x,strip.y-9,12,5,12);
        shapes.setColor(Color.valueOf("3C2C23"));
        for(float x=strip.x+24;x<strip.x+strip.width-6;x+=47)shapes.ellipse(x,strip.y-18,9,4,12);
    }
    private void drawConveyor(Rectangle belt,float time,float speed) {
        float x=belt.x,y=belt.y,w=belt.width;
        shapes.setColor(Color.valueOf("303336"));pill(x,y,w,64);
        shapes.setColor(Color.valueOf("D9DDDC"));pill(x+4,y+4,w-8,56);
        shapes.setColor(Color.valueOf("343638"));pill(x+9,y+9,w-18,46);
        shapes.setColor(Color.valueOf("E9EBE8"));
        float offset=((time*speed)%17+17)%17;
        for(float tooth=x+32+offset;tooth<x+w-32;tooth+=17) {
            shapes.rect(tooth,y+48,6,6);shapes.rect(x+w-(tooth-x)-6,y+10,6,6);
        }
        for(float cx:new float[]{x+32,x+w-32}) {
            shapes.setColor(Color.valueOf("E6E8E5"));shapes.circle(cx,y+32,22,32);
            shapes.setColor(Color.valueOf("393C3E"));shapes.circle(cx,y+32,16,28);
            shapes.setColor(Color.valueOf("E6E8E5"));shapes.circle(cx,y+32,7,20);
        }
    }
    private void pill(float x,float y,float width,float height) {
        float radius=height/2;
        shapes.rect(x+radius,y,width-height,height);
        shapes.circle(x+radius,y+radius,radius,32);shapes.circle(x+width-radius,y+radius,radius,32);
    }
    private void drawDizzyStars(Rectangle player,float time) {
        float x=player.x+player.width/2,y=player.y+player.height+22;
        shapes.setColor(Color.valueOf("CFD2D0"));
        for(int i=0;i<48;i++) {
            float a=i*MathUtils.PI2/48,b=(i+1)*MathUtils.PI2/48;
            shapes.rectLine(x+MathUtils.cos(a)*47,y+MathUtils.sin(a)*9,
                x+MathUtils.cos(b)*47,y+MathUtils.sin(b)*9,2.5f);
        }
        for(int i=0;i<3;i++) {
            float a=time*MathUtils.PI2*2+i*MathUtils.PI2/3;
            float sx=x+MathUtils.cos(a)*47,sy=y+MathUtils.sin(a)*9;
            shapes.setColor(Color.valueOf("FFDC15"));
            for(int point=0;point<10;point++) {
                float from=MathUtils.PI/2+point*MathUtils.PI/5,to=from+MathUtils.PI/5;
                float r=point%2==0?10:4.5f,next=point%2==0?4.5f:10;
                shapes.triangle(sx,sy,sx+MathUtils.cos(from)*r,sy+MathUtils.sin(from)*r,
                    sx+MathUtils.cos(to)*next,sy+MathUtils.sin(to)*next);
            }
        }
    }
    private void drawDog(SurfaceRun run,float time) {
        NormalDog dog=run.dog;
        if(dog.visible()) {
            float x=dog.bounds.x+36,y=PlatformMap.GROUND_Y;
            boolean right=dog.phase==NormalDog.Phase.RETURNING;float direction=right?1:-1;
            if(dog.phase==NormalDog.Phase.RUNNING) {
                shapes.setColor(1,1,1,.8f);
                for(int i=0;i<3;i++)shapes.rectLine(x+45+i*9,y+22+i*15,x+75+i*8,y+22+i*15,3);
            }
            shapes.setColor(Color.valueOf("764321"));shapes.ellipse(x-43,y+17,83,44,24);
            for(int i=0;i<4;i++) {
                float swing=dog.phase==NormalDog.Phase.RUNNING||right?MathUtils.sin(time*28+i)*8:0;
                shapes.rect(x-30+i*19+swing,y+2,10,26);
            }
            shapes.setColor(Color.valueOf("B77B3E"));shapes.ellipse(x-40,y+24,78,38,24);
            shapes.circle(x+direction*40,y+53,26,24);
            shapes.setColor(Color.valueOf("6B3E24"));shapes.ellipse(x+direction*32-10,y+43,18,33,18);
            shapes.setColor(Color.valueOf("E2B476"));shapes.ellipse(x+direction*58-15,y+36,30,19,18);
            shapes.setColor(Color.valueOf("292521"));shapes.circle(x+direction*70,y+47,5,12);
            shapes.circle(x+direction*48,y+62,3.5f,12);
            shapes.setColor(Color.valueOf("B77B3E"));shapes.rectLine(x-direction*38,y+40,x-direction*58,y+65,10);
        }
        float shake=dog.phase==NormalDog.Phase.WARNING?MathUtils.sin(time*65)*5:0;
        float bx=dog.home+shake,by=PlatformMap.GROUND_Y;
        shapes.setColor(Color.valueOf("3D8139"));shapes.ellipse(bx-76,by-3,152,79,24);
        shapes.setColor(Color.valueOf("5BA84A"));shapes.circle(bx-42,by+30,32,20);shapes.circle(bx,by+42,40,20);shapes.circle(bx+43,by+28,35,20);
        shapes.setColor(Color.valueOf("82C15C"));shapes.ellipse(bx-42,by+45,25,16,16);shapes.ellipse(bx+11,by+58,26,14,16);
    }
    @Override public void drawAdditionalSprites(SpriteBatch batch,StageRun attempt) {
        SurfaceRun run=(SurfaceRun)attempt;float time=(float)run.getElapsed();
        for(NormalStings.Bird bird:run.stings.birds) {
            int frame=(int)(time*10)%4;if(frame==3)frame=1;
            float y=bird.position.y+MathUtils.sin(time*3)*8;
            batch.draw(wings[frame],bird.position.x-68,y-68,68,68,136,136,1,1,bird.warning()?8:0);
        }
    }
    @Override public TextureRegion ridingSprite() { return scooterRider; }
    protected void begin(Matrix4 projection) {
        Gdx.gl.glEnable(GL20.GL_BLEND);Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);shapes.begin(ShapeRenderer.ShapeType.Filled);
    }
    protected void end() {shapes.end();Gdx.gl.glDisable(GL20.GL_BLEND);}
    @Override public void dispose() {super.dispose();purpleBirds.dispose();scooter.dispose();}
}
