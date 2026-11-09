package sangdong_lakepark.screens;

import sangdong_lakepark.audio.GameAudio;
import sangdong_lakepark.stages.StageId;
import sangdong_lakepark.ui.HardArtwork;
import sangdong_lakepark.world.HardMap;
import sangdong_lakepark.world.HardRun;

/** Complete Hard course with the common menus, READY/Go, HUD and result lifecycle. */
public final class HardScreen extends PlatformScreen {
    public HardScreen(GameAudio audio,Runnable onMap,Runnable onHome,Runnable onClear,Runnable onClearedMap){this(new HardMap(),audio,onMap,onHome,onClear,onClearedMap);}
    private HardScreen(HardMap map,GameAudio audio,Runnable onMap,Runnable onHome,Runnable onClear,Runnable onClearedMap){
        super(audio,StageId.HARD,map,new HardRun(map),new HardArtwork(),onMap,onHome,onClear,onClearedMap);
    }
}
