package sangdong_lakepark.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import sangdong_lakepark.world.StageRun;

/** Small world-space puffs, emitted only when a mounted vehicle travels on the ground. */
public final class EasyBicycleTrail {
    private static final float LIFE = .48f, INTERVAL = .11f;
    private static final class Puff { float x, y, age = LIFE, direction; }
    private final Puff[] puffs = new Puff[10];
    private int next;
    private float emission;

    public EasyBicycleTrail() { for (int i = 0; i < puffs.length; i++) puffs[i] = new Puff(); }

    public void clear() { for (Puff puff : puffs) puff.age = LIFE; emission = 0; next = 0; }

    public void update(float delta, StageRun run, float distance, boolean fell) {
        float dt = MathUtils.clamp(delta, 0, .25f);
        for (Puff puff : puffs) if (puff.age < LIFE) {
            puff.age += dt; puff.x += puff.direction * 14 * dt; puff.y += 8 * dt;
        }
        if (fell) { clear(); return; }
        if (!run.isRiding() || !run.isGrounded() || run.isCleared() || run.isFailed() || Math.abs(distance) < .5f) {
            emission = 0; return;
        }
        emission += dt;
        while (emission >= INTERVAL) {
            emission -= INTERVAL;
            Puff puff = puffs[next]; next = (next + 1) % puffs.length;
            puff.direction = run.isFacingLeft() ? 1 : -1; puff.age = 0;
            puff.x = run.player.x + (run.isFacingLeft() ? run.player.width + 24 : -24);
            puff.y = run.player.y + 2;
        }
    }

    public void draw(SpriteBatch batch, TextureRegion image) {
        Color color = batch.getColor(); float r = color.r, g = color.g, b = color.b, a = color.a;
        for (Puff puff : puffs) if (puff.age < LIFE) {
            float t = puff.age / LIFE, height = 28 - 12 * t;
            float width = height * image.getRegionWidth() / image.getRegionHeight();
            batch.setColor(.72f, .72f, .72f, .85f * (1 - t));
            batch.draw(image, puff.direction > 0 ? puff.x + width / 2 : puff.x - width / 2, puff.y,
                puff.direction > 0 ? -width : width, height);
        }
        batch.setColor(r, g, b, a);
    }
}
