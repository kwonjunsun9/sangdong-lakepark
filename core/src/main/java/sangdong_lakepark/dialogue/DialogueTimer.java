package sangdong_lakepark.dialogue;

/** Counts silent display time, or waits for actual voice completion before counting. */
public final class DialogueTimer {
    private boolean waitingForVoice, finished;
    private double elapsed, delay;

    public void reset(boolean voiced, float silentDelay) {
        waitingForVoice = voiced;
        finished = false;
        elapsed = 0;
        delay = voiced ? 1 : silentDelay;
    }

    /** Returns true once per page. Paused scenes must not tick this timer. */
    public boolean update(float delta, boolean voiceFinished) {
        if (finished || delta <= 0) return false;
        if (waitingForVoice) {
            // The completion was observed this frame; begin a full one-second wait now.
            if (voiceFinished) waitingForVoice = false;
            return false;
        }
        elapsed += delta;
        if (elapsed < delay) return false;
        finished = true;
        return true;
    }
}
