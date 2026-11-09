package sangdong_lakepark.screens;

import sangdong_lakepark.audio.GameAudio;
import sangdong_lakepark.stages.StageId;
import sangdong_lakepark.ui.NormalArtwork;
import sangdong_lakepark.world.NormalMap;
import sangdong_lakepark.world.NormalRun;

/** The complete Normal course; menus, results and HUD share Easy's implementation. */
public final class NormalScreen extends PlatformScreen {
    public NormalScreen(GameAudio audio,Runnable onMap,Runnable onHome,Runnable onClear,Runnable onClearedMap) {
        this(new NormalMap(),audio,onMap,onHome,onClear,onClearedMap);
    }
    private NormalScreen(NormalMap map,GameAudio audio,Runnable onMap,Runnable onHome,Runnable onClear,Runnable onClearedMap) {
        super(audio,StageId.NORMAL,map,new NormalRun(map),new NormalArtwork(),onMap,onHome,onClear,onClearedMap);
    }
}
