package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import sangdong_lakepark.world.*;
import sangdong_lakepark.world.HardBoxes.Vehicle;

/** Sunset terrain, station equipment and the four Hard-only hazards. */
public class HardArtwork extends SurfaceArtwork {
    private static final Color STUN_INK=Color.valueOf("28251B"),STUN_OUTLINE=Color.valueOf("FFF5DD");
    private final Texture bike,motor,portal;
    private final TextureRegion bicycle,motorcycle,dismountPortal;
    private final OwlWindArtwork owlWind=new OwlWindArtwork();
    private HardMap hard;
    public HardArtwork(){this("sangdong/hard/sunset-night-background.png");}
    protected HardArtwork(String background){
        super(background);
        Pixmap pixels=new Pixmap(Gdx.files.internal("sangdong/easy/bicycle-rider.png"));
        try{
            bike=new Texture(pixels,true);
            bike.setFilter(Texture.TextureFilter.MipMapLinearLinear,Texture.TextureFilter.Linear);
            bicycle=SpriteRegions.crop(pixels,bike,0,0,pixels.getWidth(),pixels.getHeight());
        }finally{pixels.dispose();}
        pixels=new Pixmap(Gdx.files.internal("sangdong/hard/motorcycle-rider.png"));
        try{
            motor=new Texture(pixels,true);
            motor.setFilter(Texture.TextureFilter.MipMapLinearLinear,Texture.TextureFilter.Linear);
            motorcycle=SpriteRegions.crop(pixels,motor,0,0,pixels.getWidth(),pixels.getHeight());
        }finally{pixels.dispose();}
        pixels=new Pixmap(Gdx.files.internal("sangdong/hard/dismount-portal.png"));
        try{
            portal=new Texture(pixels,true);
            portal.setFilter(Texture.TextureFilter.MipMapLinearLinear,Texture.TextureFilter.Linear);
            dismountPortal=SpriteRegions.crop(pixels,portal,0,0,pixels.getWidth(),pixels.getHeight());
        }finally{pixels.dispose();}
    }
    @Override public void drawTerrain(SpriteBatch batch,float cameraX,PlatformMap map){
        hard=(HardMap)map;batch.setColor(1,.88f,.84f,1);super.drawTerrain(batch,cameraX,map);batch.setColor(Color.WHITE);
    }
    @Override protected boolean drawStandardGround(Rectangle r){return hard==null||r!=hard.ceiling;}
    @Override protected boolean drawStandardBlock(PlatformMap.Block b){
        return !(b instanceof HardBoxes.Box)&&!hard.ropeBridge.contains(b,true)&&!hard.caveBridge.contains(b,true);
    }
    @Override public void drawHazards(Matrix4 projection,PlatformMap map,StageRun attempt){
        hard=(HardMap)map;super.drawHazards(projection,map,attempt);
        HardRun run=(HardRun)attempt;float time=(float)run.getElapsed();begin(projection);
        // Warm surface glints retain the same animated waterline and collision plane.
        shapes.setColor(1,.62f,.4f,.65f);
        for(float x=6112;x<7392;x+=8)shapes.rectLine(x,PlatformMap.riverSurface(x,time),x+8,PlatformMap.riverSurface(x+8,time),2);
        for(HardBoxes.Box b:run.boxes.all)if(b.opened)drawOpenedBox(b);else drawClosedBox(b);
        drawBridge(hard.ropeBridge,run.ropeFall(),true,time,run.ropeWarning());
        drawBridge(hard.caveBridge,run.caveFall(),false,time,run.caveAge());
        Rectangle c=hard.ceiling;
        shapes.setColor(Color.valueOf("3D354A"));shapes.rect(c.x,c.y+16,c.width,c.height-16);
        shapes.setColor(Color.valueOf("625972"));shapes.rect(c.x+5,c.y+20,c.width-10,c.height-20);
        shapes.setColor(Color.valueOf("3A444C"));shapes.rect(c.x,c.y+16,c.width,8);
        for(float x=c.x;x<c.x+c.width;x+=32){
            // Metal teeth point down to the actual head-contact plane, y320.
            shapes.setColor(Color.valueOf("4A555C"));shapes.triangle(x+2,c.y+16,x+16,c.y,x+30,c.y+16);
            shapes.setColor(Color.valueOf("E3E9DF"));shapes.triangle(x+8,c.y+14,x+16,c.y+3,x+24,c.y+14);
            shapes.setColor(Color.valueOf("A6B2B8"));shapes.circle(x+16,c.y+20,2,8);
        }
        if(run.getDismountFlash()>0){
            float t=1-run.getDismountFlash()/.3f;shapes.setColor(1,.95f,.7f,1-t);
            for(int i=0;i<8;i++){float a=i*MathUtils.PI2/8;shapes.circle(run.player.x+44+MathUtils.cos(a)*(20+t*35),run.player.y+40+MathUtils.sin(a)*(20+t*35),5*(1-t),12);}
        }
        owlWind.drawWind(shapes,run.wind);lighthouse(run,time);thunder(run,time);
        if(run.getStunTime()>0)drawStun(run);
        if(run.boulder.visible()){
            Rectangle r=run.boulder.bounds;
            if(run.boulder.breaking()){
                float age=run.boulder.breakAge();shapes.setColor(.6f,.58f,.52f,1-age/.4f);
                for(int i=0;i<8;i++){
                    float sx=r.x+64+(i-3.5f)*110*age,sy=r.y+40+250*age-850*age*age;
                    shapes.triangle(sx-12,sy-8,sx+13,sy-6,sx+2,sy+18);
                }
            }
            shapes.setColor(.6f,.43f,.28f,.35f);
            for(int i=0;i<6;i++)shapes.ellipse(r.x-20-i*18,r.y-5,35,12,16);
        }
        end();
    }
    private void drawStun(HardRun run){
        float x=run.player.x+run.player.width/2,y=run.player.y+run.player.height+34;
        // A thin light rim keeps the dark ink visible against Extreme's night sky.
        for(int pass=0;pass<2;pass++){
            shapes.setColor(pass==0?STUN_OUTLINE:STUN_INK);
            float width=pass==0?6:4;
            drawStunSpiral(x-34,y,width);
            drawStunSpiral(x+34,y,width);
        }
    }
    private void drawStunSpiral(float x,float y,float width){
        float previousX=x+2,previousY=y;
        shapes.circle(previousX,previousY,width/2,10);
        for(int i=1;i<=80;i++){
            float t=i/80f,angle=t*MathUtils.PI2*2.125f,radius=2+26*t;
            float nextX=x+MathUtils.cos(angle)*radius,nextY=y+MathUtils.sin(angle)*radius;
            shapes.rectLine(previousX,previousY,nextX,nextY,width);
            shapes.circle(nextX,nextY,width/2,10);
            previousX=nextX;previousY=nextY;
        }
    }
    @Override protected void drawMud(Rectangle strip){
        if(strip!=hard.mud.first()){super.drawMud(strip);return;}
        // Cover the whole island's grass, following the same rounded edge as its collision surface.
        float radius=0;
        for(Rectangle ground:hard.ground)if(ground.x==strip.x&&ground.width==strip.width
            &&ground.y+ground.height==strip.y){radius=PlatformMap.cornerRadius(ground);break;}
        shapes.setColor(Color.valueOf("64412D"));
        for(int row=0;row<PlatformMap.GRASS_HEIGHT;row++){
            float distance=Math.max(0,radius-row-.5f);
            float inset=radius-(float)Math.sqrt(Math.max(0,radius*radius-distance*distance));
            shapes.rect(strip.x+inset,strip.y-row-1,strip.width-2*inset,1);
        }
        shapes.setColor(Color.valueOf("8F6542"));
        for(float x=strip.x+12;x<strip.x+strip.width-20;x+=29)shapes.ellipse(x,strip.y-9,12,5,12);
        shapes.setColor(Color.valueOf("3C2C23"));
        for(float x=strip.x+24;x<strip.x+strip.width-20;x+=47)shapes.ellipse(x,strip.y-18,9,4,12);
    }
    private void drawClosedBox(HardBoxes.Box b){
        drawQuestionBox(b.x,b.y,b.kind==HardBoxes.Kind.BROWN);
    }
    private void drawOpenedBox(HardBoxes.Box b){
        shapes.setColor(Color.valueOf("3B304D"));shapes.rect(b.x,b.y,64,64);
        shapes.setColor(Color.valueOf("787887"));shapes.rect(b.x+4,b.y+4,56,56);
        shapes.setColor(Color.valueOf("A5A6B2"));shapes.rect(b.x+7,b.y+54,50,4);
    }
    private void drawBridge(com.badlogic.gdx.utils.Array<PlatformMap.Block> bridge,float fall,boolean rope,float time,float age){
        if(fall>1.5f)return;
        float drop=fall<0?0:850*fall*fall;
        for(int i=0;i<bridge.size;i++){
            PlatformMap.Block b=bridge.get(i);float x=b.x,y=b.y-drop;
            if(rope&&age>=0&&fall<0)x+=MathUtils.sin(time*65+i)*3;
            if(fall>=0){x+=MathUtils.sin(i*2+fall)*fall*24;y-=i%3*fall*25;}
            if(!rope&&age>=1&&fall<0)x+=MathUtils.sin(time*65+i)*3;
            if(rope){
                shapes.setColor(Color.valueOf("604027"));shapes.rect(x,y,b.width,64);
                shapes.setColor(Color.valueOf("B88950"));shapes.rect(x+2,y+4,b.width-4,56);
                shapes.setColor(Color.valueOf("DEC08B"));shapes.rect(x+3,y+57,b.width-6,2);
                if(fall<0){shapes.setColor(Color.valueOf("A58452"));shapes.rectLine(x,y+96,x+b.width,y+96,3);if(i%3==0)shapes.rect(x+2,y+63,3,35);}
            }else{
                shapes.setColor(Color.valueOf("555261"));shapes.rect(x,y,64,64);
                shapes.setColor(Color.valueOf("A5A2AC"));shapes.rect(x+2,y+4,60,56);
                shapes.setColor(Color.valueOf("5D596B"));float size=age<0?12:Math.min(54,12+age*25);
                shapes.rectLine(x+13,y+61,x+24,y+61-size*.5f,2);shapes.rectLine(x+24,y+61-size*.5f,x+16,y+61-size,2);
            }
        }
    }
    private void drawPortal(SpriteBatch batch,float x,float y,float time){
        float height=144+MathUtils.sin(time*2.4f)*2;
        float width=height*dismountPortal.getRegionWidth()/dismountPortal.getRegionHeight();
        batch.draw(dismountPortal,x+18-width/2,y,width,height);
    }
    protected void lighthouse(HardRun run,float time){
        float x=7504,y=184;
        shapes.setColor(Color.valueOf("E4DDD1"));shapes.rect(x-30,y,60,300);
        shapes.setColor(Color.valueOf("CA4644"));
        for(int i=0;i<3;i++)shapes.rect(x-30,y+32+i*96,60,40);
        shapes.setColor(Color.valueOf("53535B"));shapes.rect(x-37,484,74,10);
        shapes.setColor(Color.valueOf(run.laser.warning()?"FFD45B":"EDAB5C"));shapes.rect(x-24,494,48,56);
        shapes.setColor(Color.valueOf("AE3D44"));shapes.triangle(x-36,550,x+36,550,x,580);
        if(!run.laser.warning()&&!run.laser.firing())return;
        float dx=run.laser.target.x-x,dy=run.laser.target.y-527,length=(float)Math.sqrt(dx*dx+dy*dy);
        if(length<1)return;
        dx/=length;dy/=length;
        if(run.laser.warning()){
            if(MathUtils.sin(time*28)>0){shapes.setColor(1,.22f,.18f,.8f);for(float d=0;d<length;d+=28)shapes.rectLine(x+dx*d,527+dy*d,x+dx*(d+14),527+dy*(d+14),3);}
        }else{
            shapes.setColor(1,.1f,.18f,.22f);shapes.rectLine(x,527,x+dx*18000,527+dy*18000,38);
            shapes.setColor(1,.16f,.2f,.8f);shapes.rectLine(x,527,x+dx*18000,527+dy*18000,24);
            shapes.setColor(1,.96f,.88f,.95f);shapes.rectLine(x,527,x+dx*18000,527+dy*18000,5);
        }
    }
    protected void thunder(HardRun run,float time){
        for(HardThunder.Cloud c:run.thunder.clouds){
            shapes.setColor(Color.valueOf(c.warning()&&MathUtils.sin(time*30)>0?"B1A6C4":"5C5574"));
            shapes.ellipse(c.x-68,583,136,42,24);shapes.circle(c.x-26,610,28,24);shapes.circle(c.x+24,615,32,24);
            if(c.warning()){shapes.setColor(1,.9f,.15f,.25f+MathUtils.sin(time*25)*.15f);shapes.ellipse(c.x-48,c.top-4,96,12,24);}
            if(c.active()){
                shapes.setColor(Color.valueOf("FFE457"));
                float px=c.x,py=600;
                for(int i=1;i<=8;i++){float nx=c.x+(i%2==0?-16:16),ny=600-i*(600-c.top)/8;shapes.rectLine(px,py,nx,ny,9);px=nx;py=ny;}
            }
        }
    }
    @Override public void drawItems(SpriteBatch batch,PlatformMap map,StageRun attempt){
        HardRun run=(HardRun)attempt;
        for(HardBoxes.Box b:run.boxes.all)if(run.boxes.available(b)){
            Rectangle r=b.pickup;
            items.drawVehicle(batch,b.vehicle,r.x,r.y,r.width,r.height);
        }
    }
    @Override public void drawAdditionalSprites(SpriteBatch batch,StageRun attempt){
        super.drawAdditionalSprites(batch,attempt);HardRun run=(HardRun)attempt;
        float time=(float)run.getElapsed();
        drawPortal(batch,HardMap.BICYCLE_EXIT,hard.groundTop(HardMap.BICYCLE_EXIT),time);
        drawPortal(batch,HardMap.SCOOTER_EXIT,hard.groundTop(HardMap.SCOOTER_EXIT),time);
        owlWind.drawOwl(batch,run.wind);
        if(run.boulder.visible()&&!run.boulder.breaking()){
            Rectangle r=run.boulder.bounds;batch.draw(rollingRock,r.x,r.y,64,64,128,128,1,1,run.boulder.rotation()*MathUtils.radiansToDegrees);
        }
    }
    @Override public TextureRegion ridingSprite(StageRun attempt){
        Vehicle v=((HardRun)attempt).vehicle();return v==Vehicle.BICYCLE?bicycle:v==Vehicle.MOTORCYCLE?motorcycle:super.ridingSprite();
    }
    @Override public void drawScreenEffects(Matrix4 projection,StageRun attempt){
        HardRun run=(HardRun)attempt;
        if(!run.isDark()&&run.getStunTime()<=.85f)return;
        begin(projection);
        if(run.isDark())drawDarkness(run,run.darkRadius(),.92f);
        if(run.getStunTime()>.85f){shapes.setColor(1,1,.95f,(run.getStunTime()-.85f)*2);shapes.rect(run.player.x-1500,0,3000,720);}
        end();
    }
    protected void drawDarkness(HardRun run,float radius,float opacity){
        float cx=run.player.x+44,cy=run.player.y+60,r=radius;
        for(int i=0;i<128;i++){
            float a=i*MathUtils.PI2/128,b=(i+1)*MathUtils.PI2/128;
            for(int band=0;band<8;band++){
                float inner=r+band*10,outer=band==7?3000:inner+10;
                shapes.setColor(0,0,.025f,opacity*(band+1)/8);
                float ax=cx+MathUtils.cos(a)*inner,ay=cy+MathUtils.sin(a)*inner,bx=cx+MathUtils.cos(b)*inner,by=cy+MathUtils.sin(b)*inner;
                float ox=cx+MathUtils.cos(a)*outer,oy=cy+MathUtils.sin(a)*outer,px=cx+MathUtils.cos(b)*outer,py=cy+MathUtils.sin(b)*outer;
                shapes.triangle(ax,ay,ox,oy,px,py);shapes.triangle(ax,ay,px,py,bx,by);
            }
        }
    }
    @Override public void dispose(){super.dispose();bike.dispose();motor.dispose();portal.dispose();owlWind.dispose();}
}
