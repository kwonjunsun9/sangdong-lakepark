package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;
import static org.junit.Assert.*;

public class EasyHazardsTest {
    private final EasyMap map = new EasyMap();
    private final EasyRun run = new EasyRun(map);
    private void advance(int n) { for (int i = 0; i < n; i++) run.update(1f / 60f, 0, false); }
    private void waterPhase(float target) {
        for (int i = 0; i < 300; i++) { if (Math.abs(map.water.phase(run.getElapsed()) - target) < .015f) return; advance(1); }
        throw new AssertionError("Water phase not reached");
    }
    @Test public void trapHitsLaunchBarePlayerAndInvulnerabilityPreventsRepeatedDamage() {
        advance(120); Rectangle trap = map.traps.first();
        assertEquals(808, trap.x, 0); assertEquals(48, trap.width, 0); assertEquals(24, trap.height, 0);
        run.player.setPosition(trap.x, trap.y); float x = run.player.x; run.update(1f / 120f, 1, true);
        assertEquals(4, run.getLives()); assertTrue(run.player.x < x); assertTrue(run.player.y > trap.y);
        assertEquals(0, run.getFallCount());
        assertTrue(run.isInvulnerable()); assertTrue(run.getDamageFeedback() > 0); assertTrue(run.getBorderFeedback() > 0);
        for (int i = 0; i < 12; i++) { run.player.setPosition(trap.x, trap.y); run.update(1f / 120f, 1, true); }
        assertEquals(4, run.getLives());
        run.player.setPosition(400, 184); advance(90);
        run.player.setPosition(trap.x, trap.y); run.update(1f / 120f, 0, false); assertEquals(3, run.getLives());
        assertEquals(294, map.traps.get(1).y, 0);
    }
    @Test public void bikeShieldsOneHitThenCanBeObtainedAgain() {
        EasyLayoutTest.equip(map, run); Rectangle trap = map.traps.first();
        run.player.setPosition(trap.x, trap.y); run.update(1f / 120f, 1, false);
        assertFalse(run.isRiding()); assertEquals(5, run.getLives()); assertTrue(run.isInvulnerable());
        assertTrue(run.getBorderFeedback() > 0); assertEquals(0, run.getDamageFeedback(), 0);
        run.player.setPosition(400, 184); advance(90);
        EasyLayoutTest.equip(map, run); assertTrue(run.isRiding());
    }
    @Test public void blinkFadesLinearlyAndFadeInHasNoCollision() {
        EasyMap.BlinkPlatform b = map.blinking.first();
        float d=b.delay;
        assertEquals(1, b.opacity(d+1), .001f); assertEquals(.5, b.opacity(d+2.3), .001f);
        assertTrue(b.solid(d+2.3)); assertFalse(b.solid(d+2.8)); assertEquals(0, b.opacity(d+3), .001f);
        assertEquals(.5, b.opacity(d+3.8), .001f); assertFalse(b.solid(d+3.8)); assertTrue(b.solid(d+4.1));
    }
    @Test public void eachPairDisappearsOneCellAtATimeAndRowsHaveDifferentPhases() {
        assertEquals(6,map.blinking.size);
        for(int row=0;row<3;row++) {
            EasyMap.BlinkPlatform left=map.blinking.get(row*2),right=map.blinking.get(row*2+1);
            double time=left.delay+2.7;
            assertFalse(left.solid(time)); assertTrue(right.solid(time));
            assertEquals(0,left.opacity(time),.001f); assertTrue(right.opacity(time)>0);
            assertFalse(right.solid(time+.4));
        }
        assertTrue(map.blinking.get(2).delay < map.blinking.get(4).delay);
    }
    @Test public void disappearingBlockDropsItsPassengerWithoutDirectDamage() {
        advance(120); EasyMap.BlinkPlatform b = map.blinking.first();
        run.player.setPosition(b.x - 10, b.y + b.height); run.update(1f / 60f, 0, false);
        assertTrue(run.isGrounded()); advance(95); assertTrue(run.player.y < b.y + b.height); assertEquals(5, run.getLives());
    }
    @Test public void waterWarningAndRestAreSafeButPouringDealsDamage() {
        advance(120); waterPhase(.1f); run.player.setPosition(map.water.x, 184); run.update(1f / 120f, 0, false);
        assertEquals(5, run.getLives()); assertEquals(0, run.getBorderFeedback(), 0);
        run.player.setPosition(260, 184); waterPhase(1); run.player.setPosition(map.water.x, 184); run.update(1f / 120f, 0, false);
        assertEquals(4, run.getLives()); assertTrue(run.getBorderFeedback() > 0);
        run.player.setPosition(260, 184); waterPhase(3); run.player.setPosition(map.water.x, 184); run.update(1f / 120f, 0, false);
        assertEquals(4, run.getLives());
    }
    @Test public void pouringWaterDismountsBikeWithoutLosingLife() {
        EasyLayoutTest.equip(map, run); run.player.setPosition(260, 184); waterPhase(1);
        run.player.setPosition(map.water.x, 184); run.update(1f / 120f, 0, false);
        assertFalse(run.isRiding()); assertEquals(5, run.getLives());
    }
    @Test public void birdCarriesThenReleasesRockWhichShattersForPointFourSeconds() {
        EasyRocks rocks = new EasyRocks(map); EasyRocks.Rock rock = rocks.all.first();
        rocks.update(.01f,5232);
        float flightTime = (rocks.birds.first().x - rock.spawn.x) / EasyMap.BIRD_SPEED;
        for (int i = 0; i < (int) (flightTime / .01f) - 1; i++) rocks.update(.01f, 5232);
        assertEquals(EasyRocks.Phase.CARRIED, rock.phase); assertFalse(rock.dangerous());assertEquals(545, rock.bounds.y, 0);
        rocks.update(.03f, 5232); assertEquals(EasyRocks.Phase.FALLING, rock.phase);
        assertEquals(5632-32,rock.bounds.x,.01f);
        for (int i = 0; i < 150 && rock.phase != EasyRocks.Phase.SHATTERING; i++) rocks.update(.01f, 5232);
        assertEquals(EasyRocks.Phase.SHATTERING, rock.phase);
        for (int i = 0; i < 20; i++) rocks.update(.01f, 5232);
        assertEquals(EasyRocks.Phase.SHATTERING, rock.phase);
        for (int i = 0; i < 25; i++) rocks.update(.01f, 5232);
        assertEquals(EasyRocks.Phase.GONE, rock.phase);
    }
    @Test public void fallingRockRunsTheOriginalTrajectoryAtDoubleSpeed() {
        EasyRocks rocks=new EasyRocks(map);EasyRocks.Rock rock=rocks.all.first();
        rock.phase=EasyRocks.Phase.FALLING;
        for(int i=0;i<20;i++) rocks.update(.005f,0);
        float originalTrajectoryAtPointTwo = EasyMap.GRAVITY * .2f * .2f / 2;
        assertEquals(originalTrajectoryAtPointTwo,545-rock.bounds.y,2);
        assertEquals(EasyRocks.Phase.FALLING,rock.phase);
    }
    @Test public void rollingRockRotatesMovesLeftDropsDownStepsAndExpires() {
        EasyRocks rocks = new EasyRocks(map); EasyRocks.Rock rock = rocks.all.get(1);
        for (int i = 0; i < 600 && rock.phase != EasyRocks.Phase.ROLLING; i++) rocks.update(.01f, 6000);
        assertEquals(EasyRocks.Phase.ROLLING, rock.phase);
        float x = rock.bounds.x, y = rock.bounds.y, rotation = rock.rotation;
        for (int i = 0; i < 100; i++) rocks.update(.01f, 6000);
        assertEquals(480, x - rock.bounds.x, .2f); assertEquals(15, rock.rotation - rotation, .01f); assertTrue(rock.bounds.y < y);
        for (int i = 0; i < 500; i++) rocks.update(.01f, 6000);
        assertEquals(EasyRocks.Phase.GONE, rock.phase);
    }
    @Test public void rockContactConsumesRockAndDamagesOrDismountsPlayer() {
        for (boolean bike : new boolean[]{false,true}) {
            run.reset(); advance(120); if (bike) EasyLayoutTest.equip(map, run);
            run.player.setPosition(400,184); EasyRocks.Rock rock = run.rocks.all.first();
            rock.phase = EasyRocks.Phase.FALLING; rock.bounds.setPosition(400,240);
            run.update(1f / 120f, 0, false); assertEquals(bike ? 5 : 4, run.getLives());
            assertFalse(run.isRiding()); assertEquals(EasyRocks.Phase.SHATTERING, rock.phase);
            assertTrue(run.getBorderFeedback() > 0);
        }
    }
    @Test public void fallRearmsOnlyRocksToTheRightOfCheckpoint() {
        advance(120);
        for(int i=0;i<1000&&run.rocks.birds.first().drops()==0;i++)run.rocks.update(.01f,5232);
        assertEquals(EasyRocks.Flight.HOVERING,run.rocks.birds.first().flight);
        for (EasyRocks.Rock rock : run.rocks.all) rock.phase = EasyRocks.Phase.GONE;
        run.player.setPosition(6304,-101); run.update(1f / 120f,0,false);
        assertEquals(6118,run.player.x,0); assertEquals(EasyRocks.Phase.GONE,run.rocks.all.first().phase);
        for (int i=1;i<map.rocks.size;i++) assertEquals(EasyRocks.Phase.READY,run.rocks.all.get(i).phase);
    }
    @Test public void birdEntersFromOutsideThenHoversAndDropsEveryThreeSeconds() {
        EasyRocks rocks=new EasyRocks(map);EasyRocks.Bird bird=rocks.birds.first();
        rocks.update(.01f,5103);assertEquals(EasyRocks.Flight.WAITING,bird.flight);
        rocks.update(.01f,5104);assertEquals(EasyRocks.Flight.ENTERING,bird.flight);
        assertTrue("The entire sprite starts outside the camera",bird.x-EasyMap.BIRD_DRAW_SIZE/2>5104+640);
        float start=bird.x;rocks.update(.1f,5104);
        assertEquals("Flight is three times slower than 420px/s",14,start-bird.x,.01f);
        for(int i=0;i<1000&&bird.drops()==0;i++)rocks.update(.01f,5104);
        assertEquals(1,bird.drops());assertEquals(EasyRocks.Flight.HOVERING,bird.flight);
        assertEquals(bird.spawn.x,bird.x,0);
        EasyRocks.Rock first=rocks.all.first();
        for(int i=0;i<219;i++)rocks.update(.01f,5104);
        assertFalse(bird.warning());assertEquals(1,bird.drops());
        for(int i=0;i<80;i++)rocks.update(.01f,5104);
        assertTrue(bird.warning());assertEquals(1,bird.drops());
        rocks.update(.02f,5104);
        assertEquals(2,bird.drops());assertFalse(bird.warning());
        assertEquals(bird.spawn.x,bird.x,0);assertTrue(bird.visible());
        assertNotSame(first,rocks.all.peek());assertEquals(EasyRocks.Phase.GONE,first.phase);
        assertEquals(EasyRocks.Phase.FALLING,rocks.all.peek().phase);
        assertEquals(2,rocks.getFallingSoundCount());
    }
    @Test public void destroyingReleasedRockDoesNotRemoveTheHoveringBirdOrCancelItsNextDrop() {
        EasyRocks rocks=new EasyRocks(map);EasyRocks.Bird bird=rocks.birds.first();
        for(int i=0;i<500 && bird.drops()==0;i++)rocks.update(.01f,5232);
        assertEquals(1,bird.drops());
        rocks.all.first().shatter();float x=bird.x;
        for(int i=0;i<50;i++)rocks.update(.01f,5232);
        assertEquals(EasyRocks.Phase.GONE,rocks.all.first().phase);assertTrue(bird.visible());
        assertEquals(x,bird.x,0);assertEquals(1,bird.drops());
        for(int i=0;i<251;i++)rocks.update(.01f,5232);
        assertEquals(2,bird.drops());assertEquals(EasyRocks.Flight.HOVERING,bird.flight);
        assertEquals(x,bird.x,0);assertEquals(EasyRocks.Phase.FALLING,rocks.all.peek().phase);
    }
    @Test public void finaleKeepsOneBirdAtTheLastStepAndRepeatsItsDrops() {
        int count=0;for(EasyMap.RockSpawn spawn:map.rocks)if(spawn.x>6300)count++;
        assertEquals(1,count);
        EasyRocks rocks=new EasyRocks(map);EasyRocks.Bird bird=rocks.birds.peek();
        rocks.update(.1f,6367);assertFalse(bird.visible());
        rocks.update(.01f,6368);assertEquals(EasyRocks.Flight.ENTERING,bird.flight);
        assertTrue(bird.x-EasyMap.BIRD_DRAW_SIZE/2>6368+640);
        for(int i=0;i<2000;i++)rocks.update(.01f,6880);
        assertTrue(bird.drops()>3);assertTrue(bird.visible());
        assertEquals(EasyRocks.Flight.HOVERING,bird.flight);assertEquals(bird.spawn.x,bird.x,0);
        assertTrue("Expired repeats are removed",rocks.all.size<8);
        assertEquals(184+192,map.groundTop(bird.spawn.x),0);
    }
    @Test public void offscreenRoutesWaitAndHoveringBirdsStopDroppingUntilTheCameraReturns() {
        EasyRocks rocks=new EasyRocks(map);rocks.update(.01f,6880);
        assertFalse(rocks.birds.first().visible());assertEquals(0,rocks.birds.first().drops());
        assertEquals(EasyRocks.Flight.ENTERING,rocks.birds.peek().flight);
        for(int i=0;i<1000;i++)rocks.update(.01f,6880);
        EasyRocks.Bird bird=rocks.birds.peek();int drops=bird.drops();assertTrue(drops>0);
        for(int i=0;i<1000;i++)rocks.update(.01f,100);
        assertEquals(drops,bird.drops());assertFalse(bird.visible());
        for(int i=0;i<1000&&bird.drops()==drops;i++)rocks.update(.01f,6880);
        assertEquals(drops+1,bird.drops());assertEquals(EasyRocks.Flight.HOVERING,bird.flight);
        assertEquals(bird.spawn.x,bird.x,0);
        rocks.reset();assertEquals(map.rocks.size,rocks.all.size);assertEquals(0,rocks.getFallingSoundCount());
        for(EasyRocks.Bird resetBird:rocks.birds){assertEquals(0,resetBird.drops());assertFalse(resetBird.visible());}
    }
    @Test public void leavingDuringEntryRemovesTheCarriedRockAndReturningStartsAFreshApproach() {
        EasyRocks rocks=new EasyRocks(map);EasyRocks.Bird bird=rocks.birds.first();
        rocks.update(.01f,5232);assertEquals(EasyRocks.Phase.CARRIED,rocks.all.first().phase);
        rocks.update(.01f,100);assertFalse(bird.visible());
        assertEquals(EasyRocks.Phase.GONE,rocks.all.first().phase);assertEquals(0,bird.drops());
        rocks.update(.01f,5232);assertEquals(EasyRocks.Flight.ENTERING,bird.flight);
        assertEquals(EasyRocks.Phase.CARRIED,rocks.all.first().phase);
        for(int i=0;i<1000&&bird.drops()==0;i++)rocks.update(.01f,5232);
        assertEquals(1,bird.drops());assertEquals(EasyRocks.Flight.HOVERING,bird.flight);
    }
    @Test public void rollingBirdEmitsFallingThenRollingOnceAndCanRepeatAfterRespawn() {
        EasyMap isolated=new EasyMap();isolated.rocks.clear();isolated.rocks.add(new EasyMap.RockSpawn(6176,true));
        EasyRocks rocks = new EasyRocks(isolated);
        for (int attempt = 1; attempt <= 2; attempt++) {
            for (int i = 0; i < 1500 && rocks.getFallingSoundCount() < attempt; i++) rocks.update(.01f, 6000);
            assertEquals(attempt, rocks.getFallingSoundCount()); assertEquals(attempt - 1, rocks.getRollingSoundCount());
            for (int i = 0; i < 100 && rocks.getRollingSoundCount() < attempt; i++) rocks.update(.01f, 6000);
            assertEquals(attempt, rocks.getRollingSoundCount());
            for (int i = 0; i < 30; i++) rocks.update(.01f, 6000);
            assertEquals(attempt, rocks.getFallingSoundCount()); assertEquals(attempt, rocks.getRollingSoundCount());
            rocks.resetAfter(6000);
        }
        rocks.reset(); assertEquals(0, rocks.getFallingSoundCount()); assertEquals(0, rocks.getRollingSoundCount());
    }
}
