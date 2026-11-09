package sangdong_lakepark.stages;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class StageProgressTest {
    private Application previousApp;
    private final Map<String, Integer> saved = new HashMap<>();

    @Before public void setUp() {
        previousApp = Gdx.app;
        Preferences preferences = (Preferences) Proxy.newProxyInstance(Preferences.class.getClassLoader(),
            new Class<?>[] {Preferences.class}, (proxy, method, args) -> {
                if (method.getName().equals("getInteger"))
                    return saved.getOrDefault(args[0], args.length > 1 ? (Integer) args[1] : 0);
                if (method.getName().equals("putInteger")) {
                    saved.put((String) args[0], (Integer) args[1]);
                    return proxy;
                }
                return null;
            });
        Gdx.app = (Application) Proxy.newProxyInstance(Application.class.getClassLoader(),
            new Class<?>[] {Application.class}, (proxy, method, args) ->
                method.getName().equals("getPreferences") ? preferences : null);
    }

    @After public void tearDown() { Gdx.app = previousApp; }

    @Test public void firstClearQueuesOneCertificateAndUnlocksOnlyTheNextStage() {
        StageProgress progress = new StageProgress();
        assertNull(progress.pendingCertificate());
        assertFalse(progress.complete(StageId.NORMAL));
        assertTrue(progress.complete(StageId.EASY));
        assertEquals(StageId.EASY, progress.pendingCertificate());
        assertTrue(progress.isUnlocked(StageId.NORMAL));
        assertFalse(progress.isUnlocked(StageId.HARD));
        assertFalse(progress.complete(StageId.EASY));
        assertEquals(StageId.EASY, progress.pendingCertificate());
        progress.acknowledgeCertificate(StageId.EASY);
        assertNull(progress.pendingCertificate());
        assertFalse(progress.complete(StageId.EASY));
        assertNull(progress.pendingCertificate());
        assertEquals(StageId.NORMAL, progress.nextChallenge());
    }

    @Test public void easyNormalAndHardQueueTheirOwnCertificatesWithoutAnExtremeReveal() {
        StageProgress progress = new StageProgress();
        for (StageId stage : new StageId[] {StageId.EASY, StageId.NORMAL, StageId.HARD}) {
            assertTrue(progress.complete(stage));
            assertEquals(stage, progress.pendingCertificate());
            progress.acknowledgeCertificate(stage);
            assertNull(progress.pendingCertificate());
        }
        assertTrue(progress.complete(StageId.EXTREME));
        assertNull(progress.pendingCertificate());
        assertNull(progress.nextChallenge());
    }

    @Test public void unfinishedRevealsSurviveReloadAndCompletedRevealsStayAcknowledged() {
        StageProgress progress = new StageProgress();
        progress.complete(StageId.EASY);
        progress = new StageProgress();
        assertTrue(progress.isUnlocked(StageId.NORMAL));
        assertEquals(StageId.EASY, progress.pendingCertificate());
        progress.acknowledgeCertificate(StageId.EASY);
        progress = new StageProgress();
        assertNull(progress.pendingCertificate());
        assertFalse(progress.complete(StageId.EASY));
        assertNull(progress.pendingCertificate());
    }

    @Test public void laterClearsDoNotOverwriteUnseenCertificates() {
        StageProgress progress = new StageProgress();
        progress.complete(StageId.EASY);
        progress.complete(StageId.NORMAL);
        progress.complete(StageId.HARD);
        progress = new StageProgress();
        for (StageId stage : new StageId[] {StageId.EASY, StageId.NORMAL, StageId.HARD}) {
            assertEquals(stage, progress.pendingCertificate());
            progress.acknowledgeCertificate(stage);
        }
        assertNull(new StageProgress().pendingCertificate());
    }

    @Test public void existingClearRecordsDoNotTriggerRetroactiveOrReplayCertificates() {
        saved.put("clearedCount", 3);
        StageProgress progress = new StageProgress();
        assertNull(progress.pendingCertificate());
        for (StageId stage : new StageId[] {StageId.EASY, StageId.NORMAL, StageId.HARD})
            assertFalse(progress.complete(stage));
        assertNull(progress.pendingCertificate());
        assertTrue(progress.isUnlocked(StageId.EXTREME));
    }

    @Test public void invalidPendingBitsCannotRevealUnclearedOrUnsupportedCertificates() {
        saved.put("clearedCount", 1);
        saved.put("pendingCertificates", 15);
        StageProgress progress = new StageProgress();
        assertEquals(StageId.EASY, progress.pendingCertificate());
        progress.acknowledgeCertificate(StageId.EASY);
        assertNull(progress.pendingCertificate());
        assertNull(new StageProgress().pendingCertificate());
    }

    @Test public void resetClearsAllUnlocksAndPendingCertificatesAndStartsANewCampaign() {
        StageProgress progress = new StageProgress();
        for (StageId stage : StageId.values()) progress.complete(stage);
        assertNull(progress.nextChallenge());
        assertNotNull(progress.pendingCertificate());
        progress.reset();
        for (StageId stage : StageId.values()) assertEquals(stage == StageId.EASY, progress.isUnlocked(stage));
        assertNull(progress.pendingCertificate());
        StageProgress reloaded = new StageProgress();
        assertEquals(StageId.EASY, reloaded.nextChallenge());
        assertFalse(reloaded.isUnlocked(StageId.NORMAL));
        assertNull(reloaded.pendingCertificate());
        assertTrue(reloaded.complete(StageId.EASY));
        assertEquals(StageId.EASY, reloaded.pendingCertificate());
    }

    @Test public void resetPreservesUnrelatedSavedSettings() {
        saved.put("otherSetting", 42);
        StageProgress progress = new StageProgress();
        progress.complete(StageId.EASY);
        progress.reset();
        assertEquals(Integer.valueOf(42), saved.get("otherSetting"));
        assertNull(new StageProgress().pendingCertificate());
    }
}
