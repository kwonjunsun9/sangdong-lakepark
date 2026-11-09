package sangdong_lakepark.stages;

/** The stage waits for this short READY/Go sequence before accepting gameplay input. */
public final class StageStartSequence {
    private static final float READY_GROW = .2f, READY_HOLD = 1f;
    private static final float GO_EXIT = 1f;
    private static final float READY_END = READY_GROW + READY_HOLD;
    private static final float END = READY_END + GO_EXIT;
    private float elapsed;

    public void reset() { elapsed = 0; }
    public void finish() { elapsed = END; }
    public boolean isActive() { return elapsed < END; }
    public boolean isReady() { return elapsed < READY_END; }

    /** Call only while the stage is visible, landed and unpaused. */
    public void update(float delta) {
        if (delta > 0) elapsed = Math.min(END, elapsed + delta);
    }

    public float getScale() {
        if (!isReady()) return 1;
        float t = Math.min(1, elapsed / READY_GROW);
        return .8f + .2f * (1 - (1 - t) * (1 - t));
    }

    private float exitProgress() {
        return Math.max(0, Math.min(1, (elapsed - READY_END) / GO_EXIT));
    }

    public float getOffsetX() {
        float t = exitProgress();
        return 140 * t * t;
    }

    public float getAlpha() { return 1 - exitProgress(); }
}
