package com.longjiahao.asmrplayer;

/** Monotonic clock arithmetic; independent from wall-clock changes. */
public final class SleepDeadline {
    private long deadline;
    public void set(long now,double minutes){
        if(!Double.isFinite(minutes) || minutes<=0 || minutes>1440)throw new IllegalArgumentException("时间应大于0且不超过1440分钟");
        deadline=Math.addExact(now,Math.max(1,Math.round(minutes*60000)));
    }
    public void cancel(){deadline=0;}
    public boolean active(){return deadline>0;}
    public long remaining(long now){return active()?Math.max(0,deadline-now):0;}
    public boolean expired(long now){return active() && now>=deadline;}
}
