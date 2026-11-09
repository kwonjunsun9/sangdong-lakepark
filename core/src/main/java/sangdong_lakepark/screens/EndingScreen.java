package sangdong_lakepark.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import sangdong_lakepark.audio.GameAudio;
import sangdong_lakepark.ui.DialogArtwork;
import sangdong_lakepark.ui.MenuTheme;
import sangdong_lakepark.ui.PopupAnimation;

/** Extreme's arrival artwork followed by the new-campaign confirmation. */
public final class EndingScreen extends ScreenAdapter {
    private static final float WIDTH = 1280, HEIGHT = 720, ARRIVAL_DURATION = 1.2f;
    private static final int[] KEYS = {Input.Keys.LEFT, Input.Keys.RIGHT, Input.Keys.UP,
        Input.Keys.DOWN, Input.Keys.TAB, Input.Keys.SPACE, Input.Keys.ENTER, Input.Keys.ESCAPE};
    private final Stage stage = new Stage(new FitViewport(WIDTH, HEIGHT));
    private final MenuTheme theme = new MenuTheme();
    private final DialogArtwork dialogs = new DialogArtwork();
    private final Texture arrival = new Texture(Gdx.files.classpath("sangdong/ending/arrival.png"));
    private final GameAudio audio;
    private final Runnable onRestart, onMap;
    private final IntSet heldKeys = new IntSet();
    private Group prompt;
    private Button yes, no;
    private float elapsed;
    private boolean answered;

    public EndingScreen(GameAudio audio, Runnable onRestart, Runnable onMap) {
        this.audio = audio; this.onRestart = onRestart; this.onMap = onMap;
        arrival.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        stage.addCaptureListener(new InputListener() {
            @Override public boolean keyDown(InputEvent event, int keycode) {
                if (!menuKey(keycode)) return false;
                event.stop();
                if (!heldKeys.add(keycode) || !canChoose()) return true;
                if (keycode == Input.Keys.ESCAPE) {
                    audio.playClick(); choose(false);
                } else if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                    audio.playClick(); choose(stage.getKeyboardFocus() == yes);
                } else {
                    Button target = keycode == Input.Keys.LEFT ? yes : keycode == Input.Keys.RIGHT ? no
                        : stage.getKeyboardFocus() == yes ? no : yes;
                    if (stage.getKeyboardFocus() != target && stage.setKeyboardFocus(target)) audio.playSelection();
                }
                return true;
            }
            @Override public boolean keyUp(InputEvent event, int keycode) {
                heldKeys.remove(keycode);
                if (!menuKey(keycode)) return false;
                event.stop(); return true;
            }
        });
    }

    private static boolean menuKey(int key) {
        for (int candidate : KEYS) if (candidate == key) return true;
        return false;
    }

    private boolean canChoose() { return prompt != null && !answered && !PopupAnimation.isOpening(prompt); }

    private void showPrompt() {
        DialogArtwork.Page page = dialogs.page("ending-restart");
        Image shade = new Image(theme.overlay(new Color(0, 0, 0, .4f)));
        shade.setName("ending-dim"); shade.setBounds(0, 0, WIDTH, HEIGHT);
        shade.setTouchable(Touchable.enabled); stage.addActor(shade);
        prompt = new Group(); prompt.setName("ending-restart-panel");
        prompt.setBounds((WIDTH - page.width) / 2, HEIGHT * .425f - page.height / 2, page.width, page.height);
        prompt.setTouchable(Touchable.childrenOnly); prompt.addActor(page.image());
        yes = button(page, "yes", true); no = button(page, "no", false);
        stage.addActor(prompt); stage.setKeyboardFocus(no);
        PopupAnimation.open(prompt); audio.playWindow();
    }

    private Button button(DialogArtwork.Page page, String name, boolean restart) {
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.over = theme.roundedOverlay(new Color(1, 1, 1, .12f), page.buttonRadius(name));
        style.focused = style.over;
        style.down = theme.roundedOverlay(new Color(0, 0, 0, .12f), page.buttonRadius(name));
        Button button = new Button(style); button.setName("ending-" + name);
        Rectangle area = page.buttonBounds(name); button.setBounds(area.x, area.y, area.width, area.height);
        button.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { choose(restart); }
        });
        button.addListener(new ClickListener() {
            @Override public void enter(InputEvent event, float x, float y, int pointer, Actor from) {
                super.enter(event, x, y, pointer, from);
                if (pointer == -1 && canChoose()) stage.setKeyboardFocus(button);
            }
        });
        prompt.addActor(button); return button;
    }

    private void choose(boolean restart) {
        if (!canChoose()) return;
        answered = true; prompt.setTouchable(Touchable.disabled);
        Gdx.app.postRunnable(restart ? onRestart : onMap);
    }

    @Override public void show() {
        stage.cancelTouchFocus(); stage.unfocusAll(); stage.getRoot().clearChildren();
        Image background = new Image(arrival); background.setName("ending-arrival");
        background.setBounds(0, 0, WIDTH, HEIGHT); background.setTouchable(Touchable.disabled);
        stage.addActor(background);
        prompt = null; yes = no = null; elapsed = 0; answered = false;
        heldKeys.clear(); for (int key : KEYS) if (Gdx.input.isKeyPressed(key)) heldKeys.add(key);
        Gdx.input.setInputProcessor(stage); Gdx.input.setCatchKey(Input.Keys.ESCAPE, true);
    }

    @Override public void render(float delta) {
        float dt = audio.isPaused() ? 0 : MathUtils.clamp(delta, 0, .05f);
        for (int key : KEYS) if (!Gdx.input.isKeyPressed(key)) heldKeys.remove(key);
        elapsed += dt;
        if (prompt == null && elapsed >= ARRIVAL_DURATION) showPrompt();
        ScreenUtils.clear(MenuTheme.INK); stage.getViewport().apply(); stage.act(dt); stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

    @Override public void hide() {
        stage.cancelTouchFocus(); heldKeys.clear();
        if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, false);
    }

    @Override public void dispose() {
        hide(); stage.dispose(); theme.dispose(); dialogs.dispose(); arrival.dispose();
    }
}
