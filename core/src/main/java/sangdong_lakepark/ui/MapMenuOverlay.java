package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntSet;
import sangdong_lakepark.audio.GameAudio;

/** Lightweight map HUD; options share the title screen's artwork and saved audio setting. */
public final class MapMenuOverlay implements Disposable {
    private final Stage stage;
    private final MenuTheme theme;
    private final GameAudio audio;
    private final IntSet heldKeys;
    private final Runnable onHome;
    private final Texture icons = createIcons();
    private final Array<Button> buttons = new Array<>();
    private DialogArtwork artwork;
    private final ButtonAccent buttonAccent = new ButtonAccent();
    private Group modal, panel;
    private Image optionSoundOffIndicator;
    private Runnable back;

    public MapMenuOverlay(Stage stage, MenuTheme theme, GameAudio audio, IntSet heldKeys,
                          Runnable onHome) {
        this.stage = stage;
        this.theme = theme;
        this.audio = audio;
        this.heldKeys = heldKeys;
        this.onHome = onHome;
        Image guide = new Image(theme.roundedOverlay(new Color(.082f, .243f, .18f, .28f), 10));
        guide.setName("map-keyboard-guide");
        guide.setBounds(18, 610, 142, 96);
        guide.setTouchable(Touchable.disabled);
        stage.addActor(guide);
        icon(0, 28, 670, 22, 23);
        icon(1, 50, 670, 22, 23);
        label("이동", 106, 668, 48);
        label("SPACE", 32, 640, 64);
        label("선택", 106, 640, 48);
        label("ESC", 32, 612, 64);
        label("설정", 106, 612, 48);
        Image iconBackdrop = new Image(theme.roundedOverlay(new Color(.082f, .243f, .18f, .15f), 12));
        iconBackdrop.setBounds(1160, 654, 104, 48);
        iconBackdrop.setTouchable(Touchable.disabled);
        stage.addActor(iconBackdrop);
        iconButton("map-options", 2, 1164, this::showOptions);
        iconButton("map-home", 3, 1216, this::returnHome);
        stage.addCaptureListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (!isOpen() || !dialogKey(keycode)) return false;
                event.stop();
                // Shared with the map: holding Escape cannot immediately close a new dialog.
                if (!heldKeys.add(keycode)) return true;
                if (PopupAnimation.isOpening(panel)) return true;
                if (keycode == Input.Keys.ESCAPE) {
                    audio.playClick();
                    back.run();
                } else if (keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER) {
                    Actor focus = stage.getKeyboardFocus();
                    Button selected = focus instanceof Button && buttons.contains((Button) focus, true)
                        ? (Button) focus : buttons.first();
                    audio.playClick();
                    selected.fire(new ChangeListener.ChangeEvent());
                } else {
                    Actor focus = stage.getKeyboardFocus();
                    if ("map-exit-confirm".equals(modal.getName())
                        && (keycode == Input.Keys.LEFT || keycode == Input.Keys.RIGHT)) {
                        Button target = buttons.get(keycode == Input.Keys.LEFT ? 0 : 1);
                        if (stage.getKeyboardFocus() != target && stage.setKeyboardFocus(target)) audio.playSelection();
                        return true;
                    }
                    int index = focus instanceof Button ? buttons.indexOf((Button) focus, true) : -1;
                    boolean previous = keycode == Input.Keys.UP || keycode == Input.Keys.LEFT
                        || (keycode == Input.Keys.TAB && (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                        || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)));
                    Button target = buttons.get((index + (previous ? -1 : 1) + buttons.size) % buttons.size);
                    if (stage.getKeyboardFocus() != target && stage.setKeyboardFocus(target)) audio.playSelection();
                }
                return true;
            }

            @Override
            public boolean keyUp(InputEvent event, int keycode) {
                if (!isOpen() || !dialogKey(keycode)) return false;
                heldKeys.remove(keycode);
                event.stop();
                return true;
            }
        });
    }

    private static boolean dialogKey(int key) {
        return key == Input.Keys.ESCAPE || key == Input.Keys.ENTER || key == Input.Keys.SPACE
            || key == Input.Keys.LEFT || key == Input.Keys.RIGHT || key == Input.Keys.UP
            || key == Input.Keys.DOWN || key == Input.Keys.TAB;
    }

    private void label(String text, float x, float y, float width) {
        Label shadow = theme.label(text, theme.small, new Color(0, 0, 0, .85f));
        shadow.setBounds(x + 1, y - 1, width, 26);
        shadow.setTouchable(Touchable.disabled);
        stage.addActor(shadow);
        Label label = theme.label(text, theme.small, Color.WHITE);
        label.setBounds(x, y, width, 26);
        label.setTouchable(Touchable.disabled);
        stage.addActor(label);
    }

    private Image icon(int index, float x, float y, float width, float height) {
        Image image = new Image(new TextureRegion(icons, index * 128, 0, 128, 128));
        image.setBounds(x, y, width, height);
        image.setTouchable(Touchable.disabled);
        stage.addActor(image);
        return image;
    }

    private void iconButton(String name, int index, float x, Runnable action) {
        Button button = button(name, new Rectangle(x, 654, 48, 48), 12, action, false);
        Image shadow = new Image(new TextureRegion(icons, index * 128, 0, 128, 128));
        shadow.setColor(0, 0, 0, .35f);
        shadow.setBounds(9, 7, 32, 32);
        shadow.setTouchable(Touchable.disabled);
        button.addActor(shadow);
        Image image = new Image(new TextureRegion(icons, index * 128, 0, 128, 128));
        image.setBounds(8, 8, 32, 32);
        image.setTouchable(Touchable.disabled);
        button.addActor(image);
        stage.addActor(button);
        stage.addActor(buttonAccent.forButton(button));
    }

    private Button button(String name, Rectangle bounds, float radius, Runnable action, boolean selectable) {
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.over = theme.roundedOverlay(new Color(1, 1, 1, .10f), radius);
        if (selectable) style.focused = style.over;
        style.down = theme.roundedOverlay(new Color(0, 0, 0, .15f), radius);
        Button button = new Button(style);
        button.setName(name);
        button.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        bind(button, action, selectable);
        return button;
    }

    private void bind(final Button button, final Runnable action, final boolean selectable) {
        button.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1 && selectable) stage.setKeyboardFocus(button);
            }
        });
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
    }

    private void beginModal(String name, Runnable backAction) {
        close(false); audio.playWindow();
        back = backAction;
        modal = new Group();
        modal.setName("map-" + name);
        modal.setSize(1280, 720);
        Image backdrop = new Image(theme.overlay(new Color(.10f, .22f, .17f, .56f)));
        backdrop.setBounds(0, 0, 1280, 720);
        backdrop.setTouchable(Touchable.enabled);
        modal.addActor(backdrop);
        stage.addActor(modal);
    }

    private DialogArtwork.Page page(String name, Runnable backAction) {
        beginModal(name, backAction);
        if (artwork == null) {
            artwork = new DialogArtwork();
        }
        DialogArtwork.Page page = artwork.page(name);
        panel = new Group();
        panel.setBounds((1280 - page.width) / 2, (720 - page.height) / 2, page.width, page.height);
        panel.setTouchable(Touchable.childrenOnly);
        panel.addActor(page.image());
        modal.addActor(panel);
        return page;
    }

    private void pageButton(DialogArtwork.Page page, String name, Runnable action) {
        Button button = button(name, page.buttonBounds(name), page.buttonRadius(name), action, true);
        buttons.add(button);
        panel.addActor(button);
        if (!page.hasPaintedButtonAccent()) panel.addActor(buttonAccent.forButton(button));
    }

    public void showOptions() {
        showOptions(true);
    }

    private void showOptions(boolean animate) {
        DialogArtwork.Page page = page("options", this::close);
        Rectangle area = page.soundIndicatorBounds();
        optionSoundOffIndicator = new Image(page.soundOffDrawable());
        optionSoundOffIndicator.setBounds(area.x, area.y, area.width, area.height);
        optionSoundOffIndicator.setTouchable(Touchable.disabled);
        panel.addActor(optionSoundOffIndicator);
        pageButton(page, "controls", this::showControls);
        pageButton(page, "sound", this::toggleSound);
        pageButton(page, "mainMenu", this::returnHome);
        pageButton(page, "quit", this::showExitConfirmation);
        pageButton(page, "close", this::close);
        stage.setKeyboardFocus(buttons.first());
        syncSound();
        if (animate) PopupAnimation.open(panel);
        Gdx.app.log("StageSelection", "Opened map options");
    }

    private void returnHome() {
        Gdx.app.postRunnable(() -> {
            close();
            onHome.run();
        });
    }

    private void showExitConfirmation() {
        Runnable cancel = () -> showOptions(false);
        beginModal("exit-confirm", cancel);
        if (artwork == null) artwork = new DialogArtwork();
        ExitConfirmation confirmation = new ExitConfirmation(theme, artwork.page("exit-confirm"), buttonAccent, () -> {
            Gdx.app.log("StageSelection", "Exit confirmed");
            Gdx.app.exit();
        }, cancel);
        panel = confirmation;
        buttons.add(confirmation.yes);
        buttons.add(confirmation.no);
        buttons.add(confirmation.close);
        modal.addActor(panel);
        PopupAnimation.open(panel);
        stage.setKeyboardFocus(confirmation.no);
        Gdx.app.log("StageSelection", "Opened map exit confirmation");
    }

    private void showControls() {
        Runnable back = () -> showOptions(false);
        DialogArtwork.Page page = page("manual", back);
        pageButton(page, "back", back);
        pageButton(page, "close", back);
        stage.setKeyboardFocus(buttons.first());
    }

    private void toggleSound() {
        audio.toggleEnabled();
        syncSound();
    }

    public void syncSound() {
        if (optionSoundOffIndicator != null) optionSoundOffIndicator.setVisible(!audio.isEnabled());
    }

    public boolean isOpen() {
        return modal != null;
    }

    public void close() {
        close(true);
    }

    private void close(boolean sound) {
        if (modal != null) {
            modal.remove();
            if (sound) audio.playWindow();
        }
        modal = null;
        panel = null;
        optionSoundOffIndicator = null;
        buttons.clear();
        stage.setKeyboardFocus(null);
    }

    /** White rounded triangles and hollow outline icons, with transparent space between strokes. */
    private static Texture createIcons() {
        Pixmap pixels = new Pixmap(512, 128, Pixmap.Format.RGBA8888);
        pixels.setBlending(Pixmap.Blending.None);
        float[] triangle = { .25f, .18f, .25f, .82f, .80f, .50f };
        float[] roof = { .12f, .47f, .50f, .10f, .88f, .47f };
        float[] home = {
            .22f, .51f, .22f, .77f, .223f, .80f, .233f, .823f, .25f, .84f, .28f, .85f,
            .40f, .85f, .40f, .65f, .403f, .62f, .413f, .597f, .43f, .58f, .46f, .57f,
            .54f, .57f, .57f, .58f, .587f, .597f, .597f, .62f, .60f, .65f, .60f, .85f,
            .72f, .85f, .75f, .84f, .767f, .823f, .777f, .80f, .78f, .77f, .78f, .51f
        };
        float[] gear = new float[64];
        for (int i = 0; i < 32; i++) {
            float angle = (i / 4 + (i % 4 == 0 ? -.42f : i % 4 == 1 ? -.23f : i % 4 == 2 ? .23f : .42f))
                * MathUtils.PI / 4;
            float radius = i % 4 == 0 || i % 4 == 3 ? .31f : .41f;
            gear[i * 2] = .5f + MathUtils.cos(angle) * radius;
            gear[i * 2 + 1] = .5f + MathUtils.sin(angle) * radius;
        }
        for (int icon = 0; icon < 4; icon++) {
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    float px = (x + .5f) / 128, py = (y + .5f) / 128;
                    float distance;
                    if (icon < 2) {
                        float tx = icon == 0 ? 1 - px : px;
                        float edge = polygonDistance(tx, py, triangle);
                        boolean inside = tx >= .25f && tx <= .80f
                            && Math.abs(py - .5f) <= (.80f - tx) * .32f / .55f;
                        distance = (inside ? -edge : edge) - .025f;
                    } else if (icon == 2) {
                        distance = Math.min(polygonDistance(px, py, gear),
                            Math.abs(distance(px - .5f, py - .5f) - .13f)) - .018f;
                    } else {
                        distance = Math.min(pathDistance(px, py, roof, false),
                            pathDistance(px, py, home, false)) - .022f;
                    }
                    float alpha = MathUtils.clamp(.5f - distance * 128, 0, 1);
                    pixels.setColor(1, 1, 1, alpha);
                    pixels.drawPixel(icon * 128 + x, y);
                }
            }
        }
        Texture texture = new Texture(pixels, true);
        pixels.dispose();
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        return texture;
    }

    private static float polygonDistance(float x, float y, float[] points) {
        return pathDistance(x, y, points, true);
    }

    private static float pathDistance(float x, float y, float[] points, boolean closed) {
        float minimum = Float.MAX_VALUE;
        for (int i = 0; i < points.length - (closed ? 0 : 2); i += 2) {
            int next = (i + 2) % points.length;
            float dx = points[next] - points[i], dy = points[next + 1] - points[i + 1];
            float t = MathUtils.clamp(((x - points[i]) * dx + (y - points[i + 1]) * dy)
                / (dx * dx + dy * dy), 0, 1);
            minimum = Math.min(minimum, distance(x - points[i] - dx * t, y - points[i + 1] - dy * t));
        }
        return minimum;
    }

    private static float distance(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    @Override
    public void dispose() {
        close(false);
        icons.dispose();
        if (artwork != null) artwork.dispose();
        if (buttonAccent != null) buttonAccent.dispose();
    }
}
