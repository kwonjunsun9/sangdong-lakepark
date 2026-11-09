package sangdong_lakepark.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/** One attempt owns physics, equipment, timed platforms, ambushes, score and lives. */
public class StageRun {
    public static final int MAX_LIVES = 5;
    public final Rectangle player = new Rectangle(0, 0, PlatformMap.PLAYER_WIDTH, PlatformMap.PLAYER_HEIGHT);
    public final StageRocks rocks;
    protected final PlatformMap map;
    private final boolean[] collected;
    protected float velocityY, invulnerability, damageFeedback, borderFeedback, knockbackTime, knockbackDirection, bikePop;
    private double elapsed;
    private int score, lives, falls, bicycleMounts;
    protected int jumps, damageCount;
    private int jumpCount, bumpCount, pickupCount, waterEntries;
    protected boolean arriving, grounded, cleared, finished, facingLeft, failed, bicycle, itemUsed, bicycleAvailable;

    public StageRun(PlatformMap map) {
        this.map = map; rocks = new StageRocks(map); collected = new boolean[map.coins.size]; resetState();
    }
    public void reset() { resetState(); onReset(); }
    private void resetState() {
        player.setPosition(PlatformMap.START_X, PlatformMap.VIEW_HEIGHT + 24);
        java.util.Arrays.fill(collected, false); rocks.reset();
        velocityY = invulnerability = damageFeedback = borderFeedback = knockbackTime = bikePop = 0;
        elapsed = 0; score = jumps = falls = bicycleMounts = 0; lives = MAX_LIVES; arriving = true;
        jumpCount = damageCount = bumpCount = pickupCount = waterEntries = 0;
        grounded = cleared = finished = facingLeft = failed = bicycle = itemUsed = bicycleAvailable = false;
    }
    public void jumpToSection(int section) {
        reset(); Vector2 start = map.sectionStarts[MathUtils.clamp(section, 0, map.sectionStarts.length - 1)];
        player.setPosition(start); arriving = section == 0;
    }
    /** Call only during active gameplay or to fade a finished attempt's hit feedback. */
    public void update(float delta, float direction, boolean jump) {
        damageFeedback = Math.max(0, damageFeedback - Math.max(0, delta));
        borderFeedback = Math.max(0, borderFeedback - Math.max(0, delta));
        if (finished) return;
        if (!cleared) elapsed += Math.max(0, delta);
        if (arriving || cleared) { direction = 0; jump = false; }
        float remaining = MathUtils.clamp(delta, 0, .25f);
        while (remaining > 0) {
            float dt = Math.min(remaining, 1f / 120f);
            boolean wasCleared = cleared;
            step(dt, MathUtils.clamp(direction, -1, 1), jump);
            jump = false; remaining -= dt;
            if (finished || !wasCleared && cleared) break;
        }
    }
    private void step(float dt, float direction, boolean jump) {
        if (!cleared) beforeStep(dt);
        direction = adjustedDirection(direction);
        jump = jump && allowsJump();
        invulnerability = Math.max(0, invulnerability - dt);
        knockbackTime = Math.max(0, knockbackTime - dt);
        bikePop = Math.max(0, bikePop - dt);
        if (cleared) { direction = 1; jump = false; }
        else rocks.update(dt, player.x + player.width / 2);
        if (!cleared && enterRiver()) return;
        if (!cleared && !arriving) {
            for (Rectangle trap : map.traps) if (player.overlaps(trap)) hit();
            for (PlatformMap.WaterStream water : map.waterStreams)
                if (water.active(elapsed) && player.overlaps(water)) hit();
            for (StageRocks.Rock rock : rocks.all) if (rock.dangerous() && player.overlaps(rock.bounds)) {
                hit(); rock.shatter();
            }
        }
        if (failed) return;
        if (knockbackTime > 0) { direction = 0; jump = false; }
        else if (direction != 0) facingLeft = direction < 0;
        if (jump && jumps < maximumJumps()) {
            velocityY = PlatformMap.JUMP_SPEED;
            jumps++; grounded = false;
            jumpCount++;
        }
        if (!grounded && velocityY < 0 && jumps == 0) jumps = 1;
        boolean wasGrounded = grounded;
        float dx = (knockbackTime > 0 ? knockbackDirection * PlatformMap.HIT_SPEED
            : cleared ? direction * PlatformMap.EXIT_SPEED : movementVelocity(dt, direction)) * dt;
        float previousX = player.x;
        player.x = MathUtils.clamp(player.x + dx, 0,
            cleared ? map.worldWidth() + player.width : map.worldWidth() - player.width);
        for (Rectangle ground : map.ground) resolveSide(ground, dx, previousX, true);
        for (PlatformMap.Block block : map.blocks) resolveSide(block, dx, previousX, false);
        for (PlatformMap.BlinkPlatform block : map.blinking) if (block.solid(elapsed)) resolveSide(block, dx, previousX, false);
        float previousBottom = player.y;
        velocityY = Math.max(velocityY - PlatformMap.GRAVITY * dt, -900);
        player.y += velocityY * dt; grounded = false;
        for (PlatformMap.Block block : map.blocks) resolveHead(block, previousBottom);
        for (Rectangle ceiling : map.ceilings) resolveHead(ceiling, previousBottom);
        for (PlatformMap.BlinkPlatform block : map.blinking) if (block.solid(elapsed)) resolveHead(block, previousBottom);
        landOn(map.ground, previousBottom, true, wasGrounded); landOn(map.blocks, previousBottom, false, wasGrounded);
        for (PlatformMap.BlinkPlatform block : map.blinking) if (block.solid(elapsed)) landOn(block, previousBottom, false, wasGrounded);
        if (grounded) arriving = false;
        if (!cleared && enterRiver()) return;
        if (!cleared && player.y < PlatformMap.FALL_Y) { fall(map.gapAt(player.x + player.width / 2)); return; }
        if (!cleared) {
            if (bicycleAvailable && player.overlaps(map.bicycle)) {
                bicycle = true; bicycleAvailable = false; bicycleMounts++;
            }
            for (int i = 0; i < collected.length; i++) if (!collected[i] && canCollectCoin(i) && player.overlaps(map.coins.get(i))) {
                collected[i] = true; score++;
            }
            if (player.x >= map.stopX()) {
                cleared = true; knockbackTime = velocityY = 0;
                player.setPosition(map.stopX(), PlatformMap.GROUND_Y); grounded = true;
            }
        }
        afterMovement(dt);
        if (cleared && player.x >= map.worldWidth() + player.width) finished = true;
    }
    private void resolveHead(Rectangle block, float previousBottom) {
        if (velocityY > 0 && previousBottom + player.height <= block.y + .1f
            && player.y + player.height >= block.y && headOverlaps(block)) {
            player.y = block.y - player.height; velocityY = 0;
            if (onHeadHit(block)) return;
            if (block.y == map.itemBlock.y && player.x + player.width > map.itemBlock.x - PlatformMap.ITEM_HIT_MARGIN
                && player.x < map.itemBlock.x + map.itemBlock.width + PlatformMap.ITEM_HIT_MARGIN
                && !bicycle && !bicycleAvailable) {
                itemUsed = bicycleAvailable = true; bikePop = PlatformMap.BIKE_POP_TIME;
                pickupCount++;
            } else bumpCount++;
        }
    }
    private void resolveSide(Rectangle surface, float dx, float previousX, boolean rounded) {
        if (!player.overlaps(surface)) return;
        float before = player.x;
        float sideTop = surface.y + surface.height - (rounded ? PlatformMap.cornerRadius(surface) : 0);
        // Feet may miss a ledge while the wider body is already over its edge.
        // Recover that overlap before an inward move; keep outward movement and rounded cap support free.
        if (dx > 0 && (previousX + player.width <= surface.x + .1f
            || previousX < surface.x && player.y < sideTop)) player.x = surface.x - player.width;
        else if (dx < 0 && (previousX >= surface.x + surface.width - .1f
            || previousX + player.width > surface.x + surface.width && player.y < sideTop))
            player.x = surface.x + surface.width;
        if (before != player.x) onHorizontalBlock();
    }
    private boolean overlapsX(Rectangle surface) {
        return player.x + player.width > surface.x && player.x < surface.x + surface.width;
    }
    private void landOn(Array<? extends Rectangle> surfaces, float previousBottom, boolean rounded, boolean wasGrounded) {
        for (Rectangle surface : surfaces) landOn(surface, previousBottom, rounded, wasGrounded);
    }
    private void landOn(Rectangle surface, float previousBottom, boolean rounded, boolean wasGrounded) {
        float top = surface.y + surface.height;
        float footLeft = player.x + (player.width - PlatformMap.FOOT_WIDTH) / 2;
        float from = Math.max(footLeft, surface.x);
        float to = Math.min(footLeft + PlatformMap.FOOT_WIDTH, surface.x + surface.width);
        if (to - from < PlatformMap.MIN_FOOT_SUPPORT) return;
        if (rounded) {
            float contactX = MathUtils.clamp(surface.x + surface.width / 2,
                from + PlatformMap.MIN_FOOT_SUPPORT / 2, to - PlatformMap.MIN_FOOT_SUPPORT / 2);
            float radius = PlatformMap.cornerRadius(surface);
            float edge = Math.min(contactX - surface.x, surface.x + surface.width - contactX);
            if (edge < radius) top -= radius - (float) Math.sqrt(radius * radius - (radius - edge) * (radius - edge));
        }
        // A rising rounded edge can meet descending feet between two horizontal positions.
        boolean followCorner = rounded && (wasGrounded || top < surface.y + surface.height)
            && previousBottom >= surface.y + surface.height - PlatformMap.cornerRadius(surface)
            && top - previousBottom <= PlatformMap.cornerRadius(surface);
        if (velocityY <= 0 && (previousBottom >= top - .1f || followCorner) && player.y <= top) {
            player.y = top; velocityY = 0; grounded = true; jumps = 0;
        }
    }
    protected void hit() {
        if (invulnerability > 0 || failed) return;
        damageCount++;
        borderFeedback = PlatformMap.BORDER_FLASH;
        if (bicycle) { bicycle = false; itemUsed = false; invulnerability = PlatformMap.INVULNERABILITY; }
        else loseLife();
        velocityY = PlatformMap.HIT_JUMP; grounded = false;
        knockbackTime = PlatformMap.HIT_LOCK; knockbackDirection = facingLeft ? 1 : -1;
    }
    protected void loseLife() {
        lives--; invulnerability = PlatformMap.INVULNERABILITY;
        damageFeedback = PlatformMap.HIT_FLASH; borderFeedback = PlatformMap.BORDER_FLASH;
        if (lives == 0) failed = finished = true;
    }
    private boolean enterRiver() {
        float footLeft = player.x + (player.width - PlatformMap.FOOT_WIDTH) / 2;
        for (PlatformMap.Gap gap : map.gaps) if (gap.lake) {
            float from = Math.max(footLeft, gap.x), to = Math.min(footLeft + PlatformMap.FOOT_WIDTH, gap.x + gap.width);
            if (to - from >= PlatformMap.MIN_FOOT_SUPPORT && player.y <= PlatformMap.riverSurface((from + to) / 2, elapsed)) {
                fall(gap); return true;
            }
        }
        return false;
    }
    protected void fall(PlatformMap.Gap gap) {
        falls++;
        if (gap.lake) waterEntries++;
        if (gap.lake || !map.keepsVehicleInPits()) { bicycle = false; itemUsed = bicycleAvailable; }
        loseLife();
        if (failed) return;
        respawn(gap.respawn);
    }
    protected void respawn(Vector2 checkpoint) {
        player.setPosition(checkpoint); velocityY = knockbackTime = 0;
        arriving = false; grounded = true; jumps = 0;
        rocks.resetAfter(checkpoint.x);
        afterFall(checkpoint.x);
    }
    protected void beforeStep(float dt) { }
    protected void afterMovement(float dt) { }
    protected void onReset() { }
    protected void afterFall(float checkpointX) { }
    protected void onHorizontalBlock() { }
    protected boolean allowsJump() { return true; }
    protected boolean onHeadHit(Rectangle block) { return false; }
    protected boolean canCollectCoin(int index) { return true; }
    protected boolean headOverlaps(Rectangle block) { return overlapsX(block); }
    public void poison() { }
    protected float adjustedDirection(float direction) { return direction; }
    protected int maximumJumps() { return bicycle && !map.usesScooter() ? 2 : 1; }
    protected float movementVelocity(float dt, float direction) {
        return direction * PlatformMap.MOVE_SPEED * (bicycle && map.usesScooter() ? 2 : 1);
    }
    public boolean isCollected(int index) { return collected[index]; }
    public boolean isArriving() { return arriving; }
    public boolean isGrounded() { return grounded; }
    public int getLives() { return lives; }
    public boolean isFailed() { return failed; }
    public boolean isRiding() { return bicycle; }
    public float getBikePop() { return bikePop / PlatformMap.BIKE_POP_TIME; }
    public boolean isItemUsed() { return itemUsed; }
    public boolean isBicycleAvailable() { return bicycleAvailable; }
    public boolean isInvulnerable() { return invulnerability > 0; }
    public float getDamageFeedback() { return damageFeedback / PlatformMap.HIT_FLASH; }
    public float getBorderFeedback() { return borderFeedback / PlatformMap.BORDER_FLASH; }
    public int getScore() { return score; }
    public int getFallCount() { return falls; }
    public int getBicycleMountCount() { return bicycleMounts; }
    public int getJumpCount() { return jumpCount; }
    public int getDamageCount() { return damageCount; }
    public int getBumpCount() { return bumpCount; }
    public int getPickupCount() { return pickupCount; }
    public int getWaterEntryCount() { return waterEntries; }
    public double getElapsed() { return elapsed; }
    public boolean isCleared() { return cleared; }
    public boolean isFinished() { return finished; }
    public boolean isFacingLeft() { return facingLeft; }
    public String getGrade() { return score <= 2 ? "Bad" : score <= 6 ? "Good" : score <= 9 ? "Great" : "Perfect"; }
    public static String formatTime(double elapsed) {
        long seconds = (long) Math.max(0, elapsed);
        return String.format(java.util.Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }
}
