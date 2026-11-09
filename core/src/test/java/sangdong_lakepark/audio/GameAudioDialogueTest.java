package sangdong_lakepark.audio;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** Exercises the audio lifecycle without an audio device or a graphics context. */
public class GameAudioDialogueTest {
    private final Application oldApp = Gdx.app;
    private final Audio oldAudio = Gdx.audio;
    private final Files oldFiles = Gdx.files;
    private final Map<String, Voice> voices = new HashMap<>();
    private GameAudio audio;

    @Before public void setup() {
        Preferences preferences = proxy(Preferences.class, (object, method, args) -> {
            if (method.getName().equals("getBoolean")) return args[1];
            return method.getReturnType() == Preferences.class ? object : null;
        });
        Gdx.app = proxy(Application.class, (object, method, args) ->
            method.getName().equals("getPreferences") ? preferences : null);
        Gdx.files = proxy(Files.class, (object, method, args) -> new FileHandle((String) args[0]));
        Sound click = proxy(Sound.class, (object, method, args) -> null);
        Gdx.audio = proxy(Audio.class, (object, method, args) -> {
            if (method.getName().equals("newSound")) return click;
            Voice voice = new Voice();
            voices.put(((FileHandle) args[0]).name(), voice);
            return voice.music;
        });
        audio = new GameAudio();
    }

    @After public void teardown() {
        if (audio != null) audio.dispose();
        Gdx.app = oldApp;
        Gdx.audio = oldAudio;
        Gdx.files = oldFiles;
    }

    @Test public void navigatingStopsTheOldVoiceAndIgnoresItsQueuedCompletion() {
        audio.beginDialogue();
        audio.playDialogueLine("normal-greeting.mp3");
        Voice greeting = voices.get("normal-greeting.mp3");
        Music.OnCompletionListener stale = greeting.completion;
        audio.playDialogueLine(null);
        assertFalse(greeting.playing);
        assertTrue(audio.isDialogueVoiceFinished());
        audio.playDialogueLine("normal-distance.mp3");
        Voice distance = voices.get("normal-distance.mp3");
        stale.onCompletion(greeting.music);
        assertFalse(audio.isDialogueVoiceFinished());
        distance.complete();
        assertTrue(audio.isDialogueVoiceFinished());
    }

    @Test public void returningToAVoicedPageReplaysItAndRejectsTheOldCompletion() {
        audio.beginDialogue();
        audio.playDialogueLine("normal-greeting.mp3");
        Voice greeting = voices.get("normal-greeting.mp3");
        Music.OnCompletionListener stale = greeting.completion;
        audio.playDialogueLine(null);
        audio.playDialogueLine("normal-greeting.mp3");
        assertSame(greeting, voices.get("normal-greeting.mp3"));
        assertEquals(2, greeting.plays);
        stale.onCompletion(greeting.music);
        assertFalse(audio.isDialogueVoiceFinished());
        greeting.complete();
        assertTrue(audio.isDialogueVoiceFinished());
    }

    @Test public void mutedVoicesStillPlayAndReportCompletionForAutomaticAdvances() {
        audio.toggleEnabled();
        audio.beginDialogue();
        audio.playDialogueLine("normal-thanks.mp3");
        Voice thanks = voices.get("normal-thanks.mp3");
        assertTrue(thanks.playing);
        assertEquals(0, thanks.volume, 0);
        thanks.complete();
        assertTrue(audio.isDialogueVoiceFinished());
    }

    @Test public void pauseResumesUnfinishedVoicesButDoesNotReplayFinishedOnes() {
        audio.beginDialogue();
        audio.playDialogueLine("intro-question.mp3");
        Voice question = voices.get("intro-question.mp3");
        audio.pause();
        assertFalse(question.playing);
        audio.resume();
        assertTrue(question.playing);
        assertEquals(2, question.plays);
        question.complete();
        audio.pause();
        audio.resume();
        assertEquals(2, question.plays);
        assertTrue(audio.isDialogueVoiceFinished());
    }

    @Test public void exitingStopsTheVoiceAndRestoresBackgroundMusic() {
        audio.beginDialogue();
        Voice background = voices.get("gamestart.mp3");
        assertFalse(background.playing);
        audio.playDialogueLine("normal-greeting.mp3");
        Voice greeting = voices.get("normal-greeting.mp3");
        audio.endDialogue();
        assertFalse(greeting.playing);
        assertTrue(background.playing);
        assertFalse(audio.isDialogueVoiceFinished());
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler));
    }

    private static final class Voice implements InvocationHandler {
        final Music music = proxy(Music.class, this);
        Music.OnCompletionListener completion;
        boolean playing;
        int plays;
        float volume;

        void complete() {
            playing = false;
            completion.onCompletion(music);
        }

        @Override public Object invoke(Object object, java.lang.reflect.Method method, Object[] args) {
            switch (method.getName()) {
                case "play": playing = true; plays++; break;
                case "pause":
                case "stop": playing = false; break;
                case "isPlaying": return playing;
                case "setVolume": volume = (Float) args[0]; break;
                case "setOnCompletionListener": completion = (Music.OnCompletionListener) args[0]; break;
            }
            return null;
        }
    }
}
