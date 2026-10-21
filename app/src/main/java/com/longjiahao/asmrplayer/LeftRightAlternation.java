package com.longjiahao.asmrplayer;

/** Sample-clock switching with non-overlapping two-millisecond edge fades. */
public final class LeftRightAlternation {
 public static void process(float mono,long frame,int rate,double seconds,float[] output){
  long interval=Math.max(1,Math.round(rate*seconds));
  long position=Math.floorMod(frame,interval);
  long edge=Math.max(1,Math.min(interval/4,Math.round(rate*.002)));
  double envelope=Math.min(1,Math.min(position/(double)edge,(interval-1-position)/(double)edge));
  float value=(float)(Math.max(-1,Math.min(1,mono))*Math.sin(envelope*Math.PI/2));
  boolean left=(Math.floorDiv(frame,interval)&1)==0;
  output[0]=left?value:0;output[1]=left?0:value;
 }
 private LeftRightAlternation(){}
}
