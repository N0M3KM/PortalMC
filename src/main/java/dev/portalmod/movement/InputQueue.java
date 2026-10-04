package dev.portalmod.movement;

import java.util.ArrayDeque;

/** Reliable-stream sequencing plus a server-time budget; input spam cannot speed up simulation. */
public final class InputQueue {
    public record Command(long sequence, MovementInput input) { }
    private final ArrayDeque<Command> pending = new ArrayDeque<>();
    private long received;
    private long acknowledged;
    private int credit;
    private int idleTicks;

    public void offer(long sequence, MovementInput input, int limit) {
        if (sequence <= received) return;
        if (sequence != received + 1 || !input.valid() || pending.size() >= limit) {
            throw new IllegalArgumentException("Invalid, out-of-sequence or overflowing movement stream");
        }
        received = sequence;
        pending.addLast(new Command(sequence, input));
    }

    public void beginTick(int maxCatchUp) {
        credit = Math.min(maxCatchUp, credit + 1);
        idleTicks = pending.isEmpty() ? idleTicks + 1 : 0;
    }

    public Command poll() {
        if (credit == 0 || pending.isEmpty()) return null;
        credit--;
        Command command = pending.removeFirst();
        acknowledged = command.sequence();
        return command;
    }

    public long acknowledged() { return acknowledged; }
    public int idleTicks() { return idleTicks; }
    public int size() { return pending.size(); }
}
