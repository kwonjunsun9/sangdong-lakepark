package sangdong_lakepark.world;

/** Normal applies surface effects, poison timing and scooter-only coins. */
public final class NormalRun extends SurfaceRun {
    public NormalRun(NormalMap map){super(map,new NormalDog(),NormalMap.STING_PERIOD);reset();}
    @Override protected boolean canCollectCoin(int index){return(index!=3&&index!=4&&index!=9)||isRiding();}
}
