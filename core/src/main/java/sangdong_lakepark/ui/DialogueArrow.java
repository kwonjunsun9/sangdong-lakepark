package sangdong_lakepark.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/** A backgroundless arrow with shared mouse and keyboard activation feedback. */
public final class DialogueArrow extends Group {
    private static final float FLASH_DURATION = .32f;
    private final Image highlight;
    private final ClickListener pointer;
    private float flashTime;

    public DialogueArrow(TextureRegion triangle, TextureRegion highlightedTriangle, Runnable onActivate) {
        setSize(82, 82);
        setOrigin(41, 41);
        setTouchable(Touchable.enabled);
        Image icon = new Image(triangle);
        icon.setBounds(0, 0, 82, 82);
        icon.setTouchable(Touchable.disabled);
        addActor(icon);
        // Identical triangle geometry keeps all light inside the arrow silhouette.
        highlight = new Image(highlightedTriangle);
        highlight.setBounds(0, 0, 82, 82);
        highlight.getColor().a = 0;
        highlight.setTouchable(Touchable.disabled);
        addActor(highlight);
        pointer = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onActivate.run();
            }
        };
        addListener(pointer);
    }

    public void pulse() {
        flashTime = FLASH_DURATION;
    }

    public void resetFeedback() {
        flashTime = 0;
        pointer.cancel();
        setScale(1);
        highlight.getColor().a = 0;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        float dt = MathUtils.clamp(delta, 0, .05f);
        flashTime = Math.max(0, flashTime - dt);
        float pulse = flashTime > 0 ? (float) Math.sin(Math.PI * flashTime / FLASH_DURATION) : 0;
        boolean pressed = pointer.isPressed();
        float targetScale = pressed ? .94f : 1 + pulse * .11f + (pointer.isOver() ? .025f : 0);
        setScale(MathUtils.lerp(getScaleX(), targetScale, Math.min(1, dt * 30)));
        highlight.getColor().a = pressed ? 1 : Math.max(pulse * .95f, pointer.isOver() ? .55f : 0);
    }
}
