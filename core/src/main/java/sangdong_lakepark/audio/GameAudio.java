package sangdong_lakepark.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/** Owns menu/Easy music, gameplay cues and dialogue with one saved mute setting. */
public final class GameAudio implements Disposable {
    private static final float MUSIC_VOLUME = .35f;
    private static final float CLICK_VOLUME = .7f;
    private static final float VOICE_VOLUME = .85f;
    private static final float EASY_DUCK_VOLUME = .22f, COIN_VOLUME = .7f, CUE_VOLUME = .85f;
    private static final float FAIL_VOLUME = .55f, WINDOW_VOLUME = .65f, JUMP_VOLUME = .3f;
    private static final float DAMAGE_VOLUME = .4f, BUMP_VOLUME = .4f, PICKUP_VOLUME = .24f;
    private static final float ARRIVAL_VOLUME = .4f, FALLING_ROCK_VOLUME = .4f, ROLLING_ROCK_VOLUME = .35f;
    private static final float WATER_STREAM_VOLUME = .55f, WATER_ENTRY_VOLUME = .35f;
    private static final float GO_VOLUME = .4f;
    // Keep ride recordings quieter than the soundtrack.
    private static final float BICYCLE_RIDE_VOLUME = .4f, MOTOR_RIDE_VOLUME = .03f;
    private final Preferences preferences = Gdx.app.getPreferences("sangdong-lake-park");
    private final Music background;
    private final Sound click, window, arrival, go;
    private final ObjectMap<String, Music> dialogue = new ObjectMap<>();
    private Music currentVoice;
    private Music easyMusic, normalMusic, hardMusic, extremeMusic, activeStageMusic, clearCue, fallCue, failCue, currentCue;
    private Sound coin, bicycle, jump, damage, bump, pickup;
    private Sound fallingRock, rollingRock, waterStream, waterEntry;
    private Sound scooter, mud, sting, dog, motorcycle, wind, laser, thunder;
    private Sound bicycleRide, motorRide, ridingLoop;
    private long ridingId = -1;
    private boolean ridingPaused, ridingActive;
    private Sound[] easyEffects = new Sound[0];
    private long voiceGeneration;
    private long cueGeneration;
    private boolean inDialogue, voiceFinished;
    private boolean inEasy, easyCleared, easyFailed, waitingForStageStart;
    private boolean enabled = preferences.getBoolean("soundEnabled", true);
    private boolean paused;

