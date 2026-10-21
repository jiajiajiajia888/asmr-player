package com.longjiahao.asmrplayer;

public final class SeekMath {
    private SeekMath() {}
    public static long offset(long position, long delta, long duration) {
        long target;
        try { target = Math.addExact(Math.max(0, position), delta); }
        catch (ArithmeticException e) { target = delta > 0 ? Long.MAX_VALUE : 0; }
        target = Math.max(0, target);
        return duration >= 0 ? Math.min(duration, target) : target;
    }
    public static long fromProgress(int progress, long duration) {
        if (duration <= 0) return 0;
        return Math.round(duration * (Math.max(0, Math.min(10000, progress)) / 10000.0));
    }
}
