package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import sangdong_lakepark.world.HardWind;

/** Cloud owl and horizontal gusts; all motion follows the pausable wind clock. */
final class OwlWindArtwork implements Disposable {
    private static final float LEFT=1952, RIGHT=2688, HEIGHT=160;
    private static final Color INK=Color.valueOf("24514D"), LEAF_EDGE=Color.valueOf("325B45"),
        LEAF=Color.valueOf("96C657"), LEAF_LIGHT=Color.valueOf("D0E486");
    private static final float[][] GUSTS={{1980,402,165},{2100,434,270},{2100,509,270},{2270,539,265},{2400,411,200}};
    private static final float[][] LEAVES={{1960,410},{2100,525},{2490,458}};
    private static final float[][] LEAF_POINTS={{-14,0},{-5,6},{4,7},{11,4},{14,4},{10,-2},{4,-5},{-4,-5}};
    private final Texture atlas;
    private final TextureRegion[] frames=new TextureRegion[4];

    OwlWindArtwork(){
        Pixmap pixels=new Pixmap(Gdx.files.internal("sangdong/hard/wind-owl-atlas.png"));
        try{
            atlas=new Texture(pixels,true);
            atlas.setFilter(Texture.TextureFilter.MipMapLinearLinear,Texture.TextureFilter.Linear);
            int left=pixels.getWidth()/4,top=pixels.getHeight(),right=0,bottom=0;
            for(int i=0;i<4;i++){
                int x=i*pixels.getWidth()/4,end=(i+1)*pixels.getWidth()/4;
                TextureRegion region=SpriteRegions.crop(pixels,atlas,x,0,end-x,pixels.getHeight());
                left=Math.min(left,region.getRegionX()-x);right=Math.max(right,region.getRegionX()-x+region.getRegionWidth());
                top=Math.min(top,region.getRegionY());bottom=Math.max(bottom,region.getRegionY()+region.getRegionHeight());
            }
            // One shared crop keeps the cloud baseline and sprite scale fixed between wing poses.
            for(int i=0;i<4;i++)frames[i]=new TextureRegion(atlas,i*pixels.getWidth()/4+left,top,right-left,bottom-top);
        }finally{pixels.dispose();}
    }

    void drawOwl(SpriteBatch batch,HardWind wind){
        int frame=wind.active()?2+((int)((wind.phase()-.6f)*8)%2):wind.warning()?1:0;
        TextureRegion image=frames[frame];
        float width=HEIGHT*image.getRegionWidth()/image.getRegionHeight();
        batch.draw(image,2752-width/2,445,width,HEIGHT);
    }

    void drawWind(ShapeRenderer shapes,HardWind wind){
        if(!wind.active())return;
        float time=wind.phase()-.6f;
        float alpha=Math.min(1,Math.min(time/.12f,(2-time)/.12f));
        for(float[] gust:GUSTS){
            float x=flowX(gust[0],time),length=Math.min(gust[2],RIGHT-x);
            if(length<42)continue;
            float edge=Math.min(1,(x-LEFT)/18);
            shapes.setColor(INK.r,INK.g,INK.b,alpha*edge);
            gust(shapes,x,gust[1],length);
        }
        for(float[] leaf:LEAVES)leaf(shapes,flowX(leaf[0],time),leaf[1],alpha);
        shapes.setColor(INK.r,INK.g,INK.b,alpha);
        for(int i=0;i<3;i++)shapes.rectLine(2648+i%2*8,533+i*6,2680,533+i*6,1.2f);
    }

    private float flowX(float x,float time){return LEFT+((x-LEFT-time*95)%(RIGHT-LEFT)+(RIGHT-LEFT))%(RIGHT-LEFT);}

    private void gust(ShapeRenderer shapes,float x,float y,float length){
        float radius=12;
        shapes.rectLine(x+radius,y,x+length,y,1.2f);
        shapes.rectLine(x+radius+16,y+6,x+length,y+6,1.2f);
        // Start at the left end of the level tail and curl inward above it.
        float px=x+radius,py=y;
        for(int i=1;i<=36;i++){
            float t=i/36f,angle=-MathUtils.PI/2-t*MathUtils.PI*2.1f,r=radius*(1-.8f*t);
            float nx=x+radius+MathUtils.cos(angle)*r,ny=y+radius+MathUtils.sin(angle)*r;
            shapes.rectLine(px,py,nx,ny,1.2f);px=nx;py=ny;
        }
    }

    private void leaf(ShapeRenderer shapes,float x,float y,float alpha){
        shapes.setColor(LEAF_EDGE.r,LEAF_EDGE.g,LEAF_EDGE.b,alpha);
        leafFill(shapes,x,y,1);
        shapes.setColor(LEAF.r,LEAF.g,LEAF.b,alpha);
        leafFill(shapes,x,y,.78f);
        shapes.setColor(LEAF_LIGHT.r,LEAF_LIGHT.g,LEAF_LIGHT.b,alpha);
        shapes.rectLine(x-8,y,x+8,y+3,1);
        shapes.setColor(LEAF_EDGE.r,LEAF_EDGE.g,LEAF_EDGE.b,alpha);
        shapes.rectLine(x+9,y+3,x+17,y+6,1.5f);
    }
    private void leafFill(ShapeRenderer shapes,float x,float y,float scale){
        for(int i=0;i<LEAF_POINTS.length;i++){
            float[] a=LEAF_POINTS[i],b=LEAF_POINTS[(i+1)%LEAF_POINTS.length];
            shapes.triangle(x,y,x+a[0]*scale,y+a[1]*scale,x+b[0]*scale,y+b[1]*scale);
        }
    }

    @Override public void dispose(){atlas.dispose();}
}
