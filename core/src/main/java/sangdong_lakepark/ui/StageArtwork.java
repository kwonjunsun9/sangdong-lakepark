package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import sangdong_lakepark.world.PlatformMap;
import sangdong_lakepark.world.StageRun;
import sangdong_lakepark.world.StageRocks;
import sangdong_lakepark.world.HardBoxes.Vehicle;

/** Painted scenery and repeating grass/soil tiles remain separate from collision geometry. */
public class StageArtwork implements Disposable {
    private static final Color RIVER_DEEP = Color.valueOf("176AAB"), RIVER_TOP = Color.valueOf("30AFDB");
    private static final float GOAL_SIGN_WIDTH = 210, GOAL_SIGN_GROUND_OVERLAP = 3;
    private final Texture background;
    private final Texture terrain = new Texture(Gdx.files.internal("sangdong/easy/ground-texture.png"));
    private final Texture birdAtlas = new Texture(Gdx.files.internal("sangdong/easy/bird-flight-atlas.png"), true);
    private final Texture cookie, effects, goalTexture;
    private final TextureRegion cookieImage, goalSign;
    protected final TextureRegion rollingRock;
    private final TextureRegion fallingRock, dust;
    private final TextureRegion[][] birdFrames = TextureRegion.split(birdAtlas, birdAtlas.getWidth() / 3, birdAtlas.getHeight() / 2);
    protected final ShapeRenderer shapes = new ShapeRenderer();
    protected final ItemSprites items = new ItemSprites();

    public StageArtwork() { this("sangdong/easy/meadow-background.png"); }

    protected StageArtwork(String backgroundPath) {
        background = new Texture(Gdx.files.internal(backgroundPath));
        background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        terrain.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        terrain.setWrap(Texture.TextureWrap.MirroredRepeat, Texture.TextureWrap.ClampToEdge);
        birdAtlas.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        Pixmap pixels = new Pixmap(Gdx.files.internal("sangdong/easy/cookie.png"));
        try {
            cookie = new Texture(pixels);
            cookie.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            int left = pixels.getWidth(), top = pixels.getHeight(), right = 0, bottom = 0;
            for (int y = 0; y < pixels.getHeight(); y++) for (int x = 0; x < pixels.getWidth(); x++) {
                if ((pixels.getPixel(x, y) & 255) < 32) continue;
                left = Math.min(left, x); top = Math.min(top, y);
                right = Math.max(right, x); bottom = Math.max(bottom, y);
            }
            cookieImage = new TextureRegion(cookie, left, top, right - left + 1, bottom - top + 1);
        } finally { pixels.dispose(); }
        Pixmap atlas = new Pixmap(Gdx.files.internal("sangdong/easy/rocks-dust-atlas.png"));
        try {
            effects = new Texture(atlas, true);
            effects.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            int cell = atlas.getWidth() / 3;
            rollingRock = SpriteRegions.crop(atlas, effects, 0, 0, cell, atlas.getHeight());
            fallingRock = SpriteRegions.crop(atlas, effects, cell, 0, cell, atlas.getHeight());
            dust = SpriteRegions.crop(atlas, effects, cell * 2, 0, cell, atlas.getHeight());
        } finally { atlas.dispose(); }
        Pixmap goalPixels = new Pixmap(Gdx.files.internal("sangdong/goal/park-sign.png"));
        try {
            goalTexture = new Texture(goalPixels, true);
            goalTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            goalSign = SpriteRegions.crop(goalPixels, goalTexture, 0, 0, goalPixels.getWidth(), goalPixels.getHeight());
        } finally { goalPixels.dispose(); }
    }

    public void drawBackground(SpriteBatch batch, float cameraX) { drawBackground(batch,cameraX,PlatformMap.WORLD_WIDTH); }

    public void drawBackground(SpriteBatch batch,float cameraX,float worldWidth) {
        float width = PlatformMap.VIEW_WIDTH + 160;
        float height = width * background.getHeight() / background.getWidth();
        float progress = (cameraX - PlatformMap.VIEW_WIDTH / 2) / (worldWidth - PlatformMap.VIEW_WIDTH);
        batch.draw(background, -160 * MathUtils.clamp(progress, 0, 1), 0, width, height);
    }

