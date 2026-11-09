package sangdong_lakepark.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import sangdong_lakepark.audio.GameAudio;
import sangdong_lakepark.dialogue.DialogueScript;
import sangdong_lakepark.dialogue.DialogueTimer;
import sangdong_lakepark.ui.DialogueArrow;
import sangdong_lakepark.ui.MenuTheme;
import sangdong_lakepark.ui.RiderArtwork;

/** Reversible conversations with automatic advances after voiced and silent lines. */
public final class DialogueScreen extends ScreenAdapter {
    private static final float MANUAL_EXIT_FEEDBACK = .16f;
    private static final int[] NAVIGATION_KEYS = {
        Input.Keys.LEFT, Input.Keys.RIGHT, Input.Keys.SPACE, Input.Keys.ENTER
    };
    private final Stage stage = new Stage(new FitViewport(1280, 720));
    private final MenuTheme theme = new MenuTheme();
    private final RiderArtwork riderArtwork = RiderArtwork.load();
    private final Texture background, tailTexture, navigationTexture, highlightTexture;
    private final Image rider, tail;
    private final DialogueArrow previousButton, nextButton;
    private final Label speaker, text;
    private final GameAudio audio;
    private final DialogueScript script;
    private final DialogueTimer timer = new DialogueTimer();
    private final Runnable onFinished, onBack;
    private final IntSet heldKeys = new IntSet();
    private int pageIndex;
    private boolean leaving, manualExitPending;
    private float elapsed, manualExitTime;

