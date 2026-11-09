package sangdong_lakepark.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Shows startup artwork for two visible seconds before the title fade. */
public final class StartupScreen extends ScreenAdapter {
    private static final float WIDTH = 1280, HEIGHT = 720, HOLD_SECONDS = 2;
    private final FitViewport viewport = new FitViewport(WIDTH, HEIGHT);
    private final SpriteBatch batch = new SpriteBatch();
    private final Texture artwork = new Texture(Gdx.files.classpath("sangdong/startup/loading.png"));
    private final InputAdapter blocker = new InputAdapter();
    private final Runnable prepareNext, onFinished;
    private final float imageWidth, imageHeight;
    private float elapsed;
    private boolean firstFrame, prepared, transitionStarted;

    public StartupScreen(Runnable prepareNext, Runnable onFinished) {
        this.prepareNext = prepareNext;
        this.onFinished = onFinished;
        artwork.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Vector2 size = Scaling.fit.apply(artwork.getWidth(), artwork.getHeight(), WIDTH, HEIGHT);
        imageWidth = size.x;
        imageHeight = size.y;
    }

    @Override public void show() {
        elapsed = 0;
        firstFrame = true;
        prepared = false;
        transitionStarted = false;
        Gdx.input.setInputProcessor(blocker);
    }

    @Override public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        batch.draw(artwork, (WIDTH - imageWidth) / 2, (HEIGHT - imageHeight) / 2, imageWidth, imageHeight);
        batch.end();

        // The first delta may include time spent creating the application's resources.
        if (firstFrame) {
            firstFrame = false;
        } else if (!transitionStarted) {
            // Prepare the menu while the already-presented loading image remains on screen.
            if (!prepared) {
                prepared = true;
                prepareNext.run();
            }
            elapsed += Math.max(0, delta);
            if (elapsed >= HOLD_SECONDS) {
                transitionStarted = true;
                onFinished.run();
            }
        }
    }

    @Override public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void hide() {
        if (Gdx.input.getInputProcessor() == blocker) Gdx.input.setInputProcessor(null);
    }

    @Override public void dispose() {
        hide();
        artwork.dispose();
        batch.dispose();
    }
}
