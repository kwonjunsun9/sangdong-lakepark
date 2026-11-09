package sangdong_lakepark.world;

import org.junit.Test;
import static org.junit.Assert.*;

public class EasyLayoutTest {
    static void equip(EasyMap map, EasyRun run) {
        for(int i=0;i<120;i++) run.update(1f/60f,0,false);
        run.player.setPosition(2810,184);run.update(1f/60f,0,true);
        for(int i=0;i<60;i++)run.update(1f/60f,0,false);
        assertTrue(run.isBicycleAvailable());assertEquals(462,map.bicycle.y,0);
        run.player.setPosition(map.bicycle.x,map.bicycle.y);run.update(1f/60f,0,false);assertTrue(run.isRiding());
    }
    @Test public void keepingBikeAllowsAllTenCoinsInOneAttempt() { verify(true,10); }
    @Test public void losingBikeAfterCreekStillClearsWithSevenCoins() { verify(false,7); }
    private void verify(boolean keep,int score) {
        EasyMap map=new EasyMap();EasyRun run=new EasyRun(map);
        EasyCourseRoute.play(map,run,keep,(d,j)->run.update(1f/60f,d,j));
        assertTrue(run.isCleared());assertTrue(run.getLives()>0);assertEquals(score,run.getScore());
        assertEquals(keep,run.isRiding());assertEquals(keep?"Perfect":"Great",run.getGrade());
    }
    @Test public void bikeAddsSecondJumpWithoutIncreasingJumpSpeedOrCollisionBox() {
        EasyMap map=new EasyMap();EasyRun run=new EasyRun(map);
        for(int i=0;i<120;i++)run.update(1f/60f,0,false);
        float first=jumpPeak(run,false)-184;
        equip(map,run);run.player.setPosition(260,184);float bikeFirst=jumpPeak(run,false)-184;
        run.player.setPosition(260,184);float bikeDouble=jumpPeak(run,true)-184;
        assertEquals(first,bikeFirst,.1f);assertTrue(bikeDouble>first*1.9f);
        assertEquals(88,run.player.width,0);assertEquals(120,run.player.height,0);
    }
    private float jumpPeak(EasyRun run,boolean twice) {
        run.update(1f/60f,0,true);float max=run.player.y;
        for(int i=0;i<150;i++){run.update(1f/60f,0,twice&&i==22);max=Math.max(max,run.player.y);}return max;
    }
    @Test public void mountingCountsOnlyActualPickupsAndResetsOnRetry() {
        EasyMap map = new EasyMap(); EasyRun run = new EasyRun(map);
        equip(map, run); assertEquals(1, run.getBicycleMountCount());
        for (int i = 0; i < 30; i++) run.update(1f / 60f, 0, false);
        assertEquals(1, run.getBicycleMountCount());
        run.player.setPosition(map.traps.first().x, map.traps.first().y);
        run.update(1f / 60f, 0, false); assertFalse(run.isRiding());
        for (int i = 0; i < 90; i++) run.update(1f / 60f, 0, false);
        equip(map, run); assertEquals(2, run.getBicycleMountCount());
        run.reset(); assertEquals(0, run.getBicycleMountCount());
    }
    @Test public void blocksStopHeadsAndSidesAndBikeSpawnsAboveOnlyWhenMissing() {
        EasyMap map=new EasyMap();EasyRun run=new EasyRun(map);
        for(int i=0;i<120;i++)run.update(1f/60f,0,false);
        EasyMap.Block wood=map.blocks.get(3);run.player.setPosition(wood.x-44,184);run.update(1f/60f,0,true);
        float maxHead=0;for(int i=0;i<30;i++){run.update(1f/60f,0,false);maxHead=Math.max(maxHead,run.player.y+run.player.height);}
        assertTrue(maxHead<=wood.y+.1f);assertFalse(run.isBicycleAvailable());
        run.player.setPosition(wood.x-run.player.width-2,wood.y+8);run.update(.1f,1,false);
        assertTrue(run.player.x+run.player.width<=wood.x+.1f);
        equip(map,run);run.player.setPosition(2810,184);run.update(1f/60f,0,true);
        for(int i=0;i<60;i++)run.update(1f/60f,0,false);assertFalse(run.isBicycleAvailable());
    }
    @Test public void creekRequiresBikeFromTheFootSupportedTakeoff() {
        EasyMap map=new EasyMap();EasyRun bare=new EasyRun(map);
        for(int i=0;i<120;i++)bare.update(1f/60f,0,false);
        bare.player.setPosition(3450,184);bare.update(1f/60f,1,true);
        boolean crossed = false;
        for(int i=0;i<130;i++) {
            bare.update(1f/60f,1,i==22);
            crossed |= bare.player.x + bare.player.width > 3808 && bare.player.y >= EasyMap.GROUND_Y - .1f;
        }
        assertFalse("A bare jump must not reach the far shore above ground level", crossed);
        assertTrue(bare.getLives()<5);
        EasyRun bike=new EasyRun(map);equip(map,bike);bike.player.setPosition(3450,184);
        bike.update(1f/60f,1,true);boolean landed=false;
        for(int i=0;i<95;i++) {
            bike.update(1f/60f,1,i==22);
            if(bike.player.x>3808 && bike.isGrounded()) {landed=true;break;}
        }
        assertTrue(landed);assertTrue(bike.isRiding());
    }
    @Test public void fallingDismountsTheBikeAndUsesNewCheckpoint() {
        EasyMap map=new EasyMap();EasyRun run=new EasyRun(map);equip(map,run);
        run.player.setPosition(3708,-101);run.update(1f/60f,0,false);
        assertEquals(4,run.getLives());assertFalse(run.isRiding());assertFalse(run.isItemUsed());
        assertEquals(3384,run.player.x,0);assertEquals(184,run.player.y,0);
        EasyLayoutTest.equip(map, run); assertTrue(run.isRiding());
    }
    @Test public void everyPitDismountsEvenDuringProtectionAndRemovesTheExtraJump() {
        EasyMap map = new EasyMap();
        for (EasyMap.Gap gap : map.gaps) {
            EasyRun attempt = new EasyRun(map); equip(map, attempt);
            attempt.player.setPosition(gap.x + gap.width / 2 - attempt.player.width / 2, -151);
            attempt.update(1f / 60f, 0, false);
            assertEquals(4, attempt.getLives()); assertFalse(attempt.isRiding()); assertFalse(attempt.isItemUsed());
            assertEquals(gap.respawn.x, attempt.player.x, .01f); assertEquals(gap.respawn.y, attempt.player.y, .01f);
            assertEquals(1, attempt.getFallCount()); assertTrue(attempt.isInvulnerable());
            int before = attempt.getJumpCount(); attempt.update(1f / 60f, 0, true);
            attempt.update(1f / 60f, 0, true); assertEquals(before + 1, attempt.getJumpCount());
            attempt.player.setPosition(gap.x + gap.width / 2 - attempt.player.width / 2, -151);
            attempt.update(1f / 60f, 0, false); assertEquals(3, attempt.getLives()); assertFalse(attempt.isRiding());
        }
    }
    @Test public void bodyOverhangCannotStandWithoutFeetAndWalkingOffDoesNotTeleport() {
        EasyMap map = new EasyMap(); EasyRun run = new EasyRun(map);
        for (int i=0;i<120;i++) run.update(1f/60f,0,false);
        run.player.setPosition(712,232); run.update(1f/60f,0,false);
        assertFalse("Body overlap alone must not support the player",run.isGrounded());
        assertTrue(run.player.y < 232);
        run.player.setPosition(612,232); run.update(1f/60f,0,false); assertTrue(run.isGrounded());
        for(int i=0;i<22;i++) {
            float previous = run.player.x; run.update(1f/60f,1,false);
            assertTrue("Leaving a rounded ledge must not push the player to its opposite side",run.player.x >= previous);
        }
        for(int i=0;i<60;i++)run.update(1f/60f,0,false);
        assertEquals(184,run.player.y,.01f);
    }
    @Test public void roundedCornerSupportsFeetAtTheDrawnHeight() {
        EasyMap map = new EasyMap(); EasyRun run = new EasyRun(map);
        for(int i=0;i<120;i++) run.update(1f/60f,0,false);
        run.player.setPosition(692,250);
        for(int i=0;i<30;i++) run.update(1f/60f,0,false);
        assertTrue(run.isGrounded()); assertTrue(run.player.y < 232); assertTrue(run.player.y > 215);
    }
    @Test public void raisedRestPostRequiresAJumpFromTheBlinkingBlock() {
        EasyMap map = new EasyMap(); EasyRun run = new EasyRun(map);
        for(int i=0;i<120;i++) run.update(1f/60f,0,false);
        run.player.setPosition(4806,296); run.update(.1f,1,false);
        assertTrue("Walking into the raised post must be blocked",run.player.x + run.player.width <= 4896+.1f);
        assertTrue(run.player.y < 360);
        run.reset();for(int i=0;i<120;i++)run.update(1f/60f,0,false);
        run.player.setPosition(4728,296); run.update(1f/60f,0,false);
        run.update(1f/60f,1,true);
        for(int i=0;i<45;i++) run.update(1f/60f,run.player.x<4912?1:0,false);
        assertEquals(360,run.player.y,.01f); assertTrue(run.isGrounded());
    }
    @Test public void lakeCoinNeedsDoubleJumpEvenFromTheHighestNearbyPost() {
        EasyMap map=new EasyMap();EasyRun bare=new EasyRun(map);
        for(int i=0;i<120;i++)bare.update(1f/60f,0,false);
        bare.player.setPosition(4838,360);bare.update(1f/60f,0,false);bare.update(1f/60f,-1,true);
        for(int i=0;i<70;i++)bare.update(1f/60f,-1,false);
        assertFalse(bare.isCollected(5));
        EasyRun bike=new EasyRun(map);equip(map,bike);bike.player.setPosition(4660,296);
        bike.update(1f/60f,0,false);bike.update(1f/60f,0,true);
        for(int i=0;i<55;i++)bike.update(1f/60f,0,i==22);
        assertTrue(bike.isCollected(5));
    }
    @Test public void everySupportedApproachPositionHasRoomForAFullJump() {
        for(float x:new float[]{2918,2944,2972}) {
            EasyRun run=new EasyRun(new EasyMap());
            for(int i=0;i<120;i++)run.update(1f/60f,0,false);
            run.player.setPosition(x,274);for(int i=0;i<30;i++)run.update(1f/60f,0,false);
            assertTrue(run.isGrounded());float start=run.player.y,peak=start;
            run.update(1f/60f,0,true);
            for(int i=0;i<60;i++){run.update(1f/60f,0,false);peak=Math.max(peak,run.player.y);}
            assertTrue("No head collision above the approach at x="+x,peak-start>115);
        }
    }
    @Test public void markedFinalHillsAreRemovedWhileTheFloorRemains() {
        EasyMap map=new EasyMap();
        for(float x:new float[]{6384,6480,6528,6592})assertEquals(184,map.groundTop(x),0);
        assertEquals(280,map.groundTop(6650),0);assertEquals(328,map.groundTop(6750),0);assertEquals(376,map.groundTop(6860),0);
    }
    @Test public void itemRespondsAtBothExpandedEdgesButNotOutsideTheMargin() {
        EasyMap map=new EasyMap();
        float[] positions={map.itemBlock.x-88-8,map.itemBlock.x+64+8,map.itemBlock.x-4,
            map.itemBlock.x-88-17,map.itemBlock.x+64+17};
        for(int index=0;index<positions.length;index++) {
            EasyRun run=new EasyRun(map);for(int i=0;i<120;i++)run.update(1f/60f,0,false);
            run.player.setPosition(positions[index],184);run.update(1f/60f,0,false);
            assertFalse("Walking under the row must not spawn a bicycle",run.isBicycleAvailable());
            run.update(1f/60f,0,true);
            for(int i=0;i<60;i++)run.update(1f/60f,0,false);
            assertEquals("Expanded hit area at x="+positions[index],index<3,run.isBicycleAvailable());
        }
    }
}
