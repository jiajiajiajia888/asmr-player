package com.longjiahao.asmrplayer;
/** A viewport-wide horizontal swipe previews up to two minutes; commit only on release. */
public final class SwipeSeek {
 private SwipeSeek(){}
 public static long target(long start,double dx,int width,long duration){if(width<=0 || !Double.isFinite(dx))return SeekMath.offset(start,0,duration);return SeekMath.offset(start,Math.round(dx/width*120000),duration);}
}
