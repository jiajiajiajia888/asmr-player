package com.longjiahao.asmrplayer;

/** Detect on input, before panning/HRTF. Persistent imbalance, hysteresis and fades. */
public final class AutoChannelFill {
 private final double fadeAlpha;private final int holdFrames;private final float[] leftWindow,rightWindow;private int windowIndex;
 private double leftPower,rightPower,leftMix,rightMix,thresholdDb=Double.NaN,quietPower;
 private int candidate,candidateFrames,direction;
 public AutoChannelFill(int rate){if(rate<=0)throw new IllegalArgumentException("sample rate");int window=Math.max(1,(int)Math.round(rate*.010));leftWindow=new float[window];rightWindow=new float[window];fadeAlpha=1-Math.exp(-1/(rate*.012));holdFrames=Math.max(1,(int)Math.round(rate*.018));}
 public void process(float left,float right,double db,float[] output){
  if(db!=thresholdDb){thresholdDb=db;quietPower=Math.pow(10,db/10);}
  float l=left*left,r=right*right;leftPower=Math.max(0,leftPower+(l-leftWindow[windowIndex])/leftWindow.length);rightPower=Math.max(0,rightPower+(r-rightWindow[windowIndex])/rightWindow.length);leftWindow[windowIndex]=l;rightWindow[windowIndex]=r;if(++windowIndex==leftWindow.length)windowIndex=0;
  int wanted=0;
  // Enter at 12 dB disparity; retain to 8 dB and threshold +3 dB.
  if(direction==1 && leftPower>1e-8 && rightPower<quietPower*2 && rightPower<leftPower*.158489)wanted=1;
  else if(direction==2 && rightPower>1e-8 && leftPower<quietPower*2 && leftPower<rightPower*.158489)wanted=2;
  else if(leftPower>1e-8 && rightPower<quietPower && rightPower<leftPower*.063096)wanted=1;
  else if(rightPower>1e-8 && leftPower<quietPower && leftPower<rightPower*.063096)wanted=2;
  if(wanted!=candidate){candidate=wanted;candidateFrames=0;}
  if(candidateFrames<holdFrames)candidateFrames++;
  if(candidateFrames>=holdFrames)direction=candidate;
  leftMix+=fadeAlpha*((direction==2?1:0)-leftMix);rightMix+=fadeAlpha*((direction==1?1:0)-rightMix);
  // Smooth additive crossfeed: no sample-by-sample gate and no subtraction
  // that can erase the newly audible source before detection catches up.
  output[0]=combine(left,right*leftMix);output[1]=combine(right,left*rightMix);
 }
 // Preserve the source exactly; only reduce the added copy when it exceeds headroom.
 private static float combine(float source,double copy){if(source*copy>0)copy=Math.copySign(Math.min(Math.abs(copy),Math.max(0,1-Math.abs(source))),copy);return (float)Math.max(-1,Math.min(1,source+copy));}
}
