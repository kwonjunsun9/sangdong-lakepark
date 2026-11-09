package sangdong_lakepark.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/** Image-backed exit confirmation shared by the title, map and gameplay menus. */
public final class ExitConfirmation extends Group {
    public final Button yes, no, close;

    public ExitConfirmation(MenuTheme theme, DialogArtwork.Page page, ButtonAccent accent, Runnable onYes, Runnable onNo) {
        setName("exit-confirm-panel");
        setBounds((1280 - page.width) / 2, (720 - page.height) / 2, page.width, page.height);
        setTouchable(Touchable.childrenOnly);
        addActor(page.image());
        yes = button(theme, page, "yes", onYes);
        no = button(theme, page, "no", onNo);
        close = button(theme, page, "close", onNo);
        addActor(accent.forHover(yes));
        addActor(accent.forHover(no));
    }

    private Button button(MenuTheme theme, DialogArtwork.Page page, String name, final Runnable action) {
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.over = theme.roundedOverlay(new Color(1, 1, 1, .12f), page.buttonRadius(name));
        style.focused = style.over;
        style.down = theme.roundedOverlay(new Color(0, 0, 0, .15f), page.buttonRadius(name));
        final Button button = new Button(style);
        button.setName(name);
        Rectangle bounds = page.buttonBounds(name);
        button.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        button.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1 && getStage() != null) getStage().setKeyboardFocus(button);
            }
        });
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        addActor(button);
        return button;
    }
}
