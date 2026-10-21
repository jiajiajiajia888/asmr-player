package com.longjiahao.asmrplayer;

/** Local file policy: short start threshold, bounded forward/back buffers. */
public final class PlaybackTuning {
 public static final int MIN_MS=1000,MAX_MS=10000,START_MS=150,REBUFFER_MS=350,BACK_MS=15000;
 public static final long SEEK_TOLERANCE_US=250000;
 private PlaybackTuning(){}
}