    public void drawTerrain(SpriteBatch batch, float cameraX, PlatformMap map) {
        float left = cameraX - PlatformMap.VIEW_WIDTH / 2, right = left + PlatformMap.VIEW_WIDTH;
        for (Rectangle ground : map.ground) {
            if (!drawStandardGround(ground)) continue;
            float start = Math.max(left, ground.x), end = Math.min(right, ground.x + ground.width);
            if (end <= start) continue;
            float top = ground.y + ground.height;
            float radius = PlatformMap.cornerRadius(ground);
            float cap = radius == 0 ? PlatformMap.GRASS_HEIGHT : radius;
            float soilHeight = terrain.getHeight() - PlatformMap.GRASS_HEIGHT;
            float y = top - cap, offset = Math.max(0, cap - PlatformMap.GRASS_HEIGHT);
            while (y > ground.y) {
                float height = Math.min(y - ground.y, soilHeight - offset);
                batch.draw(terrain, start, y - height, end - start, height,
                    start / terrain.getWidth(), (PlatformMap.GRASS_HEIGHT + offset + height) / terrain.getHeight(),
                    end / terrain.getWidth(), (PlatformMap.GRASS_HEIGHT + offset) / terrain.getHeight());
                y -= height; offset = 0;
            }
            // Only top corners are rounded; sample the same texture in thin cap strips.
            for (float row = 0; row < cap; row++) {
                float distance = radius - row - .5f;
                float inset = radius == 0 ? 0 : radius - (float) Math.sqrt(Math.max(0, radius * radius - distance * distance));
                float x0 = Math.max(start, ground.x + inset), x1 = Math.min(end, ground.x + ground.width - inset);
                if (x1 <= x0) continue;
                batch.draw(terrain, x0, top - row - 1, x1 - x0, 1,
                    x0 / terrain.getWidth(), (row + 1) / terrain.getHeight(), x1 / terrain.getWidth(), row / terrain.getHeight());
            }
        }
    }

    public void drawCoins(SpriteBatch batch, PlatformMap map, StageRun run) {
        for (int i = 0; i < map.coins.size; i++) {
            if (run.isCollected(i)) continue;
            Rectangle coin = map.coins.get(i);
            batch.draw(cookieImage, coin.x, coin.y, coin.width, coin.height);
        }
    }

