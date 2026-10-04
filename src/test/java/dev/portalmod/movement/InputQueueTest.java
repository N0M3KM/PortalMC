package dev.portalmod.movement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InputQueueTest {
    @Test void packetBurstCannotRunFasterThanServerTimeBudget() {
        InputQueue queue = new InputQueue();
        for (int i = 1; i <= 100; i++) queue.offer(i, MovementInput.IDLE, 128);
        int consumed = 0;
        for (int tick = 0; tick < 10; tick++) {
            queue.beginTick(2);
            while (queue.poll() != null) consumed++;
        }
        assertEquals(10, consumed);
        assertEquals(10, queue.acknowledged());
    }
    @Test void idleCreditIsBoundedAndDuplicatesDoNotConsumeIt() {
        InputQueue queue = new InputQueue();
        for (int i = 0; i < 50; i++) queue.beginTick(2);
        queue.offer(1, MovementInput.IDLE, 128);
        queue.offer(1, MovementInput.IDLE, 128);
        queue.offer(2, MovementInput.IDLE, 128);
        queue.offer(3, MovementInput.IDLE, 128);
        assertNotNull(queue.poll()); assertNotNull(queue.poll()); assertNull(queue.poll());
        assertEquals(1, queue.size());
    }
    @Test void gapsAndOverflowRejectInsteadOfSilentlyDesynchronizing() {
        InputQueue queue = new InputQueue();
        assertThrows(IllegalArgumentException.class, () -> queue.offer(2, MovementInput.IDLE, 1));
        queue.offer(1, MovementInput.IDLE, 1);
        assertThrows(IllegalArgumentException.class, () -> queue.offer(2, MovementInput.IDLE, 1));
    }
}
