package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/** Switches scenes at full black and blocks their event handlers until the fade finishes. */
public final class ScreenFade implements Disposable {
    private static final float FADE_OUT = .32f, FADE_IN = .28f;
    private final SpriteBatch batch = new SpriteBatch();
    private final ScreenViewport viewport = new ScreenViewport();
    private final InputProcessor blocker = new InputAdapter();
    private final Texture pixel;
    private InputProcessor resumeInput;
    private Runnable change;
    private float elapsed, opacity;
    private Phase phase = Phase.IDLE;

    private enum Phase { IDLE, OUT, IN }

    public ScreenFade() {
        Pixmap pixels = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixels.setColor(Color.WHITE);
        pixels.fill();
        pixel = new Texture(pixels);
        pixels.dispose();
    }

    public boolean start(Runnable next) {
        if (isActive()) return false;
        change = next;
        elapsed = opacity = 0;
        phase = Phase.OUT;
        resumeInput = Gdx.input.getInputProcessor();
        Gdx.input.setInputProcessor(blocker);
        return true;
    }

    public void update(float delta) {
        if (!isActive()) return;
        elapsed += MathUtils.clamp(delta, 0, .05f);
        if (phase == Phase.OUT) {
            opacity = Interpolation.smooth.apply(Math.min(1, elapsed / FADE_OUT));
            if (elapsed >= FADE_OUT) {
                phase = Phase.IN;
                elapsed = 0;
                opacity = 1;
                Runnable next = change;
                change = null;
                next.run();
                // A scene's show() may install its input processor; an in-place dialog keeps the old one.
                InputProcessor target = Gdx.input.getInputProcessor();
                if (target != blocker) resumeInput = target;
                Gdx.input.setInputProcessor(blocker);
            }
        } else {
            opacity = 1 - Interpolation.smooth.apply(Math.min(1, elapsed / FADE_IN));
            if (elapsed >= FADE_IN) {
                opacity = 0;
                phase = Phase.IDLE;
                Gdx.input.setInputProcessor(resumeInput);
                resumeInput = null;
            }
        }
    }

    public boolean isActive() {
        return phase != Phase.IDLE;
    }

    public void draw() {
        if (opacity <= 0) return;
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.setColor(0, 0, 0, opacity);
        batch.begin();
        batch.draw(pixel, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.end();
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (Gdx.input.getInputProcessor() == blocker) Gdx.input.setInputProcessor(null);
        change = null;
        resumeInput = null;
        batch.dispose();
        pixel.dispose();
    }
}