    public void drawHazards(Matrix4 projection, PlatformMap map, StageRun run) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection); shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (PlatformMap.Gap gap : map.gaps) if (gap.lake) drawRiver(gap, run.getElapsed());
        for (PlatformMap.Block block : map.blocks) {
            if (!drawStandardBlock(block)) continue;
            if(block.kind==PlatformMap.BlockKind.CONVEYOR)continue;
            boolean item = block == map.itemBlock;
            if (item && !run.isItemUsed()) { drawQuestionBox(block.x, block.y, true); continue; }
            drawBlock(block.x, block.y, block.kind == PlatformMap.BlockKind.STONE ? "8B9194" : "9C713E", 1);
        }
        for (PlatformMap.BlinkPlatform row : map.blinking) for (float x = row.x; x < row.x + row.width; x += PlatformMap.BLOCK_SIZE)
            drawBlock(x, row.y, "9378C9", row.opacity(run.getElapsed()));
        for (Rectangle trap : map.traps) {
            float x = trap.x - 8, y = trap.y, width = trap.width + 16;
            shapes.setColor(Color.valueOf("6D4432")); shapes.ellipse(x - 4, y - 5, width + 8, 16, 24);
            shapes.setColor(Color.valueOf("56656B")); shapes.rect(x, y + 1, width, 7);
            int teeth = Math.max(5, (int) (width / 13));
            for (int i = 0; i < teeth; i++) {
                float tooth = x + i * width / teeth;
                shapes.setColor(Color.valueOf("4A555C")); shapes.triangle(tooth, y + 7, tooth + 6, y + 29, tooth + 13, y + 7);
                shapes.setColor(Color.valueOf("E3E9DF")); shapes.triangle(tooth + 3, y + 9, tooth + 6, y + 24, tooth + 10, y + 9);
            }
        }
        for (PlatformMap.WaterStream stream : map.waterStreams) drawWater(stream, run.getElapsed());
        for (StageRocks.Rock rock : run.rocks.all) {
            if (rock.phase == StageRocks.Phase.READY || rock.phase == StageRocks.Phase.GONE) continue;
            if (rock.phase == StageRocks.Phase.SHATTERING) {
                float t = rock.breakAge(), alpha = Math.max(0, 1 - t / PlatformMap.ROCK_BREAK_TIME);
                float centerX = rock.bounds.x + rock.bounds.width / 2, baseY = rock.bounds.y + 16;
                for (int i = 0; i < 6; i++) {
                    float shardX = centerX + (i - 2.5f) * 42 * t;
                    float shardY = baseY + (140 + (i % 3) * 35) * t - PlatformMap.GRAVITY * t * t / 2;
                    shapes.setColor(.6f, .58f, .52f, alpha);
                    shapes.triangle(shardX - 8, shardY - 4, shardX + 7, shardY - 6, shardX + 2, shardY + 10);
                }
                shapes.setColor(.55f, .43f, .29f, alpha * .45f);
                shapes.ellipse(centerX - 45 - t * 50, rock.bounds.y - 4, 90 + t * 100, 18 + t * 24, 24);
            }
        }
        for (StageRocks.Bird bird : run.rocks.birds) if (bird.warning()) {
            float x = bird.spawn.x;
            shapes.setColor(0, 0, 0, .35f + .15f * MathUtils.sin((float) run.getElapsed() * 20));
            shapes.ellipse(x - 36, map.groundTop(x), 72, 12, 24);
        }
        shapes.end(); Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void drawBirds(SpriteBatch batch, StageRocks rocks) {
        float size = PlatformMap.BIRD_DRAW_SIZE;
        for (StageRocks.Bird bird : rocks.birds) if (bird.visible())
            batch.draw(birdFrames[bird.spawn.rolls ? 1 : 0][bird.wingFrame()],
                bird.x - size / 2, PlatformMap.BIRD_Y - size / 2, size, size);
    }

    public void drawRockSprites(SpriteBatch batch, StageRocks rocks) {
        for (StageRocks.Rock rock : rocks.all) {
            if (rock.phase != StageRocks.Phase.CARRIED && rock.phase != StageRocks.Phase.FALLING
                && rock.phase != StageRocks.Phase.ROLLING) continue;
            TextureRegion image = rock.phase == StageRocks.Phase.ROLLING ? rollingRock : fallingRock;
            batch.draw(image, rock.bounds.x, rock.bounds.y, rock.bounds.width / 2, rock.bounds.height / 2,
                rock.bounds.width, rock.bounds.height, 1, 1, rock.rotation * MathUtils.radiansToDegrees);
        }
    }

    public void drawBicycleTrail(SpriteBatch batch, EasyBicycleTrail trail) { trail.draw(batch, dust); }

    private void drawRiver(PlatformMap.Gap river, double time) {
        float base = PlatformMap.RIVER_LEVEL - PlatformMap.RIVER_AMPLITUDE - 12, end = river.x + river.width;
        shapes.rect(river.x, 0, river.width, base, RIVER_DEEP, RIVER_DEEP, RIVER_TOP, RIVER_TOP);
        for (float x = river.x; x < end; x += 8) {
            float next = Math.min(x + 8, end);
            float y = PlatformMap.riverSurface(x, time), nextY = PlatformMap.riverSurface(next, time);
            shapes.setColor(RIVER_TOP);
            shapes.triangle(x, base, next, base, x, y);
            shapes.triangle(next, base, next, nextY, x, y);
            shapes.setColor(.72f, .96f, 1, .8f); shapes.rectLine(x, y, next, nextY, 2);
        }
        // Short drifting ripples follow the same wave instead of a fixed row of white wedges.
        shapes.setColor(.75f, .96f, 1, .45f);
        for (int row = 0; row < 3; row++) for (float start = river.x - 146; start < end; start += 146) {
            float drift = (float) ((time * 24 + row * 47) % 146);
            for (int part = 0; part < 5; part++) {
                float x = start + drift + part * 7, next = x + 7;
                if (x < river.x || next > end) continue;
                float y = PlatformMap.riverSurface(x, time) - 20 - row * 28;
                float nextY = PlatformMap.riverSurface(next, time) - 20 - row * 28;
                shapes.rectLine(x, y, next, nextY, 1.5f);
            }
        }
    }

    protected void drawBlock(float x, float y, String color, float alpha) {
        if (alpha <= 0) return;
        Color fill = Color.valueOf(color);
        shapes.setColor(fill.r * .6f, fill.g * .6f, fill.b * .6f, alpha); shapes.rect(x, y, 64, 64);
        shapes.setColor(fill.r, fill.g, fill.b, alpha); shapes.rect(x + 4, y + 4, 56, 56);
        shapes.setColor(1, 1, .9f, alpha * .55f); shapes.rect(x + 8, y + 53, 48, 3);
        shapes.setColor(.2f, .2f, .17f, alpha * .65f);
        shapes.circle(x + 10, y + 10, 2, 8); shapes.circle(x + 54, y + 10, 2, 8);
    }

    private void drawWater(PlatformMap.WaterStream water, double time) {
        float center = water.x + water.width / 2;
        boolean warning = water.warning(time), active = water.active(time);
        if (active) {
            shapes.setColor(.38f, .8f, 1, .65f); shapes.rect(water.x, water.y, water.width, water.height);
            shapes.setColor(.8f, .96f, 1, .85f);
            for (int i = 0; i < 5; i++) shapes.rect(water.x + 4 + i * 9, water.y, 2, water.height);
            shapes.ellipse(water.x - 16, water.y - 1, water.width + 32, 16, 24);
        } else if (warning) {
            shapes.setColor(.45f, .78f, 1, .8f);
            for (int i = 0; i < 6; i++) {
                float y = water.y + water.height - 24 - ((float) time * 180 + i * 32) % 150;
                shapes.ellipse(center - 16 + (i % 3) * 14, y, 4, 10, 12);
            }
        }
        float shake = warning ? MathUtils.sin((float) time * 35) * 4 : 0;
        shapes.setColor(Color.valueOf(active ? "687889" : "8C98A6"));
        shapes.ellipse(center - 60 + shake, PlatformMap.WATER_CLOUD_Y - 5, 120, 38, 24);
        shapes.circle(center - 25 + shake, PlatformMap.WATER_CLOUD_Y + 12, 18, 24);
        shapes.circle(center + 12 + shake, PlatformMap.WATER_CLOUD_Y + 18, 22, 24);
    }

    protected void drawQuestionBox(float x, float y, boolean brown) {
        shapes.setColor(Color.valueOf(brown?"50321F":"3B304D"));shapes.rect(x,y,64,64);
        shapes.setColor(Color.valueOf(brown?"B0743E":"8D51C5"));shapes.rect(x+4,y+4,56,56);
        shapes.setColor(Color.valueOf(brown?"DAA569":"BF8CE8"));shapes.rect(x+7,y+54,50,4);
        shapes.setColor(Color.valueOf(brown?"50321F":"3B304D"));
        for(float cornerX:new float[]{8,52})for(float cornerY:new float[]{8,52})shapes.rect(x+cornerX,y+cornerY,3,3);
        shapes.setColor(Color.valueOf("FFE86B"));
        String[] rows={"01110","11011","00011","00110","00100","00000","00100"};
        for(int row=0;row<7;row++)for(int col=0;col<5;col++)if(rows[row].charAt(col)=='1')shapes.rect(x+17+col*6,y+11+(6-row)*6,6,6);
    }

    public void drawItems(SpriteBatch batch, PlatformMap map, StageRun run) {
        if (run.isBicycleAvailable()) {
            float pop = MathUtils.sin((1 - run.getBikePop()) * MathUtils.PI) * 18;
            items.drawVehicle(batch, map.usesScooter() ? Vehicle.SCOOTER : Vehicle.BICYCLE,
                map.bicycle.x, map.bicycle.y + pop, map.bicycle.width, map.bicycle.height);
        }
    }

    protected boolean drawStandardGround(Rectangle ground) { return true; }
    protected boolean drawStandardBlock(PlatformMap.Block block) { return true; }
    public void drawBehindTerrain(Matrix4 projection,PlatformMap map,StageRun run) { }
    public void drawScreenEffects(Matrix4 projection,StageRun run) { }
    public void drawHud(SpriteBatch batch,StageRun run,BitmapFont font) { }
    public TextureRegion ridingSprite(StageRun run) { return ridingSprite(); }
    public void drawRidingAccessory(Matrix4 projection, StageRun run) { }
    public TextureRegion ridingSprite() { return null; }
    public void drawAdditionalSprites(SpriteBatch batch, StageRun run) { }

    public void drawDebug(Matrix4 projection, PlatformMap map, StageRun run) {
        shapes.setProjectionMatrix(projection); shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.YELLOW);
        for (Rectangle area : map.ground) shapes.rect(area.x, area.y, area.width, area.height);
        shapes.setColor(Color.CYAN);
        for (Rectangle area : map.blocks) shapes.rect(area.x, area.y, area.width, area.height);
        for (PlatformMap.BlinkPlatform area : map.blinking) if (area.solid(run.getElapsed())) shapes.rect(area.x, area.y, area.width, area.height);
        shapes.setColor(Color.RED);
        for (Rectangle area : map.traps) shapes.rect(area.x, area.y, area.width, area.height);
        for (PlatformMap.WaterStream water : map.waterStreams)
            if (water.active(run.getElapsed())) shapes.rect(water.x, water.y, water.width, water.height);
        for (StageRocks.Rock rock : run.rocks.all) if (rock.dangerous()) shapes.rect(rock.bounds.x, rock.bounds.y, rock.bounds.width, rock.bounds.height);
        shapes.setColor(Color.WHITE); shapes.rect(run.player.x, run.player.y, run.player.width, run.player.height);
        shapes.setColor(Color.ORANGE); shapes.line(run.player.x + (run.player.width - PlatformMap.FOOT_WIDTH) / 2, run.player.y,
            run.player.x + (run.player.width + PlatformMap.FOOT_WIDTH) / 2, run.player.y);
        shapes.setColor(Color.GREEN);
        for (int i = 0; i < map.coins.size; i++) if (!run.isCollected(i)) {
            Rectangle area = map.coins.get(i); shapes.rect(area.x, area.y, area.width, area.height);
        }
        if (run.isBicycleAvailable()) shapes.rect(map.bicycle.x, map.bicycle.y, map.bicycle.width, map.bicycle.height);
        shapes.line(map.stopX(), PlatformMap.GROUND_Y, map.stopX(), PlatformMap.VIEW_HEIGHT);
        shapes.end();
    }

    public void drawDamageFeedback(Matrix4 projection, float intensity, float width, float height) {
        if (intensity <= 0) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection); shapes.begin(ShapeRenderer.ShapeType.Filled);
        float feather = Math.min(width, height) * .12f, band = feather / 32;
        for (int i = 0; i < 32; i++) {
            float inset = i * band, alpha = 1 - i / 32f;
            shapes.setColor(.95f, .14f, .14f, intensity * .28f * alpha);
            shapes.rect(inset, inset, band, height - inset * 2);
            shapes.rect(width - inset - band, inset, band, height - inset * 2);
            shapes.rect(inset + band, inset, width - (inset + band) * 2, band);
            shapes.rect(inset + band, height - inset - band, width - (inset + band) * 2, band);
        }
        shapes.end(); Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void drawGoal(SpriteBatch batch, PlatformMap map, BitmapFont font) {
        float height = GOAL_SIGN_WIDTH * goalSign.getRegionHeight() / goalSign.getRegionWidth();
        float center = map.arch.x + map.arch.width / 2;
        batch.draw(goalSign, center - GOAL_SIGN_WIDTH / 2, map.arch.y - GOAL_SIGN_GROUND_OVERLAP, GOAL_SIGN_WIDTH, height);
    }

    @Override public void dispose() { background.dispose(); terrain.dispose(); birdAtlas.dispose(); cookie.dispose(); effects.dispose(); goalTexture.dispose(); shapes.dispose(); items.dispose(); }
}
