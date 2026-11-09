package sangdong_lakepark.screens;

import sangdong_lakepark.audio.GameAudio;
import sangdong_lakepark.stages.StageId;
import sangdong_lakepark.ui.ExtremeArtwork;
import sangdong_lakepark.world.ExtremeMap;
import sangdong_lakepark.world.ExtremeRun;

/** Extreme keeps the Clear record screen; its map action opens the final arrival scene. */
public final class ExtremeScreen extends PlatformScreen {
    public ExtremeScreen(GameAudio audio,Runnable onMap,Runnable onHome,Runnable onClear,Runnable onClearedMap){
        this(new ExtremeMap(),audio,onMap,onHome,onClear,onClearedMap);
    }
    private ExtremeScreen(ExtremeMap map,GameAudio audio,Runnable onMap,Runnable onHome,Runnable onClear,Runnable onClearedMap){
        super(audio,StageId.EXTREME,map,new ExtremeRun(map),new ExtremeArtwork(),onMap,onHome,onClear,onClearedMap);
    }
}
