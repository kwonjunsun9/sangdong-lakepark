package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/** Straight title and spaced gold controls over the paused world, without an opaque panel. */
public final class EasyPauseView implements Disposable {
    public static final float WIDTH = 440, HEIGHT = 420;
    private final BitmapFont title;
    private final EasyClearView gold;
    private final ButtonAccent accent = new ButtonAccent();

    public EasyPauseView(EasyClearView gold) {
        this.gold = gold;
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/pretendard-jp/PretendardJP-Bold.otf"));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = 72; p.characters = "PAUSE "; p.color = Color.valueOf("FFF8E5");
            p.borderWidth = 3; p.borderColor = Color.valueOf("173F35");
            p.shadowOffsetX = 1; p.shadowOffsetY = 5; p.shadowColor = Color.valueOf("173F3580");
            p.minFilter = p.magFilter = Texture.TextureFilter.Linear;
            title = generator.generateFont(p);
        } finally { generator.dispose(); }
    }

    public Table panel(Runnable resume, Runnable retry, Runnable stage, Array<Button> controls) {
        Table panel = new Table(); panel.setName("easy-pause-panel"); panel.setSize(WIDTH, HEIGHT);
        panel.setTransform(true); panel.setTouchable(Touchable.childrenOnly);
        Label heading = new Label("PAUSE", new Label.LabelStyle(title, Color.WHITE));
        heading.setName("easy-pause-title"); heading.setBounds(0, 292, WIDTH, 108);
        heading.setAlignment(Align.center); heading.setTouchable(Touchable.disabled); panel.addActor(heading);
        button(panel, controls, "계속하기", 212, resume);
        button(panel, controls, "다시 시작", 116, retry);
        button(panel, controls, "스테이지", 20, stage);
        return panel;
    }

    private void button(Table panel, Array<Button> controls, String text, float y, Runnable action) {
        TextButton button = new TextButton(text, gold.buttonStyle()); button.setName("easy-" + text);
        button.setBounds(65, y, 310, 68);
        button.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { action.run(); }
        });
        controls.add(button); panel.addActor(button); panel.addActor(accent.forButton(button));
    }

    @Override public void dispose() { title.dispose(); accent.dispose(); }
}
