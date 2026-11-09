package sangdong_lakepark.world;

import org.junit.Test;
import static org.junit.Assert.*;

public class HardThunderTest {
    private final HardRun run = new HardRun(new HardMap());
    private void advance(HardThunder thunder, int frames) {
        for (int i = 0; i < frames; i++) thunder.update(1f / 120, run);
    }

    @Test public void offscreenCloudKeepsCyclingWithoutEmittingSoundEvents() {
        HardThunder thunder = new HardThunder();
        run.player.x = 6900; advance(thunder, 130);
        assertEquals(1, thunder.getStrikeCount()); assertEquals(1, thunder.getVisibleStrikeCount());
        run.player.x = 100; advance(thunder, 360);
        assertEquals(2, thunder.getStrikeCount()); assertEquals(1, thunder.getVisibleStrikeCount());
        assertTrue(thunder.clouds[0].active());
        run.player.x = 6900; advance(thunder, 360);
        assertEquals(3, thunder.getStrikeCount()); assertEquals(2, thunder.getVisibleStrikeCount());
    }

    @Test public void anotherVisibleCloudCannotMakeAnOffscreenStrikeAudible() {
        HardThunder.Cloud first = new HardThunder.Cloud(2000, 1900, 2100, 248);
        HardThunder.Cloud second = new HardThunder.Cloud(7000, 6900, 7100, 248);
        HardThunder thunder = new HardThunder(first, second);
        run.player.x = 1960; advance(thunder, 130);
        run.player.x = 6960; advance(thunder, 360);
        assertEquals(3, thunder.getStrikeCount()); assertEquals(2, thunder.getVisibleStrikeCount());
        assertSame(second, thunder.getLastVisibleStrike());
        assertFalse(first.visible(6364, 7644)); assertTrue(second.visible(6364, 7644));
    }

    @Test public void visibilityUsesClampedCameraAndIncludesPartiallyVisibleClouds() {
        HardThunder.Cloud edge = new HardThunder.Cloud(1340, 1300, 1400, 248);
        HardThunder.Cloud outside = new HardThunder.Cloud(1349, 1300, 1400, 248);
        HardThunder thunder = new HardThunder(edge, outside);
        edge.age = outside.age = 1;
        run.player.x = 100; advance(thunder, 1);
        assertEquals(2, thunder.getStrikeCount()); assertEquals(1, thunder.getVisibleStrikeCount());
        assertSame(edge, thunder.getLastVisibleStrike());
        assertTrue(edge.visible(0, 1280)); assertFalse(outside.visible(0, 1280));
    }

    @Test public void resetClearsAudioEventsWhileRearmPreservesEventCounters() {
        HardThunder thunder = new HardThunder(); run.player.x = 6900;
        advance(thunder, 130); thunder.rearm(); advance(thunder, 130);
        assertEquals(2, thunder.getStrikeCount()); assertEquals(2, thunder.getVisibleStrikeCount());
        thunder.reset();
        assertEquals(0, thunder.getStrikeCount()); assertEquals(0, thunder.getVisibleStrikeCount());
        assertNull(thunder.getLastVisibleStrike()); assertEquals(-1, thunder.clouds[0].age, 0);
    }
}
