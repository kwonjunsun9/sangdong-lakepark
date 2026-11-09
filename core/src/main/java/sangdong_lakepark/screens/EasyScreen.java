package sangdong_lakepark.screens;

import sangdong_lakepark.audio.GameAudio;
import sangdong_lakepark.stages.StageId;
import sangdong_lakepark.ui.EasyArtwork;
import sangdong_lakepark.world.EasyMap;
import sangdong_lakepark.world.EasyRun;

/** Easy's existing layout and behavior, hosted by the shared gameplay screen. */
public final class EasyScreen extends PlatformScreen {
    public EasyScreen(GameAudio audio,Runnable onMap,Runnable onHome,Runnable onClear,Runnable onClearedMap) {
        this(new EasyMap(),audio,onMap,onHome,onClear,onClearedMap);
    }
    private EasyScreen(EasyMap map,GameAudio audio,Runnable onMap,Runnable onHome,Runnable onClear,Runnable onClearedMap) {
        super(audio,StageId.EASY,map,new EasyRun(map),new EasyArtwork(),onMap,onHome,onClear,onClearedMap);
    }
}