    public GameAudio() {
        background = Gdx.audio.newMusic(Gdx.files.classpath("audio/gamestart.mp3"));
        background.setLooping(true);
        background.setVolume(MUSIC_VOLUME);
        click = Gdx.audio.newSound(Gdx.files.classpath("audio/click.mp3"));
        window = Gdx.audio.newSound(Gdx.files.classpath("audio/opensound.wav"));
        arrival = Gdx.audio.newSound(Gdx.files.classpath("audio/fallingwhistle.mp3"));
        go = Gdx.audio.newSound(Gdx.files.classpath("audio/gooo.mp3"));
        if (enabled) background.play();
        Gdx.app.log("GameAudio", "Loaded audio/gamestart.mp3 and audio/click.mp3 (sound "
            + (enabled ? "ON" : "OFF") + ")");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isDialogueVoiceFinished() {
        return inDialogue && voiceFinished;
    }

    public void toggleEnabled() {
        enabled = !enabled;
        preferences.putBoolean("soundEnabled", enabled).flush();
        if (!enabled) {
            background.pause();
            if (activeStageMusic != null) activeStageMusic.pause();
            click.stop();
            window.stop(); arrival.stop(); go.stop(); stopEffects();
        }
        resumeCurrentAudio();
    }

    public void playClick() {
        if (enabled && !paused && !inDialogue && !waitingForStageStart && !easyFailed) {
            click.stop();
            click.play(CLICK_VOLUME);
        }
    }

    /** Menu sounds remain available while the menu pauses the gameplay soundtrack. */
    public void playWindow() {
        playInterfaceSound(false);
    }

    /** Explicit keyboard navigation remains audible on the Fail screen without restarting music. */
    public void playSelection() { playInterfaceSound(true); }

    public void playArrival() {
        if (enabled && !paused && !inDialogue && !waitingForStageStart && !easyFailed) {
            arrival.stop(); arrival.play(ARRIVAL_VOLUME);
        }
    }
    public void stopArrival() { arrival.stop(); }

    /** Go is a sound effect layered over music, without ducking or replacing its soundtrack. */
    public void playGo() {
        if (paused || inDialogue || easyFailed || easyCleared) return;
        finishStageStart();
        if (enabled) { go.stop(); go.play(GO_VOLUME); }
    }
    public void stopGo() { go.stop(); }

    private void playInterfaceSound(boolean selection) {
        if (enabled && !inDialogue && (selection || !easyFailed)) {
            click.stop(); window.stop(); window.play(WINDOW_VOLUME);
        }
    }

    /** A new attempt restarts its soundtrack and clears any cue from the previous attempt. */
    public void startEasy() { loadEasy(); startStage(easyMusic, false); }

    public void startNormal() { startNormal(false); }

    private void startNormal(boolean waitForStart) {
        loadNormal();startStage(normalMusic, waitForStart);
    }
    private void loadNormal() {
        loadEasy();
        if (normalMusic == null) {
            normalMusic = Gdx.audio.newMusic(Gdx.files.classpath("audio/normalgameplay.mp3"));
            normalMusic.setLooping(true);
            scooter = Gdx.audio.newSound(Gdx.files.classpath("audio/kickboard.mp3"));
            mud = Gdx.audio.newSound(Gdx.files.classpath("audio/진흙.mp3"));
            sting = Gdx.audio.newSound(Gdx.files.classpath("audio/swoosh.mp3"));
            dog = Gdx.audio.newSound(Gdx.files.classpath("audio/barking.mp3"));
            Sound[] effects = new Sound[easyEffects.length + 4];
            System.arraycopy(easyEffects, 0, effects, 0, easyEffects.length);
            effects[effects.length-4]=scooter;effects[effects.length-3]=mud;
            effects[effects.length-2]=sting;effects[effects.length-1]=dog;easyEffects=effects;
        }
    }

    /** Normal music starts with the initial fall and continues through READY/Go. */
    public void prepareNormal() { startNormal(false); }

    public void prepareHard() {
        loadHard();startStage(hardMusic,false);
    }
    public void prepareExtreme(){
        loadHard();
        if(extremeMusic==null){extremeMusic=Gdx.audio.newMusic(Gdx.files.classpath("audio/extremegameplay.mp3"));extremeMusic.setLooping(true);}
        startStage(extremeMusic,false);
    }
    private void loadHard(){
        loadNormal();
        if (hardMusic == null) {
            hardMusic=Gdx.audio.newMusic(Gdx.files.classpath("audio/hardgameplay.mp3"));hardMusic.setLooping(true);
            motorcycle=Gdx.audio.newSound(Gdx.files.classpath("audio/motocycle.mp3"));
            wind=Gdx.audio.newSound(Gdx.files.classpath("audio/wind.mp3"));
            laser=Gdx.audio.newSound(Gdx.files.classpath("audio/laser.mp3"));
            thunder=Gdx.audio.newSound(Gdx.files.classpath("audio/thunder.mp3"));
            Sound[] effects=new Sound[easyEffects.length+4];
            System.arraycopy(easyEffects,0,effects,0,easyEffects.length);
            effects[effects.length-4]=motorcycle;effects[effects.length-3]=wind;
            effects[effects.length-2]=laser;effects[effects.length-1]=thunder;easyEffects=effects;
        }
    }
    public void playMotorcycle(){playEffect(motorcycle,.7f);}
    public void playWind(){playEffect(wind,.5f);}
    public void playLaser(){playEffect(laser,.5f);}
    public void playBlueLaser(){playEffect(laser,.5f);}
    public void playThunder(){playEffect(thunder,.55f);}

    public void finishStageStart() {
        if (!inEasy || !waitingForStageStart) return;
        waitingForStageStart = false;
        resumeCurrentAudio();
    }

    private void startStage(Music soundtrack, boolean waitForStart) {
        stopRiding();
        stopCue(); stopVoice(); inDialogue = false;
        background.pause(); stopEffects(); window.stop(); arrival.stop(); go.stop();
        if (activeStageMusic != null) activeStageMusic.stop();
        activeStageMusic = soundtrack; activeStageMusic.stop();
        inEasy = true; easyCleared = easyFailed = false;
        waitingForStageStart = waitForStart;
        activeStageMusic.setVolume(MUSIC_VOLUME); resumeCurrentAudio();
    }

    public void endEasy() { endStage(); }
    public void endStage() {
        go.stop(); stopRiding();
        if (!inEasy) return;
        stopCue(); stopEffects(); window.stop(); arrival.stop(); activeStageMusic.stop();
        inEasy = easyCleared = easyFailed = waitingForStageStart = false; resumeCurrentAudio();
    }

    public void playKickboard() { playEffect(scooter, .7f); }
    public void playMud() { playEffect(mud, .38f); }
    public void playSting() { playEffect(sting, .42f); }
    public void playDog() { playEffect(dog, .55f); }

    public void playCoin() {
        if (canPlayEffect()) coin.play(COIN_VOLUME);
    }

    public void playBicycle() {
        if (canPlayEffect()) {
            bicycle.stop(); bicycle.play(CUE_VOLUME);
        }
    }

    /** Bicycles sound only during input-driven movement; engines keep idling while mounted. */
    public void syncRiding(boolean riding, boolean motorized, boolean moving) {
        Sound wanted = riding && inEasy && !easyCleared && !easyFailed && !inDialogue
            ? motorized ? motorRide : bicycleRide : null;
        if (wanted != ridingLoop) {
            stopRiding(); ridingLoop = wanted;
        }
        ridingActive = motorized || moving;
        syncRidingPlayback();
    }

    private void syncRidingPlayback() {
        if (ridingLoop == null) return;
        if (!ridingActive || !canPlayEffect()) {
            if (ridingId >= 0 && !ridingPaused) {
                ridingLoop.pause(ridingId); ridingPaused = true;
            }
        } else if (ridingId < 0) {
            ridingId = ridingLoop.loop(ridingLoop == bicycleRide ? BICYCLE_RIDE_VOLUME : MOTOR_RIDE_VOLUME);
            ridingPaused = false;
        } else if (ridingPaused) {
            ridingLoop.resume(ridingId); ridingPaused = false;
        }
    }

    private void stopRiding() {
        if (ridingLoop != null && ridingId >= 0) ridingLoop.stop(ridingId);
        ridingLoop = null; ridingId = -1; ridingPaused = ridingActive = false;
    }

    public void stopThunder() { if (thunder != null) thunder.stop(); }

    public void playJump() { playEffect(jump, JUMP_VOLUME); }
    public void playDamage() { playEffect(damage, DAMAGE_VOLUME); }
    public void playBump() { playEffect(bump, BUMP_VOLUME); }
    public void playPickup() { playEffect(pickup, PICKUP_VOLUME); }
    public void playFallingRock() { playEffect(fallingRock, FALLING_ROCK_VOLUME); }
    public void playRollingRock() { playEffect(rollingRock, ROLLING_ROCK_VOLUME); }
    public void playWaterStream() { playEffect(waterStream, WATER_STREAM_VOLUME); }
    public void stopWaterStream() { if (waterStream != null) waterStream.stop(); }
    public void playWaterEntry() { playEffect(waterEntry, WATER_ENTRY_VOLUME); }

    private boolean canPlayEffect() { return inEasy && !waitingForStageStart && !easyCleared && !easyFailed && enabled && !paused && !inDialogue; }
    private void playEffect(Sound effect, float volume) {
        if (canPlayEffect()) { effect.stop(); effect.play(volume); }
    }
    private void stopEffects() { for (Sound effect : easyEffects) effect.stop(); }

    /** Failure owns the audio until retry or leaving: old cues can never restart a soundtrack. */
    public void playFail() {
        if (!inEasy || easyFailed || easyCleared) return;
        stopRiding();
        easyFailed = true;
        background.stop(); activeStageMusic.stop(); stopVoice(); inDialogue = false;
        stopEffects(); click.stop(); window.stop(); arrival.stop(); go.stop();
        playCue(failCue);
    }

    public void playStageClear() {
        if (!inEasy || easyCleared || easyFailed) return;
        stopRiding();
        easyCleared = true; go.stop(); playCue(clearCue);
    }

    public void playFall() {
        if (inEasy && !easyCleared && !easyFailed) playCue(fallCue);
    }

    private void loadEasy() {
        if (easyMusic != null) return;
        easyMusic = Gdx.audio.newMusic(Gdx.files.classpath("audio/easygameplay.mp3"));
        easyMusic.setLooping(true);
        clearCue = Gdx.audio.newMusic(Gdx.files.classpath("audio/stageclear.mp3"));
        fallCue = Gdx.audio.newMusic(Gdx.files.classpath("audio/넌망한거야.mp3"));
        failCue = Gdx.audio.newMusic(Gdx.files.classpath("audio/fail.mp3"));
        clearCue.setLooping(false); fallCue.setLooping(false); failCue.setLooping(false);
        coin = Gdx.audio.newSound(Gdx.files.classpath("audio/coinsound.mp3"));
        bicycle = Gdx.audio.newSound(Gdx.files.classpath("audio/bicyclesound.mp3"));
        bicycleRide = Gdx.audio.newSound(Gdx.files.classpath("audio/bicycle.mp3"));
        motorRide = Gdx.audio.newSound(Gdx.files.classpath("audio/vroomsound.mp3"));
        jump = Gdx.audio.newSound(Gdx.files.classpath("audio/jump.mp3"));
        damage = Gdx.audio.newSound(Gdx.files.classpath("audio/damage.wav"));
        bump = Gdx.audio.newSound(Gdx.files.classpath("audio/bump.mp3"));
        pickup = Gdx.audio.newSound(Gdx.files.classpath("audio/pickup.wav"));
        fallingRock = Gdx.audio.newSound(Gdx.files.classpath("audio/fallingrock.mp3"));
        rollingRock = Gdx.audio.newSound(Gdx.files.classpath("audio/rollingrock.mp3"));
        waterStream = Gdx.audio.newSound(Gdx.files.classpath("audio/splash.mp3"));
        waterEntry = Gdx.audio.newSound(Gdx.files.classpath("audio/watersplash.mp3"));
        easyEffects = new Sound[]{coin, bicycle, jump, damage, bump, pickup, fallingRock, rollingRock, waterStream, waterEntry};
        Gdx.app.log("GameAudio", "Loaded Easy soundtrack and gameplay effects");
    }

    private void playCue(Music cue) {
        stopCue(); currentCue = cue;
        final long generation = cueGeneration;
        cue.setOnCompletionListener(completed -> {
            if (generation != cueGeneration || currentCue != completed) return;
            currentCue = null; updateEasyVolume();
        });
        updateEasyVolume(); resumeCurrentAudio();
    }

    private void stopCue() {
        cueGeneration++;
        if (currentCue != null) currentCue.stop();
        currentCue = null;
    }

    private void updateEasyVolume() {
        if (activeStageMusic != null) activeStageMusic.setVolume(easyCleared || currentCue != null ? EASY_DUCK_VOLUME : MUSIC_VOLUME);
    }

    public void beginDialogue() {
        stopRiding();
        inDialogue = true;
        background.pause();
        if (activeStageMusic != null) activeStageMusic.pause();
        stopCue();
        updateEasyVolume();
        stopEffects(); window.stop(); arrival.stop(); go.stop();
        click.stop();
        stopVoice();
    }

    public void playDialogueLine(String voiceFile) {
        stopVoice();
        if (voiceFile != null) {
            currentVoice = dialogue.get(voiceFile);
            if (currentVoice == null) {
                currentVoice = Gdx.audio.newMusic(Gdx.files.classpath("audio/" + voiceFile));
                currentVoice.setLooping(false);
                dialogue.put(voiceFile, currentVoice);
            }
        }
        voiceFinished = currentVoice == null;
        if (currentVoice != null) {
            final long generation = voiceGeneration;
            currentVoice.setOnCompletionListener(completed -> {
                // A completion queued before rewinding must not finish a newly restarted voice.
                if (generation == voiceGeneration && currentVoice == completed) voiceFinished = true;
            });
        }
        resumeCurrentAudio();
        Gdx.app.log("GameAudio", "Dialogue voice="
            + (voiceFile == null ? "none" : voiceFile) + " (sound " + (enabled ? "ON" : "OFF") + ")");
    }

    public void endDialogue() {
        if (!inDialogue) return;
        stopVoice();
        inDialogue = false;
        resumeCurrentAudio();
    }

    /** Hand the voice directly to a silent stage intro, without resuming the map soundtrack. */
    public void endDialogueForStage() {
        stopVoice(); inDialogue = false; waitingForStageStart = true;
        background.pause();
        if (activeStageMusic != null) activeStageMusic.pause();
    }

    private void stopVoice() {
        voiceGeneration++;
        if (currentVoice != null) currentVoice.stop();
        currentVoice = null;
        voiceFinished = true;
    }

    private void resumeCurrentAudio() {
        syncRidingPlayback();
        if (paused) return;
        if (waitingForStageStart && !inDialogue) return;
        if (inDialogue) {
            // Play muted voices at zero volume so their completion timing still advances.
            if (currentVoice != null) currentVoice.setVolume(enabled ? VOICE_VOLUME : 0);
            if (currentVoice != null && !voiceFinished && !currentVoice.isPlaying()) currentVoice.play();
        } else {
            Music music = inEasy ? activeStageMusic : background;
            if (enabled && !easyFailed && !music.isPlaying()) music.play();
            if (currentCue != null) {
                // Muted cues still finish on time, so unmuting never replays an old event.
                currentCue.setVolume(enabled ? currentCue == failCue ? FAIL_VOLUME : CUE_VOLUME : 0);
                if (!currentCue.isPlaying()) currentCue.play();
            }
            if (enabled && !easyFailed) {
                for (Sound effect : easyEffects) effect.resume();
                window.resume();
                arrival.resume();
                go.resume();
            }
        }
    }

    public void pause() {
        paused = true;
        syncRidingPlayback();
        background.pause();
        if (activeStageMusic != null) activeStageMusic.pause();
        if (currentCue != null) currentCue.pause();
        for (Sound effect : easyEffects) effect.pause();
        window.pause();
        arrival.pause();
        go.pause();
        click.stop();
        if (currentVoice != null) currentVoice.pause();
    }

    public void resume() {
        paused = false;
        resumeCurrentAudio();
    }

    @Override
    public void dispose() {
        stopRiding();
        stopCue(); stopVoice();
        background.dispose();
        click.dispose();
        window.dispose();
        arrival.dispose();
        go.dispose();
        for (Music voice : dialogue.values()) voice.dispose();
        if (easyMusic != null) {
            bicycleRide.dispose(); motorRide.dispose();
            easyMusic.dispose(); if (normalMusic != null) normalMusic.dispose(); if(hardMusic!=null)hardMusic.dispose(); if(extremeMusic!=null)extremeMusic.dispose(); clearCue.dispose(); fallCue.dispose(); failCue.dispose();
            for (Sound effect : easyEffects) effect.dispose();
        }
    }
}
