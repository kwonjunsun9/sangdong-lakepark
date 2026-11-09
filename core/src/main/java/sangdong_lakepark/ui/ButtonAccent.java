package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;

/** Yellow rays beside a hovered, keyboard-selected or activated button. */
public final class ButtonAccent implements Disposable {
    private final Texture texture;
    private final TextureRegion rays;

    public ButtonAccent() {
        Pixmap source = new Pixmap(Gdx.files.classpath("sangdong/dialogs/button-accent.png"));
        try {
            int left = source.getWidth(), top = source.getHeight(), right = 0, bottom = 0;
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    if ((source.getPixel(x, y) & 0xff) < 64) continue;
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
            texture = new Texture(source);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            rays = new TextureRegion(texture, left, top, right - left + 1, bottom - top + 1);
        } finally {
            source.dispose();
        }
    }

    public Actor forButton(final Button button) {
        return accent(button, false);
    }

    /** Hover-only rays, independent of the default keyboard selection in a dialog. */
    public Actor forHover(final Button button) {
        return accent(button, true);
    }

    private Actor accent(final Button button, final boolean hoverOnly) {
        button.addListener(new ClickListener() {
            @Override public void enter(InputEvent event, float x, float y, int pointer, Actor from) {
                if (pointer == -1 && button.getStage() != null) button.getStage().setKeyboardFocus(button);
            }
        });
        return new Actor() {
            private float activation;

            {
                setName(button.getName() + "-accent");
                setTouchable(Touchable.disabled);
                button.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        activation = .16f;
                    }
                });
            }

            @Override
            public void act(float delta) {
                super.act(delta);
                activation = Math.max(0, activation - delta);
            }

            @Override
            public void draw(Batch batch, float parentAlpha) {
                boolean focused = button.getStage() != null
                    && button.getStage().getKeyboardFocus() == button;
                boolean hovered = button.isOver() && (button.getStage() == null
                    || !(button.getStage().getKeyboardFocus() instanceof Button));
                if (hoverOnly ? !button.isOver() : !focused && !hovered && !button.isPressed() && activation <= 0) return;
                float pulse = button.isPressed() ? 1.12f : 1 + .12f * activation / .16f;
                float height = Math.min(36, button.getHeight() * .5f) * pulse;
                float width = height * rays.getRegionWidth() / rays.getRegionHeight();
                Color color = batch.getColor();
                float r = color.r, g = color.g, b = color.b, a = color.a;
                batch.setColor(1, 1, 1, parentAlpha * getColor().a);
                batch.draw(rays, button.getX() - width * .7f,
                    button.getY() + button.getHeight() - height * .35f, width, height);
                batch.setColor(r, g, b, a);
            }
        };
    }

    public TextureRegionDrawable drawable() { return new TextureRegionDrawable(rays); }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
