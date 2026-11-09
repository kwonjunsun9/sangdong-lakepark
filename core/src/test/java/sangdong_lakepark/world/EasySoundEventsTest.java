package sangdong_lakepark.world;

import org.junit.Test;
import static org.junit.Assert.*;

public class EasySoundEventsTest {
    private final EasyMap map = new EasyMap();
    private final EasyRun run = new EasyRun(map);
    private void advance(int frames) { for (int i = 0; i < frames; i++) run.update(1f / 60f, 0, false); }

    @Test public void onlyAcceptedJumpsEmitIncludingTheBicycleSecondJump() {
        advance(120); run.update(1f / 60f, 0, true); assertEquals(1, run.getJumpCount());
        run.update(1f / 60f, 0, true); assertEquals(1, run.getJumpCount());
        EasyLayoutTest.equip(map, run); run.player.setPosition(400, 184); advance(60);
        int before = run.getJumpCount(); run.update(1f / 60f, 0, true); advance(20);
        run.update(1f / 60f, 0, true); assertEquals(before + 2, run.getJumpCount());
        run.update(1f / 60f, 0, true); assertEquals(before + 2, run.getJumpCount());
        run.reset(); assertEquals(0, run.getJumpCount());
    }

    @Test public void headHitReportsPickupOnlyWhenAnItemActuallyAppears() {
        advance(120); run.player.setPosition(2810, 184); run.update(1f / 60f, 0, true); advance(60);
        assertTrue(run.isBicycleAvailable()); assertEquals(1, run.getPickupCount()); assertEquals(0, run.getBumpCount());
        run.player.setPosition(2810, 184); run.update(1f / 60f, 0, true); advance(60);
        assertEquals(1, run.getPickupCount()); assertEquals(1, run.getBumpCount());
        run.reset(); assertEquals(0, run.getPickupCount()); assertEquals(0, run.getBumpCount());
    }

    @Test public void emptyBlockHeadHitBumpsOnceButSideAndLandingDoNot() {
        advance(120); EasyMap.Block wood = map.blocks.first();
        run.player.setPosition(wood.x + 8, wood.y - run.player.height - 30); run.update(1f / 60f, 0, true);
        for (int i = 0; i < 30 && run.getBumpCount() == 0; i++) advance(1);
        assertEquals(1, run.getBumpCount()); assertEquals(0, run.getPickupCount());
        run.reset(); advance(120); run.player.setPosition(wood.x - run.player.width - 2, wood.y + 8);
        run.update(1f / 60f, 1, false); assertEquals(0, run.getBumpCount());
        run.player.setPosition(wood.x + 8, wood.y + wood.height + 30); advance(60);
        assertTrue(run.isGrounded()); assertEquals(0, run.getBumpCount()); assertEquals(0, run.getPickupCount());
    }

    @Test public void onlyAcceptedObstacleHitsEmitDamageAndFallsUseTheirOwnEvent() {
        EasyLayoutTest.equip(map, run);
        run.player.setPosition(map.traps.first().x, map.traps.first().y); run.update(1f / 60f, 0, false);
        assertEquals(1, run.getDamageCount()); assertEquals(5, run.getLives()); assertFalse(run.isRiding());
        for (int i = 0; i < 20; i++) {
            run.player.setPosition(map.traps.first().x, map.traps.first().y); run.update(1f / 60f, 0, false);
        }
        assertEquals(1, run.getDamageCount());
        run.player.setPosition(3708, -101); run.update(1f / 60f, 0, false);
        assertEquals(1, run.getDamageCount()); assertEquals(1, run.getFallCount());
        run.reset(); assertEquals(0, run.getDamageCount()); assertEquals(0, run.getFallCount());
    }
}
