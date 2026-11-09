package sangdong_lakepark.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.utils.Align;

/** Shared centered pop entrance; buttons become interactive once the panel settles. */
public final class PopupAnimation {
    private PopupAnimation() { }

    public static void open(Group panel) {
        if (panel.getParent() instanceof Layout) ((Layout) panel.getParent()).validate();
        if (panel instanceof Layout) ((Layout) panel).validate();
        panel.setTransform(true);
        panel.setOrigin(Align.center);
        Touchable touchable = panel.getTouchable();
        panel.setTouchable(Touchable.disabled);
        panel.setScale(.86f);
        panel.addAction(new Entrance(touchable));
    }

    public static boolean isOpening(Group panel) {
        if (panel != null) for (Action action : panel.getActions()) {
            if (action instanceof Entrance) return true;
        }
        return false;
    }

    private static final class Entrance extends TemporalAction {
        private final Touchable touchable;

        Entrance(Touchable touchable) { super(.30f); this.touchable = touchable; }

        @Override protected void update(float percent) {
            float scale = percent < .6f
                ? MathUtils.lerp(.86f, 1.035f, Interpolation.sineOut.apply(percent / .6f))
                : MathUtils.lerp(1.035f, 1, Interpolation.sine.apply((percent - .6f) / .4f));
            actor.setScale(scale);
        }

        @Override protected void end() {
            actor.setScale(1);
            actor.setTouchable(touchable);
        }
    }
}