    private final InputAdapter keyboard = new InputAdapter() {
        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
                if (!leaving) {
                    leaving = true;
                    Gdx.app.postRunnable(onBack);
                }
                return true;
            }
            if (!isNavigationKey(keycode)) return false;
            if (heldKeys.add(keycode)) navigate(keycode == Input.Keys.LEFT ? -1 : 1);
            return true;
        }

        @Override
        public boolean keyUp(int keycode) {
            heldKeys.remove(keycode);
            return isNavigationKey(keycode);
        }
    };
    private final InputMultiplexer input = new InputMultiplexer(keyboard, stage);

    public DialogueScreen(GameAudio audio, DialogueScript script, Runnable onFinished, Runnable onBack) {
        this.audio = audio;
        this.script = script;
        this.onFinished = onFinished;
        this.onBack = onBack;
        // Use the bold Gothic face; every two-line dialogue uses the same leading.
        theme.heading.getData().setLineHeight(46);
        theme.body.getData().setScale(.9f);
        background = new Texture(Gdx.files.classpath(script.backgroundPath()));
        background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image park = new Image(background);
        park.setBounds(0, 0, 1280, 720);
        stage.addActor(park);
        rider = riderArtwork.image(456);
        rider.setPosition(350 - rider.getWidth() / 2, 50);
        rider.setVisible(script.usesRiderOverlay());
        stage.addActor(rider);

        tailTexture = speechTail();
        tail = new Image(tailTexture);
        tail.setSize(100, 58);
        tail.setOrigin(50, 29);
        stage.addActor(tail);
        Image bubble = new Image(theme.roundedOverlay(Color.WHITE, 78));
        bubble.setBounds(144, 534, 1040, 170);
        stage.addActor(bubble);
        text = theme.label("", theme.heading, Color.valueOf("151515"));
        text.setAlignment(Align.left);
        text.setBounds(190, 550, 910, 138);
        stage.addActor(text);
        Image namePlate = new Image(theme.roundedOverlay(Color.valueOf("41413F"), 20));
        namePlate.setBounds(882, 512, 302, 60);
        stage.addActor(namePlate);
        speaker = theme.label("", theme.heading, Color.WHITE);
        speaker.setAlignment(Align.center);
        speaker.setBounds(882, 512, 302, 60);
        stage.addActor(speaker);

        Image guideBackground = new Image(theme.roundedOverlay(new Color(.082f, .243f, .18f, .38f), 10));
        guideBackground.setBounds(8, 580, 104, 108);
        stage.addActor(guideBackground);
        guideLabel("←", theme.button, 22, 654, 24, 24);
        guideLabel("→", theme.button, 22, 638, 24, 24);
        guideLabel("대화", theme.body, 50, 642, 62, 32);
        guideLabel("ESC 이전", theme.body, 22, 588, 110, 28);

        navigationTexture = navigationTriangle(false);
        highlightTexture = navigationTriangle(true);
        previousButton = navigationButton(true, 1000);
        nextButton = navigationButton(false, 1136);
        for (Actor actor : stage.getActors()) {
            if (actor != previousButton && actor != nextButton) actor.setTouchable(Touchable.disabled);
        }
    }

    private void guideLabel(String value, BitmapFont font, float x, float y, float width, float height) {
        Label shadow = theme.label(value, font, new Color(0, 0, 0, .8f));
        shadow.setBounds(x + 1.2f, y - 1.2f, width, height);
        stage.addActor(shadow);
        Label label = theme.label(value, font, Color.WHITE);
        label.setBounds(x, y, width, height);
        stage.addActor(label);
    }

    private DialogueArrow navigationButton(final boolean previous, float x) {
        TextureRegion region = new TextureRegion(navigationTexture);
        TextureRegion highlightedRegion = new TextureRegion(highlightTexture);
        region.flip(previous, false);
        highlightedRegion.flip(previous, false);
        DialogueArrow button = new DialogueArrow(region, highlightedRegion, () -> navigate(previous ? -1 : 1));
        button.setPosition(x, 24);
        stage.addActor(button);
        return button;
    }

    private static Texture speechTail() {
        Pixmap pixels = new Pixmap(128, 72, Pixmap.Format.RGBA8888);
        pixels.setColor(Color.WHITE);
        pixels.fillTriangle(128, 0, 72, 0, 0, 71);
        Texture texture = new Texture(pixels);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixels.dispose();
        return texture;
    }

    private static Texture navigationTriangle(boolean highlighted) {
        Pixmap pixels = new Pixmap(512, 512, Pixmap.Format.RGBA8888);
        int[] xs = { 42, 472, 42 }, ys = { 42, 256, 470 };
        pixels.setColor(highlighted ? Color.WHITE : Color.valueOf("FFF3D4"));
        pixels.fillTriangle(xs[0], ys[0], xs[1], ys[1], xs[2], ys[2]);
        pixels.setColor(Color.valueOf(highlighted ? "EFC66B" : "49362E"));
        for (int edge = 0; edge < 3; edge++) {
            int end = (edge + 1) % 3;
            int steps = Math.max(Math.abs(xs[end] - xs[edge]), Math.abs(ys[end] - ys[edge]));
            for (int i = 0; i <= steps; i++) {
                float t = (float) i / steps;
                pixels.fillCircle(Math.round(MathUtils.lerp(xs[edge], xs[end], t)),
                    Math.round(MathUtils.lerp(ys[edge], ys[end], t)), 7);
            }
        }
        Texture texture = new Texture(pixels, true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        pixels.dispose();
        return texture;
    }

    private static boolean isNavigationKey(int keycode) {
        for (int key : NAVIGATION_KEYS) if (keycode == key) return true;
        return false;
    }

    private void showPage() {
        DialogueScript.Line line = script.line(pageIndex);
        speaker.setText(script==DialogueScript.HARD&&line.mother?"대학생":line.speaker());
        text.setText(line.text);
        tail.setPosition(line.mother ? 1005 : 455, 486);
        tail.setScaleX(line.mother ? -1 : 1);
        elapsed = 0;
        timer.reset(line.voice != null, script.silentPageDelay());
        audio.playDialogueLine(line.voice);
        Gdx.app.log(script.logTag, "Page " + (pageIndex + 1) + " speaker=" + (script==DialogueScript.HARD&&line.mother?"대학생":line.speaker()));
    }

    private void navigate(int direction) {
        if (leaving) return;
        (direction < 0 ? previousButton : nextButton).pulse();
        if (pageIndex == script.lastPage() && direction > 0) {
            // Stop the voice now, then keep a few frames for the pressed-arrow feedback.
            leaving = true;
            manualExitPending = true;
            manualExitTime = 0;
            if(script==DialogueScript.HARD||script==DialogueScript.EXTREME)audio.endDialogueForStage();else audio.endDialogue();
            Gdx.app.log(script.logTag, "Manual dialogue transition");
            return;
        }
        int next = MathUtils.clamp(pageIndex + direction, 0, script.lastPage());
        if (next == pageIndex) return;
        pageIndex = next;
        showPage();
    }

    @Override
    public void show() {
        pageIndex = 0;
        leaving = false;
        manualExitPending = false;
        previousButton.resetFeedback();
        nextButton.resetFeedback();
        heldKeys.clear();
        for (int key : NAVIGATION_KEYS) if (Gdx.input.isKeyPressed(key)) heldKeys.add(key);
        audio.beginDialogue();
        showPage();
        Gdx.input.setInputProcessor(input);
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, true);
    }

    @Override
    public void hide() {
        hide(false);
    }

    public void hideForStage() { hide(true); }

    private void hide(boolean stageHandoff) {
        stage.cancelTouchFocus();
        if (Gdx.input.getInputProcessor() == input) Gdx.input.setInputProcessor(null);
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, false);
        heldKeys.clear();
        manualExitPending = false;
        if (stageHandoff) audio.endDialogueForStage(); else audio.endDialogue();
    }

    @Override
    public void render(float delta) {
        for (int key : NAVIGATION_KEYS) if (!Gdx.input.isKeyPressed(key)) heldKeys.remove(key);
        if (!audio.isPaused()) {
            elapsed += Math.min(delta, .05f);
            if (manualExitPending) {
                manualExitTime += Math.min(Math.max(0, delta), .05f);
                if (manualExitTime >= MANUAL_EXIT_FEEDBACK) {
                    manualExitPending = false;
                    Gdx.app.postRunnable(onFinished);
                }
            } else if (!leaving && timer.update(delta, audio.isDialogueVoiceFinished())) {
                if (pageIndex == script.lastPage()) {
                    leaving = true;
                    Gdx.app.log(script.logTag, "Automatic dialogue transition");
                    Gdx.app.postRunnable(onFinished);
                } else {
                    pageIndex++;
                    showPage();
                }
            }
        }
        rider.setY(50 + (script.shakeLastPage && pageIndex == script.lastPage()
            ? (float) Math.sin(elapsed * 65) * 1.6f : 0));
        ScreenUtils.clear(MenuTheme.INK);
        stage.getViewport().apply();
        stage.act(Math.min(delta, .05f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        hide();
        stage.dispose();
        riderArtwork.dispose();
        background.dispose();
        tailTexture.dispose();
        navigationTexture.dispose();
        highlightTexture.dispose();
        theme.dispose();
    }
}
