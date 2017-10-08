package ru.programpark.planners.common;

// Интервал планирования
public class SchedulingFrame {
    public final SchedulingData data;
    public final int runIndex, locoFrameIndex, teamFrameIndex;
    public static final int LAST_INDEX = 11111111;
    public final long range, rangeStart, rangeEnd,
        locoFrame, locoFrameStart, locoFrameEnd,
        teamFrame, teamFrameStart, teamFrameEnd;

    public SchedulingFrame(int runIndex, long range, long rangeStart,
                           long locoFrame, long teamFrame) {
        this.data = SchedulingData.getFrameData();
        this.runIndex = runIndex;
        this.range = range;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeStart + range;
        this.locoFrame = locoFrame;
        this.locoFrameIndex = 0;
        this.teamFrame = teamFrame;
        this.teamFrameIndex = 0;
        this.locoFrameStart = this.locoFrameEnd =
            this.teamFrameStart = this.teamFrameEnd =
            rangeStart;
    }

    private SchedulingFrame(int runIndex, long range, long rangeStart,
                            long locoFrame, int locoFrameIndex,
                            long locoFrameStart, long teamFrame,
                            int teamFrameIndex, long teamFrameStart) {
        this.data = SchedulingData.getFrameData();
        this.runIndex = runIndex;
        this.range = range;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeStart + range;
        this.locoFrame = locoFrame;
        this.locoFrameIndex = locoFrameIndex;
        this.locoFrameStart = locoFrameStart;
        this.locoFrameEnd = locoFrameStart + locoFrame;
        this.teamFrame = teamFrame;
        this.teamFrameIndex = teamFrameIndex;
        this.teamFrameStart = teamFrameStart;
        this.teamFrameEnd = teamFrameStart + teamFrame;
    }

    public SchedulingFrame nextFrame() {
        return new SchedulingFrame(runIndex, range, rangeStart,
            locoFrame, nextLocoFrameIndex(), locoFrameEnd,
            teamFrame, 0, locoFrameEnd);
    }

    private int nextLocoFrameIndex() {
        return (locoFrameEnd > rangeEnd) ? LAST_INDEX : (locoFrameIndex + 1);
    }

    private int nextTeamFrameIndex() {
        return (locoFrameIndex - 1) * (int) (locoFrame / teamFrame) + 1;
    }

    public SchedulingFrame nextTeamFrame() {
        if (teamFrameEnd < locoFrameEnd) {
            if (teamFrameIndex == 0) {
                return new SchedulingFrame(runIndex, range, rangeStart,
                    locoFrame, locoFrameIndex, locoFrameStart,
                    teamFrame, nextTeamFrameIndex(), teamFrameStart);
            } else {
                return new SchedulingFrame(runIndex, range, rangeStart,
                    locoFrame, locoFrameIndex, locoFrameStart,
                    teamFrame, teamFrameIndex + 1, teamFrameEnd);
            }
        } else {
            return nextFrame();
        }
    }

    public SchedulingFrame nextLocoFrame() {
        return (locoFrameIndex == 0) ? this : nextTeamFrame();
    }
}
