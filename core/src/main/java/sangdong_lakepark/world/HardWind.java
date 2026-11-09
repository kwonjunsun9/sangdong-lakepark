package sangdong_lakepark.world;

/** The owl's clock starts on camera entry and never advances during pause/READY. */
public final class HardWind {
    private float age;
    private boolean started;
    private int gusts;
    public void reset(){age=0;started=false;gusts=0;}
    public void update(float dt,float playerCenter){
        if(!started&&playerCenter+640>=2672)started=true;
        if(!started)return;
        boolean before=active();age+=dt;if(!before&&active())gusts++;
    }
    public float phase(){return age%4.6f;}
    public boolean warning(){return started&&phase()<.6f;}
    public boolean active(){return started&&phase()>=.6f&&phase()<2.6f;}
    public float velocity(float center){return active()&&center>=1952&&center<=2752?-200:0;}
    public int getGustCount(){return gusts;}
}
