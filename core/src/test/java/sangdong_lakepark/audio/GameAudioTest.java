package sangdong_lakepark.audio;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

public class GameAudioTest {
    private Application previousApp; private Audio previousAudio; private Files previousFiles;
    private final Map<String, Track> music = new HashMap<>(), sounds = new HashMap<>();
    private GameAudio audio;

    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
    private static Object empty(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == int.class) return 0;
        return null;
    }
    @Before public void setUp() {
        previousApp = Gdx.app; previousAudio = Gdx.audio; previousFiles = Gdx.files;
        Map<String, Boolean> saved = new HashMap<>();
        Preferences prefs = proxy(Preferences.class, (p, method, args) -> {
            if (method.getName().equals("getBoolean")) return saved.getOrDefault(args[0], args.length > 1 && (Boolean) args[1]);
            if (method.getName().equals("putBoolean")) { saved.put((String) args[0], (Boolean) args[1]); return p; }
            return empty(method.getReturnType());
        });
        Gdx.app = proxy(Application.class, (p, method, args) -> method.getName().equals("getPreferences") ? prefs : empty(method.getReturnType()));
        Gdx.files = proxy(Files.class, (p, method, args) -> method.getName().equals("classpath") ? new FileHandle((String) args[0]) : empty(method.getReturnType()));
        Gdx.audio = proxy(Audio.class, (p, method, args) -> {
            if (method.getName().equals("newMusic") || method.getName().equals("newSound")) {
                String path = ((FileHandle) args[0]).path(); Track track = new Track();
                if (method.getName().equals("newMusic")) { track.music = proxy(Music.class, track); music.put(path, track); return track.music; }
                sounds.put(path, track); return proxy(Sound.class, track);
            }
            return empty(method.getReturnType());
        });
        audio = new GameAudio();
    }
    @After public void tearDown() {
        if (audio != null) audio.dispose();
        Gdx.app = previousApp; Gdx.audio = previousAudio; Gdx.files = previousFiles;
    }
    private Track track(String name) { return music.get("audio/" + name + ".mp3"); }
    private Track sound(String name) { return sounds.get("audio/" + name + ".mp3"); }
    private Track wave(String name) { return sounds.get("audio/" + name + ".wav"); }

    @Test public void failStopsEveryOtherSourceAndCompletionNeverRestartsBackground() {
        audio.startEasy(); audio.playGo(); audio.playCoin(); audio.playBicycle(); audio.playJump(); audio.playDamage();
        audio.playFall(); Track oldFall = track("넌망한거야"); Music.OnCompletionListener stale = oldFall.listener;
        audio.playFail(); audio.playFail();
        assertFalse(track("easygameplay").playing); assertFalse(track("gamestart").playing); assertFalse(oldFall.playing);
        for (Track s : sounds.values()) assertFalse(s.playing);
        assertEquals(1, track("fail").plays); assertTrue(track("fail").playing); assertFalse(track("fail").looping);
        assertEquals(.55f, track("fail").volume, .001f);
        stale.onCompletion(oldFall.music); audio.playFall(); audio.playStageClear(); audio.playJump(); audio.playWindow();
        assertEquals(1, oldFall.plays); assertEquals(0, track("stageclear").plays); assertFalse(wave("opensound").playing);
        audio.pause(); assertFalse(track("fail").playing); audio.resume(); assertTrue(track("fail").playing);
        audio.toggleEnabled(); assertEquals(0, track("fail").volume, 0); track("fail").complete();
        audio.toggleEnabled(); audio.resume(); assertFalse(track("easygameplay").playing); assertFalse(track("gamestart").playing);
        audio.startEasy(); assertTrue(track("easygameplay").playing); assertEquals(.35f, track("easygameplay").volume, .001f);
        audio.playFail(); audio.endEasy(); assertFalse(track("fail").playing); assertTrue(track("gamestart").playing);
    }

    @Test public void windowSoundWorksWhileMenuPausesMusicButRespectsMute() {
        audio.startEasy(); audio.pause(); audio.playWindow();
        assertTrue(wave("opensound").playing); assertFalse(track("easygameplay").playing);
        assertEquals(.65f, wave("opensound").volume, .001f);
        audio.toggleEnabled(); assertFalse(wave("opensound").playing); audio.playWindow(); assertEquals(1, wave("opensound").plays);
        audio.toggleEnabled(); audio.resume(); audio.playWindow(); assertEquals(2, wave("opensound").plays);
    }

    @Test public void audibleGameplayEffectsDoNotReplayAfterMuteOrRetry() {
        audio.startEasy(); audio.playJump(); audio.playDamage(); audio.playBump(); audio.playPickup();
        Track[] effects = {sound("jump"), wave("damage"), sound("bump"), wave("pickup")};
        for (Track effect : effects) {
            assertEquals(1, effect.plays); assertTrue(effect.volume >= .24f && effect.volume <= .65f);
        }
        audio.pause(); for (Track effect : effects) assertTrue(effect.paused);
        audio.resume(); for (Track effect : effects) assertEquals(1, effect.plays);
        audio.toggleEnabled(); audio.playJump(); audio.playDamage(); audio.playBump(); audio.playPickup();
        for (Track effect : effects) { assertFalse(effect.playing); assertEquals(1, effect.plays); }
        audio.toggleEnabled(); for (Track effect : effects) assertEquals(1, effect.plays);
        audio.playJump(); audio.startEasy(); assertFalse(sound("jump").playing);
    }

    @Test public void keyboardSelectionSoundsInPausedAndFailedMenusWithoutRestartingMusic() {
        audio.startEasy(); audio.pause(); audio.playSelection();
        assertEquals(1, wave("opensound").plays); assertFalse(track("easygameplay").playing);
        audio.resume(); audio.playFail(); audio.playSelection();
        assertEquals(2, wave("opensound").plays); assertTrue(track("fail").playing);
        assertFalse(track("easygameplay").playing); assertFalse(track("gamestart").playing);
        audio.toggleEnabled(); audio.playSelection(); assertEquals(2, wave("opensound").plays);
        track("fail").complete(); audio.toggleEnabled(); audio.playSelection();
        assertEquals(3, wave("opensound").plays); assertFalse(track("easygameplay").playing);
    }

    @Test public void planArrivalAndHazardSoundsRespectPauseMuteAndFailPriority() {
        audio.playArrival(); assertEquals(1, sound("fallingwhistle").plays);
        audio.pause(); assertTrue(sound("fallingwhistle").paused);
        audio.resume(); assertEquals(1, sound("fallingwhistle").plays);
        audio.stopArrival(); assertFalse(sound("fallingwhistle").playing);
        audio.startEasy(); audio.playArrival(); audio.playFallingRock(); audio.playRollingRock();
        audio.playWaterStream(); audio.playWaterEntry();
        for (String name : new String[]{"fallingrock", "rollingrock", "splash", "watersplash"}) {
            assertEquals(1, sound(name).plays); assertTrue(sound(name).volume > 0);
        }
        audio.stopWaterStream(); assertFalse(sound("splash").playing);
        audio.toggleEnabled(); audio.playFallingRock(); audio.playArrival();
        assertEquals(1, sound("fallingrock").plays); assertEquals(2, sound("fallingwhistle").plays);
        audio.toggleEnabled(); audio.playWaterStream(); audio.playFail();
        for (Track source : sounds.values()) assertFalse(source.playing);
        assertTrue(track("fail").playing);
    }

    @Test public void easyReplacesMenuAndRetryRestartsAtNormalVolume() {
        assertTrue(track("gamestart").playing); assertNull(track("easygameplay"));
        audio.startEasy(); Track easy = track("easygameplay");
        assertFalse(track("gamestart").playing); assertTrue(easy.playing); assertTrue(easy.looping);
        easy.position = 12; audio.playCoin(); assertEquals(1, sound("coinsound").plays);
        audio.endEasy(); assertFalse(easy.playing); assertTrue(track("gamestart").playing);
        audio.startEasy(); assertEquals(0, easy.position, 0); assertEquals(.35f, easy.volume, .001f);
    }
    @Test public void clearDucksWithoutRestartingBackgroundAndPlaysOnce() {
        audio.startEasy(); Track easy = track("easygameplay"); easy.position = 12;
        audio.playStageClear(); audio.playStageClear();
        assertTrue(easy.playing); assertEquals(1, easy.plays); assertEquals(12, easy.position, 0);
        assertEquals(.22f, easy.volume, .001f); assertEquals(1, track("stageclear").plays); assertFalse(track("stageclear").looping);
        track("stageclear").complete(); assertEquals(.22f, easy.volume, .001f);
        audio.playFall(); assertEquals(0, track("넌망한거야").plays);
    }
    @Test public void fallRestoresBackgroundAfterItsCueFinishes() {
        audio.startEasy(); Track easy = track("easygameplay"); easy.position = 8;
        audio.playFall(); assertEquals(.22f, easy.volume, .001f); assertTrue(easy.playing);
        track("넌망한거야").complete(); assertEquals(.35f, easy.volume, .001f);
        assertEquals(8, easy.position, 0); assertEquals(1, easy.plays);
    }
    @Test public void bicycleCueRespectsPauseMuteRetryAndLeaving() {
        audio.startEasy(); audio.playBicycle(); Track bike = sound("bicyclesound");
        assertEquals(1, bike.plays); assertTrue(bike.playing);
        audio.pause(); assertTrue(bike.paused); audio.playBicycle(); assertEquals(1, bike.plays);
        audio.resume(); assertFalse(bike.paused); assertEquals(1, bike.plays);
        audio.toggleEnabled(); assertFalse(bike.playing); audio.playBicycle(); assertEquals(1, bike.plays);
        audio.toggleEnabled(); assertEquals(1, bike.plays);
        audio.playBicycle(); assertEquals(2, bike.plays);
        audio.startEasy(); assertFalse(bike.playing);
        audio.playBicycle(); audio.endEasy(); assertFalse(bike.playing);
    }
    @Test public void staleCompletionsCannotUndoNewFallClearOrRetry() {
        audio.startEasy(); audio.playFall(); Track fall = track("넌망한거야");
        Music.OnCompletionListener old = fall.listener;
        audio.playFall(); old.onCompletion(fall.music);
        assertEquals(.22f, track("easygameplay").volume, .001f);
        fall.complete(); assertEquals(.35f, track("easygameplay").volume, .001f);
        audio.playFall(); old = fall.listener; audio.startEasy(); old.onCompletion(fall.music);
        assertEquals(.35f, track("easygameplay").volume, .001f);
        audio.playStageClear(); old.onCompletion(fall.music);
        assertTrue(track("stageclear").playing); assertEquals(.22f, track("easygameplay").volume, .001f);
        old = track("stageclear").listener; audio.endEasy(); old.onCompletion(track("stageclear").music);
        assertFalse(track("easygameplay").playing); assertTrue(track("gamestart").playing);
    }
    @Test public void pauseAndResumePreserveMusicAndCuePositions() {
        audio.startEasy(); audio.playCoin(); audio.playFall();
        Track easy = track("easygameplay"), fall = track("넌망한거야"); easy.position = 20; fall.position = .4f;
        audio.pause(); assertTrue(audio.isPaused()); assertFalse(easy.playing); assertFalse(fall.playing);
        assertTrue(sound("coinsound").paused);
        audio.resume(); assertFalse(audio.isPaused()); assertTrue(easy.playing); assertTrue(fall.playing);
        assertEquals(20, easy.position, 0); assertEquals(.4f, fall.position, 0); assertFalse(sound("coinsound").paused);
    }
    @Test public void mutedFallFinishesSilentlyAndDoesNotReplayWhenUnmuting() {
        audio.startEasy(); audio.playFall(); audio.toggleEnabled();
        assertFalse(track("easygameplay").playing); assertEquals(0, track("넌망한거야").volume, 0);
        audio.playCoin(); assertEquals(0, sound("coinsound").plays);
        track("넌망한거야").complete(); audio.toggleEnabled();
        assertTrue(track("easygameplay").playing); assertEquals(.35f, track("easygameplay").volume, .001f);
        assertEquals(1, track("넌망한거야").plays);
    }
    @Test public void mutedClearCompletesAndKeepsTheReducedResultVolume() {
        audio.toggleEnabled(); audio.startEasy(); audio.playStageClear();
        assertFalse(track("easygameplay").playing); assertTrue(track("stageclear").playing);
        assertEquals(0, track("stageclear").volume, 0); track("stageclear").complete();
        audio.toggleEnabled(); assertTrue(track("easygameplay").playing);
        assertEquals(.22f, track("easygameplay").volume, .001f); assertEquals(1, track("stageclear").plays);
    }
    @Test public void dialogueStillMutesVoicesWithoutBreakingTheirCompletion() {
        audio.beginDialogue(); audio.playDialogueLine("intro-question.mp3"); Track question = track("intro-question");
        Music.OnCompletionListener old = question.listener;
        audio.playDialogueLine("intro-thanks.mp3"); old.onCompletion(question.music); assertFalse(audio.isDialogueVoiceFinished());
        audio.toggleEnabled(); assertEquals(0, track("intro-thanks").volume, 0);
        track("intro-thanks").complete(); assertTrue(audio.isDialogueVoiceFinished());
        audio.endDialogue(); assertFalse(track("gamestart").playing);
        audio.toggleEnabled(); assertTrue(track("gamestart").playing);
    }
    @Test public void leavingWhilePausedWaitsToResumeMenuAndDisposesAllSources() {
        audio.startEasy(); audio.playStageClear(); audio.pause(); audio.endEasy();
        assertFalse(track("gamestart").playing); assertFalse(track("stageclear").playing);
        audio.resume(); assertTrue(track("gamestart").playing);
        audio.dispose(); audio = null;
        for (Track track : music.values()) assertTrue(track.disposed);
        for (Track track : sounds.values()) assertTrue(track.disposed);
    }

    @Test public void postEasyDialogueStopsClearAudioAndKeepsSilentPagesQuiet() {
        audio.startEasy(); audio.playStageClear();
        Track clear = track("stageclear"); Music.OnCompletionListener stale = clear.listener;
        audio.endEasy(); audio.beginDialogue(); audio.playDialogueLine("normal-greeting.mp3");
        Track greeting = track("normal-greeting");
        assertFalse(clear.playing); assertFalse(track("easygameplay").playing);
        assertFalse(track("gamestart").playing); assertTrue(greeting.playing);
        stale.onCompletion(clear.music);
        assertFalse(track("gamestart").playing); assertFalse(audio.isDialogueVoiceFinished());
        audio.playDialogueLine(null);
        assertFalse(greeting.playing); assertTrue(audio.isDialogueVoiceFinished());
        assertFalse(track("gamestart").playing); assertFalse(track("easygameplay").playing);
        audio.endDialogue();
        assertTrue(track("gamestart").playing); assertFalse(track("easygameplay").playing);
    }

    @Test public void normalUsesItsOwnLoopAndSwitchingBackToEasyStopsIt() {
        audio.startNormal();
        Track normal = track("normalgameplay");
        assertTrue(normal.playing); assertTrue(normal.looping);
        assertFalse(track("easygameplay").playing); assertFalse(track("gamestart").playing);
        assertEquals(.35f, normal.volume, .001f);
        normal.position = 8; audio.pause(); assertFalse(normal.playing);
        audio.resume(); assertTrue(normal.playing); assertEquals(8, normal.position, 0);
        audio.startEasy(); assertFalse(normal.playing); assertTrue(track("easygameplay").playing);
    }

    @Test public void normalEffectsMutePauseAndStopWithTheirStage() {
        audio.startNormal(); audio.playKickboard(); audio.playMud(); audio.playSting(); audio.playDog();
        for (String name : new String[]{"kickboard", "진흙", "swoosh", "barking"})
            assertEquals(1, sound(name).plays);
        audio.pause(); assertTrue(sound("barking").paused);
        audio.resume(); assertFalse(sound("barking").paused);
        audio.toggleEnabled(); assertFalse(track("normalgameplay").playing);
        audio.playSting(); assertEquals(1, sound("swoosh").plays);
        audio.toggleEnabled(); audio.endStage();
        assertFalse(track("normalgameplay").playing); assertTrue(track("gamestart").playing);
        assertFalse(sound("barking").playing);
    }

    @Test public void normalDialogueHandsOffWithoutMenuMusicAndNormalMusicStartsAtTheFall() {
        int menuPlays=track("gamestart").plays;
        audio.beginDialogue();audio.playDialogueLine("normal-thanks.mp3");
        Track voice=track("normal-thanks");Music.OnCompletionListener stale=voice.listener;
        voice.complete();audio.endDialogueForStage();
        audio.pause();audio.resume();audio.toggleEnabled();audio.toggleEnabled();audio.playArrival();
        assertEquals(menuPlays,track("gamestart").plays);assertFalse(track("gamestart").playing);
        assertEquals(0,sound("fallingwhistle").plays);
        audio.prepareNormal();stale.onCompletion(voice.music);
        assertTrue(track("normalgameplay").playing);assertEquals(1,track("normalgameplay").plays);
        assertFalse(voice.playing);assertFalse(track("gamestart").playing);
        audio.playGo();audio.finishStageStart();
        assertEquals(1,track("normalgameplay").plays);
        assertEquals(1,sound("gooo").plays);assertTrue(sound("gooo").playing);
        audio.playKickboard();assertEquals(1,sound("kickboard").plays);
    }

    @Test public void restartingNormalRestartsMusicAtTheFallAndLeavingRestoresTheMenu() {
        audio.startNormal();audio.playDog();audio.prepareNormal();
        assertFalse(sound("barking").playing);assertTrue(track("normalgameplay").playing);
        assertEquals(2,track("normalgameplay").plays);
        audio.pause();audio.resume();assertTrue(track("normalgameplay").playing);
        audio.endStage();assertTrue(track("gamestart").playing);
        audio.finishStageStart();assertFalse(track("normalgameplay").playing);
        audio.startEasy();assertTrue(track("easygameplay").playing);
    }

    @Test public void normalFailureSuppressesItsMusicAndRetryStartsTheTrackAgain() {
        audio.startNormal(); audio.playFail();
        assertFalse(track("normalgameplay").playing); assertTrue(track("fail").playing);
        track("fail").complete(); audio.pause(); audio.resume();
        assertFalse(track("normalgameplay").playing); assertFalse(track("gamestart").playing);
        audio.startNormal(); assertTrue(track("normalgameplay").playing);
        assertEquals(0, track("normalgameplay").position, 0);
    }

    @Test public void goLayersOverTheExistingMusicAndFollowsPauseMuteAndRetry() {
        audio.startEasy(); Track background = track("easygameplay");
        background.position = 12; int starts = background.plays;
        audio.playGo();
        assertEquals(1, sound("gooo").plays); assertEquals(.4f, sound("gooo").volume, .001f);
        assertTrue(background.playing); assertEquals(starts, background.plays);
        assertEquals(12, background.position, 0); assertEquals(.35f, background.volume, .001f);
        audio.pause(); assertTrue(sound("gooo").paused); assertFalse(background.playing);
        audio.resume(); assertFalse(sound("gooo").paused); assertEquals(1, sound("gooo").plays);
        audio.toggleEnabled(); assertFalse(sound("gooo").playing);
        audio.playGo(); audio.toggleEnabled(); assertEquals(1, sound("gooo").plays);
        audio.startEasy(); assertFalse(sound("gooo").playing);
        audio.playGo(); assertEquals(2, sound("gooo").plays);
        audio.endStage(); assertFalse(sound("gooo").playing);
    }

    @Test public void goCannotInterruptDialoguePauseOrFailureAndMutedGoIsNotReplayed() {
        audio.prepareNormal(); audio.pause(); audio.playGo();
        assertEquals(0, sound("gooo").plays); assertFalse(track("normalgameplay").playing);
        audio.resume(); audio.toggleEnabled(); audio.playGo();
        assertEquals(0, sound("gooo").plays); assertFalse(track("normalgameplay").playing);
        audio.toggleEnabled(); assertTrue(track("normalgameplay").playing);
        assertEquals(0, sound("gooo").plays);
        audio.playFail(); audio.playGo(); assertEquals(0, sound("gooo").plays);
        assertTrue(track("fail").playing); assertFalse(track("normalgameplay").playing);
        audio.endStage(); audio.beginDialogue(); audio.playGo();
        assertEquals(0, sound("gooo").plays);
    }

    @Test public void hardUsesUploadedTrackAndAllNewEffectsFollowMutePauseRetryAndExit(){
        audio.beginDialogue();audio.playDialogueLine("hard-question.mp3");audio.endDialogueForStage();audio.prepareHard();
        assertTrue(track("hardgameplay").playing);assertTrue(track("hardgameplay").looping);
        assertEquals(0,track("normalgameplay").plays);assertFalse(track("gamestart").playing);
        audio.playGo();assertEquals(1,track("hardgameplay").plays);
        audio.playMotorcycle();audio.playWind();audio.playLaser();audio.playThunder();
        for(String n:new String[]{"motocycle","wind","laser","thunder"})assertEquals(1,sound(n).plays);
        audio.pause();assertFalse(track("hardgameplay").playing);audio.resume();audio.toggleEnabled();
        audio.playWind();audio.playLaser();audio.playThunder();audio.toggleEnabled();
        for(String n:new String[]{"motocycle","wind","laser","thunder"})assertEquals(1,sound(n).plays);
        audio.playFail();assertFalse(track("hardgameplay").playing);audio.prepareHard();assertTrue(track("hardgameplay").playing);
        assertEquals(0,track("hardgameplay").position,0);audio.endStage();assertTrue(track("gamestart").playing);
    }

    @Test public void extremeOwnsItsMusicAndOriginalBlueLaserPitchAcrossPauseRetryAndExit(){
        audio.prepareExtreme();assertTrue(track("extremegameplay").playing);assertTrue(track("extremegameplay").looping);
        assertEquals(0,track("hardgameplay").plays);assertEquals(0,track("normalgameplay").plays);
        audio.playBlueLaser();assertEquals(1,sound("laser").pitch,0);
        audio.pause();audio.playBlueLaser();assertEquals(1,sound("laser").plays);
        audio.resume();audio.playCoin();audio.playThunder();audio.prepareExtreme();
        assertFalse(sound("laser").playing);assertFalse(sound("thunder").playing);
        assertEquals(3,track("extremegameplay").plays);assertEquals(0,track("extremegameplay").position,0);
        audio.playStageClear();assertTrue(track("stageclear").playing);assertEquals(.22f,track("extremegameplay").volume,.001f);
        audio.prepareExtreme();audio.endStage();assertFalse(track("extremegameplay").playing);assertTrue(track("gamestart").playing);
        Track extreme=track("extremegameplay");audio.dispose();audio=null;assertTrue(extreme.disposed);
    }

    @Test public void extremeDialogueStopsEachVoiceAndHandsOffToItsStageWithoutMenuMusic(){
        audio.beginDialogue();assertFalse(track("gamestart").playing);
        Track previous=null;
        for(String voice:new String[]{"extreme-departure","extreme-night-ride","extreme-fun"}){
            audio.playDialogueLine(voice+".mp3");
            if(previous!=null)assertFalse(previous.playing);
            Track current=track(voice);assertTrue(current.playing);assertFalse(current.looping);
            assertFalse(audio.isDialogueVoiceFinished());current.complete();assertTrue(audio.isDialogueVoiceFinished());
            previous=current;
        }
        audio.endDialogueForStage();assertFalse(previous.playing);assertFalse(track("gamestart").playing);
        audio.prepareExtreme();assertTrue(track("extremegameplay").playing);assertFalse(track("gamestart").playing);
    }

    @Test public void bicycleRideLoopsOnceBesideUnchangedAcquisitionCueAndMusic() {
        audio.startEasy(); audio.playBicycle();
        Track bgm = track("easygameplay"); bgm.position = 12;
        int musicStarts = bgm.plays;
        for (int i = 0; i < 120; i++) audio.syncRiding(true, false, true);
        Track ride = sound("bicycle");
        assertTrue(ride.playing); assertTrue(ride.looping); assertEquals(1, ride.loops);
        assertEquals(.4f, ride.volume, .001f);
        assertEquals(1, sound("bicyclesound").plays); assertEquals(.85f, sound("bicyclesound").volume, .001f);
        assertEquals(musicStarts, bgm.plays); assertEquals(12, bgm.position, 0); assertEquals(.35f, bgm.volume, .001f);
        audio.syncRiding(false, false, true); assertFalse(ride.playing);
        assertTrue(sound("bicyclesound").playing);
        audio.syncRiding(true, false, true); assertEquals(2, ride.loops);
    }

    @Test public void bicycleIsSilentAtRestAndResumesOnlyWhileMovingWithoutRestarting() {
        audio.startEasy(); audio.playBicycle(); audio.syncRiding(true, false, false);
        Track ride = sound("bicycle");
        assertFalse(ride.playing); assertEquals(0, ride.loops);
        audio.syncRiding(true, false, true);
        assertTrue(ride.playing); assertEquals(1, ride.loops); ride.position = 2;
        audio.syncRiding(true, false, false);
        assertFalse(ride.playing); assertTrue(ride.paused);
        audio.pause(); audio.resume(); audio.toggleEnabled(); audio.toggleEnabled();
        assertFalse(ride.playing); assertEquals(1, ride.loops);
        audio.syncRiding(true, false, true);
        assertTrue(ride.playing); assertEquals(1, ride.loops); assertEquals(2, ride.position, 0);
        assertEquals(1, sound("bicyclesound").plays);
        audio.syncRiding(true, false, false); audio.syncRiding(false, false, false); audio.resume();
        assertFalse(ride.playing); assertFalse(ride.paused);
    }

    @Test public void scooterAndMotorcycleShareQuietEngineWithoutReplacingTheirMountCues() {
        audio.prepareHard(); audio.syncRiding(true, false, true);
        audio.playKickboard(); audio.syncRiding(true, true, false);
        assertFalse(sound("bicycle").playing);
        Track engine = sound("vroomsound");
        assertTrue(engine.playing); assertEquals(.03f, engine.volume, .001f);
        audio.playMotorcycle(); audio.syncRiding(true, true, false);
        assertEquals(1, engine.loops);
        assertEquals(1, sound("kickboard").plays); assertEquals(.7f, sound("kickboard").volume, .001f);
        assertEquals(1, sound("motocycle").plays); assertEquals(.7f, sound("motocycle").volume, .001f);
        audio.syncRiding(false, true, false); assertFalse(engine.playing);
    }

    @Test public void ridePausesAndResumesItsExistingInstanceWithoutRestarting() {
        audio.startEasy(); audio.syncRiding(true, false, true);
        Track ride = sound("bicycle"); ride.position = 2;
        audio.pause(); assertFalse(ride.playing); assertTrue(ride.paused);
        audio.syncRiding(true, false, true); audio.resume();
        assertTrue(ride.playing); assertFalse(ride.paused);
        assertEquals(1, ride.loops); assertEquals(2, ride.position, 0);
        audio.toggleEnabled(); assertFalse(ride.playing);
        audio.toggleEnabled(); assertTrue(ride.playing); assertEquals(1, ride.loops);
    }

    @Test public void mountingOrDismountingWhileMutedCannotResumeAStaleRide() {
        audio.prepareNormal(); audio.toggleEnabled(); audio.syncRiding(true, true, false);
        Track engine = sound("vroomsound"); assertEquals(0, engine.loops);
        audio.toggleEnabled(); assertTrue(engine.playing); assertEquals(1, engine.loops);
        audio.toggleEnabled(); audio.syncRiding(false, true, false); audio.toggleEnabled();
        assertFalse(engine.playing); assertEquals(1, engine.loops);
        audio.syncRiding(true, true, false); audio.pause(); audio.syncRiding(false, true, false); audio.resume();
        assertFalse(engine.playing); assertEquals(2, engine.loops);
    }

    @Test public void retryExitClearFailAndDialogueEndTheRideAndDoNotResumeIt() {
        audio.startEasy(); audio.syncRiding(true, false, true); Track ride = sound("bicycle");
        audio.startEasy(); audio.resume(); assertFalse(ride.playing);
        audio.syncRiding(true, false, true); audio.endStage(); audio.resume(); assertFalse(ride.playing);
        audio.startEasy(); audio.syncRiding(true, false, true); audio.playStageClear();
        audio.syncRiding(true, false, true); audio.resume(); assertFalse(ride.playing);
        audio.startEasy(); audio.syncRiding(true, false, true); audio.playFail();
        audio.syncRiding(true, false, true); audio.resume(); assertFalse(ride.playing);
        audio.startEasy(); audio.syncRiding(true, false, true); audio.beginDialogue();
        audio.syncRiding(true, false, true); audio.endDialogue(); assertFalse(ride.playing);
        audio.syncRiding(true, false, true); audio.dispose(); audio = null;
        assertFalse(ride.playing); assertTrue(ride.disposed); assertTrue(sound("vroomsound").disposed);
    }

    @Test public void stoppedThunderCannotReturnAfterPauseOrMute() {
        audio.prepareHard(); audio.playThunder(); Track thunder = sound("thunder");
        assertTrue(thunder.playing); audio.stopThunder();
        audio.pause(); audio.resume(); audio.toggleEnabled(); audio.toggleEnabled();
        assertFalse(thunder.playing); assertEquals(1, thunder.plays);
        audio.playThunder(); assertTrue(thunder.playing); assertEquals(2, thunder.plays);
    }

    private static final class Track implements InvocationHandler {
        Music music; Music.OnCompletionListener listener;
        boolean playing, paused, looping, disposed; float volume, position,pitch=1; int plays, loops;
        void complete() { playing = false; listener.onCompletion(music); }
        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "play":
                    plays++; playing = true; paused = false;
                    if (args != null && args.length > 0) volume = (Float) args[0];
                    if (args != null && args.length > 1) pitch = (Float) args[1];
                    return method.getReturnType() == long.class ? 1L : null;
                case "loop":
                    loops++; plays++; playing = true; paused = false; looping = true;
                    volume = (Float) args[0]; return 1L;
                case "pause": paused |= playing; playing = false; return null;
                case "resume": playing |= paused; paused = false; return null;
                case "stop": playing = false; paused = false; position = 0; return null;
                case "setVolume": volume = (Float) args[args.length - 1]; return null;
                case "getVolume": return volume;
                case "isPlaying": return playing;
                case "setLooping": looping = (Boolean) args[args.length - 1]; return null;
                case "isLooping": return looping;
                case "setPosition": position = (Float) args[0]; return null;
                case "getPosition": return position;
                case "setOnCompletionListener": listener = (Music.OnCompletionListener) args[0]; return null;
                case "dispose": disposed = true; playing = false; return null;
                default: return empty(method.getReturnType());
            }
        }
    }
}
