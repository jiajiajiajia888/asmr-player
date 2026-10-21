package com.longjiahao.asmrplayer;

/** Short feed-forward reflections, no feedback, allocation or extra codec stage. */
public final class RoomDistance {
    private final float[][] history;
    private final int[] delays;
    private int write;
    public RoomDistance(int rate){history=new float[5][Math.max(8,(int)Math.ceil(rate*.070)+1)];delays=new int[]{Math.max(1,(int)(rate*.013)),Math.max(1,(int)(rate*.023)),Math.max(1,(int)(rate*.037)),Math.max(1,(int)(rate*.059))};}
    public void push(float l,float r,float al,float ar,float center){write=(write+1)%history[0].length;history[0][write]=l;history[1][write]=r;history[2][write]=al;history[3][write]=ar;history[4][write]=center;}
    private float sample(int mode,int ear,int delay){int channel=mode==1?0:mode==2?1:mode==3?4:mode==4?2+ear:ear;return history[channel][(write-delay+history[0].length)%history[0].length];}
    public void apply(SoundEngine.Settings s,float[] out){
        if((!s.moving() && !s.surround()) || s.distance()==0)return;
        double d=s.distance(),normal=1+.55*d;
        for(int ear=0;ear<2;ear++){float room=.22f*sample(s.copyMode(),1-ear,delays[ear])+.16f*sample(s.copyMode(),ear,delays[2])+.10f*sample(s.copyMode(),1-ear,delays[3])+.07f*sample(s.copyMode(),ear,delays[1-ear]);out[ear]=(float)((out[ear]+room*d)/normal);}
    }
}
