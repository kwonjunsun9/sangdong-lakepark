package sangdong_lakepark.stages;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.MathUtils;

/** A contiguous clear record: Easy must be cleared before Normal, and so on. */
public final class StageProgress {
    private static final StageId[] STAGES = StageId.values();
    private final Preferences preferences;
    private int clearedCount;
    private int pendingCertificates;

    public StageProgress() {
        preferences = Gdx.app.getPreferences("sangdong-lake-park-progress");
        clearedCount = MathUtils.clamp(preferences.getInteger("clearedCount", 0), 0, STAGES.length);
        pendingCertificates = preferences.getInteger("pendingCertificates", 0)
            & ((1 << clearedCount) - 1) & ((1 << StageId.EXTREME.ordinal()) - 1);
    }

    public boolean isUnlocked(StageId stage) {
        return stage.ordinal() <= clearedCount;
    }

    /** Returns true only when this clear advances the saved route. Replays never skip a stage. */
    public boolean complete(StageId stage) {
        if (stage.ordinal() != clearedCount) return false;
        clearedCount++;
        if (stage != StageId.EXTREME) pendingCertificates |= 1 << stage.ordinal();
        preferences.putInteger("clearedCount", clearedCount)
            .putInteger("pendingCertificates", pendingCertificates).flush();
        return true;
    }

    /** A first-clear certificate stays queued until its map reveal finishes. */
    public StageId pendingCertificate() {
        for (StageId stage : STAGES)
            if ((pendingCertificates & (1 << stage.ordinal())) != 0) return stage;
        return null;
    }

    public void acknowledgeCertificate(StageId stage) {
        pendingCertificates &= ~(1 << stage.ordinal());
        preferences.putInteger("pendingCertificates", pendingCertificates).flush();
    }

    /** Start a new campaign without changing audio or other saved settings. */
    public void reset() {
        clearedCount = pendingCertificates = 0;
        preferences.putInteger("clearedCount", 0).putInteger("pendingCertificates", 0).flush();
    }

    public StageId nextChallenge() {
        return clearedCount < STAGES.length ? STAGES[clearedCount] : null;
    }
}
