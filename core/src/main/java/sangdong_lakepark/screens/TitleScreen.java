package sangdong_lakepark.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import sangdong_lakepark.audio.GameAudio;
import sangdong_lakepark.ui.ButtonAccent;
import sangdong_lakepark.ui.DialogArtwork;
import sangdong_lakepark.ui.ExitConfirmation;
import sangdong_lakepark.ui.MenuTheme;
import sangdong_lakepark.ui.PopupAnimation;
import sangdong_lakepark.ui.TitleArtwork;

/** Image-backed title menu and its introduction, options and controls pages. */
public final class TitleScreen extends ScreenAdapter {
    private static final float WIDTH = 1280, HEIGHT = 720;
    private static final String[] ABOUT_PAGES = { "about", "about-character", "about-items", "about-idea" };
    private static final String ABOUT_VIDEO_URL = "https://www.youtube.com/watch?v=o-nMYZaeAnc";
    private static final int[] MENU_KEYS = { Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT,
        Input.Keys.TAB, Input.Keys.SPACE, Input.Keys.ENTER, Input.Keys.ESCAPE };
    private final Stage stage = new Stage(new FitViewport(WIDTH, HEIGHT));
    private final MenuTheme theme = new MenuTheme();
    private final TitleArtwork artwork = TitleArtwork.load(WIDTH, HEIGHT);
    private final DialogArtwork dialogs = new DialogArtwork();
    private final ButtonAccent buttonAccent = new ButtonAccent();
    private final GameAudio audio;
    private final Array<Button> mainButtons = new Array<>();
    private final Array<Button> dialogButtons = new Array<>();
    private Image soundMuteIndicator, optionSoundOffIndicator, selectionMarker;
    private final IntSet heldKeys = new IntSet();
    private Group activeDialog, dialogPanel;
    private Runnable backAction;
    private final Runnable onStart;
    private int aboutPageIndex = -1;

    public TitleScreen(GameAudio audio, Runnable onStart) {
        this.audio = audio;
        this.onStart = onStart;
        buildMenu();
        stage.addCaptureListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (!isMenuKey(keycode)) return false;
                event.stop();
                // A held selection key must not activate a newly opened confirmation window.
                if (!heldKeys.add(keycode)) return true;
                if (PopupAnimation.isOpening(dialogPanel)) return true;
                if (keycode == Input.Keys.ESCAPE) {
                    audio.playClick();
                    if (activeDialog == null) showOptions();
                    else if (backAction != null) backAction.run();
                    return true;
                }
                if (aboutPageIndex >= 0 && (keycode == Input.Keys.LEFT || keycode == Input.Keys.RIGHT)) {
                    audio.playClick();
                    showAbout(aboutPageIndex + (keycode == Input.Keys.LEFT ? -1 : 1));
                    return true;
                }
                Array<Button> buttons = activeDialog == null ? mainButtons : dialogButtons;
                if (buttons.isEmpty()) return false;
                int index = -1;
                for (int i = 0; i < buttons.size; i++) {
                    if (buttons.get(i) == stage.getKeyboardFocus()) index = i;
                }
                boolean horizontal = activeDialog != null && "exit-confirm".equals(activeDialog.getName())
                    && (keycode == Input.Keys.LEFT || keycode == Input.Keys.RIGHT);
                if (horizontal) {
                    Button target = buttons.get(keycode == Input.Keys.LEFT ? 0 : 1);
                    if (stage.getKeyboardFocus() != target && stage.setKeyboardFocus(target)) audio.playSelection();
                    return true;
                }
                if (keycode == Input.Keys.UP || keycode == Input.Keys.DOWN || keycode == Input.Keys.TAB) {
                    boolean backwards = keycode == Input.Keys.UP || keycode == Input.Keys.LEFT || (keycode == Input.Keys.TAB
                        && (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                        || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)));
                    index = index < 0 ? 0 : (index + (backwards ? -1 : 1) + buttons.size) % buttons.size;
                    Button target = buttons.get(index);
                    if (stage.getKeyboardFocus() != target && stage.setKeyboardFocus(target)) audio.playSelection();
                    return true;
                }
                if (keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER) {
                    Button selected = index < 0 ? buttons.first() : buttons.get(index);
                    audio.playClick();
                    selected.fire(new ChangeListener.ChangeEvent());
                    return true;
                }
                return true;
            }

            @Override
            public boolean keyUp(InputEvent event, int keycode) {
                heldKeys.remove(keycode);
                if (!isMenuKey(keycode)) return false;
                event.stop();
                return true;
            }
        });
        stage.setKeyboardFocus(mainButtons.first());
    }

    private void buildMenu() {
        stage.addActor(artwork.image());
        mainButtons.add(mainButton("start", artwork.startButtonBounds(), this::showStartHelp, true));
        mainButtons.add(mainButton("about", artwork.aboutButtonBounds(), this::showAbout, true));
        mainButtons.add(mainButton("exit", artwork.exitButtonBounds(), this::showExitConfirmation, true));
        mainButton("options", artwork.optionsButtonBounds(), this::showOptions, false);
        mainButton("sound", artwork.soundButtonBounds(), this::toggleSound, false);
        soundMuteIndicator = indicator(artwork.soundIndicatorBounds(), theme.muteSlash());
        stage.addActor(soundMuteIndicator);
        selectionMarker = artwork.selectionMarker();
        selectionMarker.setDrawable(buttonAccent.drawable());
        stage.addActor(selectionMarker);
        buildKeyboardGuide();
    }

    private static boolean isMenuKey(int keycode) {
        for (int key : MENU_KEYS) if (key == keycode) return true;
        return false;
    }

    private void buildKeyboardGuide() {
        Image background = new Image(theme.roundedOverlay(new Color(.082f, .243f, .18f, .4f), 10));
        background.setBounds(24, 584, 158, 108);
        background.setTouchable(Touchable.disabled);
        stage.addActor(background);
        guideLabel("▲▼", 36, 653, 54);
        guideLabel("메뉴 이동", 98, 653, 78);
        guideLabel("SPACE", 36, 623, 54);
        guideLabel("선택", 98, 623, 78);
        guideLabel("ESC", 36, 593, 54);
        guideLabel("설정", 98, 593, 78);
    }

    private void guideLabel(String value, float x, float y, float width) {
        Label shadow = theme.label(value, theme.small, new Color(0, 0, 0, .92f));
        shadow.setBounds(x + 1.5f, y - 1.5f, width, 30);
        shadow.setTouchable(Touchable.disabled);
        stage.addActor(shadow);
        Label label = theme.label(value, theme.small, Color.WHITE);
        label.setBounds(x, y, width, 30);
        label.setTouchable(Touchable.disabled);
        stage.addActor(label);
    }

    private void updateSelectionMarker() {
        Actor focus = stage.getKeyboardFocus();
        boolean visible = activeDialog == null && focus instanceof Button && mainButtons.contains((Button) focus, true);
        selectionMarker.setVisible(visible);
        if (visible) selectionMarker.setPosition(focus.getX() - selectionMarker.getWidth() * .45f,
            focus.getTop() - selectionMarker.getHeight() * .2f);
    }

    private Button mainButton(String name, Rectangle bounds, Runnable action, boolean keyboardSelectable) {
        Button button = imageButton(name, bounds, action, keyboardSelectable, bounds.height / 2);
        stage.addActor(button);
        if (!keyboardSelectable) stage.addActor(buttonAccent.forButton(button));
        return button;
    }

    private Button imageButton(String name, Rectangle bounds, Runnable action, boolean keyboardSelectable, float radius) {
        Button.ButtonStyle style = new Button.ButtonStyle();
        if (keyboardSelectable) {
            style.over = theme.roundedOverlay(new Color(1, 1, 1, .12f), radius);
            style.focused = style.over;
        }
        style.down = theme.roundedOverlay(new Color(.35f, .18f, 0, .16f), radius);
        Button button = new Button(style);
        button.setName(name);
        button.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        bindAction(button, action, keyboardSelectable);
        return button;
    }

    private void bindAction(final Button button, final Runnable action, final boolean keyboardSelectable) {
        button.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1 && keyboardSelectable) stage.setKeyboardFocus(button);
            }
        });
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
    }

    private DialogArtwork.Page beginDialog(String pageName, Runnable back) {
        beginModal(pageName, back);
        DialogArtwork.Page page = dialogs.page(pageName);
        dialogPanel = new Group();
        dialogPanel.setBounds((WIDTH - page.width) / 2, (HEIGHT - page.height) / 2, page.width, page.height);
        dialogPanel.setTouchable(Touchable.childrenOnly);
        dialogPanel.addActor(page.image());
        activeDialog.addActor(dialogPanel);
        return page;
    }

    private void beginModal(String name, Runnable back) {
        audio.playWindow();
        aboutPageIndex = -1;
        if (activeDialog != null) activeDialog.remove();
        dialogButtons.clear();
        optionSoundOffIndicator = null;
        backAction = back;
        activeDialog = new Group();
        activeDialog.setName(name);
        activeDialog.setSize(WIDTH, HEIGHT);
        Image backdrop = new Image(theme.overlay(new Color(.10f, .22f, .17f, .56f)));
        backdrop.setBounds(0, 0, WIDTH, HEIGHT);
        // A full-stage hit target prevents the title menu receiving clicks through the page.
        backdrop.setTouchable(Touchable.enabled);
        activeDialog.addActor(backdrop);
        stage.addActor(activeDialog);
        Gdx.app.log("TitleScreen", "Opened " + name);
    }

    private void dialogButton(DialogArtwork.Page page, String name, Runnable action) {
        Button button = imageButton(name, page.buttonBounds(name), action, true, page.buttonRadius(name));
        dialogButtons.add(button);
        dialogPanel.addActor(button);
        if (!page.hasPaintedButtonAccent()) dialogPanel.addActor(buttonAccent.forButton(button));
    }

    private void focusDialog() {
        if (dialogButtons.notEmpty()) stage.setKeyboardFocus(dialogButtons.first());
    }

    private void closeDialog() {
        aboutPageIndex = -1;
        if (activeDialog != null) {
            audio.playWindow();
            Gdx.app.log("TitleScreen", "Closed " + activeDialog.getName());
            activeDialog.remove();
        }
        activeDialog = null;
        dialogPanel = null;
        optionSoundOffIndicator = null;
        backAction = null;
        dialogButtons.clear();
        stage.setKeyboardFocus(mainButtons.first());
    }

    private void showOptions() {
        showOptions(true);
    }

    private void showOptions(boolean animate) {
        DialogArtwork.Page page = beginDialog("options", this::closeDialog);
        optionSoundOffIndicator = indicator(page.soundIndicatorBounds(), page.soundOffDrawable());
        dialogPanel.addActor(optionSoundOffIndicator);
        dialogButton(page, "controls", () -> showControls(false));
        dialogButton(page, "sound", this::toggleSound);
        dialogButton(page, "mainMenu", this::closeDialog);
        dialogButton(page, "quit", this::showExitConfirmation);
        dialogButton(page, "close", this::closeDialog);
        focusDialog();
        if (animate) PopupAnimation.open(dialogPanel);
    }

    private void showStartHelp() {
        showControls(true);
    }

    private void showControls(boolean starting) {
        Runnable back = starting ? this::closeDialog : () -> showOptions(false);
        DialogArtwork.Page page = beginDialog(starting ? "controls" : "manual", back);
        if (starting) {
            Runnable start = onStart == null ? this::showComingSoon : () -> Gdx.app.postRunnable(onStart);
            dialogButton(page, "start", start);
        }
        dialogButton(page, "back", back);
        if (!starting) dialogButton(page, "close", back);
        focusDialog();
        if (starting) PopupAnimation.open(dialogPanel);
    }

    private void showAbout() {
        showAbout(0);
        PopupAnimation.open(dialogPanel);
    }

    private void showAbout(int index) {
        final int current = Math.floorMod(index, ABOUT_PAGES.length);
        DialogArtwork.Page page = beginDialog(ABOUT_PAGES[current], this::closeDialog);
        aboutPageIndex = current;
        dialogButton(page, "previous", () -> showAbout(current - 1));
        dialogButton(page, "next", () -> showAbout(current + 1));
        dialogButton(page, "close", this::closeDialog);
        if (current == ABOUT_PAGES.length - 1) dialogButton(page, "source", () -> Gdx.net.openURI(ABOUT_VIDEO_URL));
        stage.setKeyboardFocus(dialogPanel.findActor("next"));
    }

    private void showExitConfirmation() {
        Runnable cancel = activeDialog == null ? this::closeDialog : () -> showOptions(false);
        beginModal("exit-confirm", cancel);
        ExitConfirmation confirmation = new ExitConfirmation(theme, dialogs.page("exit-confirm"), buttonAccent, () -> {
            Gdx.app.log("TitleScreen", "Exit confirmed");
            Gdx.app.exit();
        }, cancel);
        dialogPanel = confirmation;
        dialogButtons.add(confirmation.yes);
        dialogButtons.add(confirmation.no);
        dialogButtons.add(confirmation.close);
        activeDialog.addActor(dialogPanel);
        PopupAnimation.open(dialogPanel);
        stage.setKeyboardFocus(confirmation.no);
    }

    private void showComingSoon() {
        closeDialog();
        Dialog dialog = new Dialog("", new Window.WindowStyle(theme.small, MenuTheme.INK,
            theme.panel(MenuTheme.CREAM, MenuTheme.GREEN, 8)));
        dialog.getContentTable().pad(36);
        dialog.getContentTable().add(theme.label("모험을 준비하고 있어요", theme.heading, MenuTheme.INK)).row();
        TextButton back = new TextButton("시작 화면으로", theme.buttonStyle(true));
        bindAction(back, this::closeDialog, true);
        dialog.getContentTable().add(back).size(576, 56).padTop(24);
        dialogButtons.add(back);
        activeDialog = dialog;
        dialogPanel = dialog;
        backAction = this::closeDialog;
        dialog.show(stage);
        audio.playWindow();
        focusDialog();
    }

    private Image indicator(Rectangle bounds, Drawable drawable) {
        Image image = new Image(drawable);
        image.setScaling(Scaling.stretch);
        image.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        image.setTouchable(Touchable.disabled);
        image.setVisible(!audio.isEnabled());
        return image;
    }

    private void toggleSound() {
        audio.toggleEnabled();
        soundMuteIndicator.setVisible(!audio.isEnabled());
        if (optionSoundOffIndicator != null) optionSoundOffIndicator.setVisible(!audio.isEnabled());
    }

    @Override
    public void show() {
        closeDialog();
        soundMuteIndicator.setVisible(!audio.isEnabled());
        heldKeys.clear();
        for (int key : MENU_KEYS) if (Gdx.input.isKeyPressed(key)) heldKeys.add(key);
        Gdx.input.setInputProcessor(stage);
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, true);
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, false);
        heldKeys.clear();
    }

    @Override
    public void render(float delta) {
        for (int key : MENU_KEYS) if (!Gdx.input.isKeyPressed(key)) heldKeys.remove(key);
        ScreenUtils.clear(MenuTheme.INK);
        stage.getViewport().apply();
        stage.act(Math.min(delta, .05f));
        updateSelectionMarker();
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
        artwork.dispose();
        dialogs.dispose();
        buttonAccent.dispose();
        theme.dispose();
    }
}
