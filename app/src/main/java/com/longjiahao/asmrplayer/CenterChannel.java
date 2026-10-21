package com.longjiahao.asmrplayer;

/** Level-aware mono fold-down with a smooth fallback for opposing polarity. */
public final class CenterChannel {
    private final double alpha;
    private double lp,rp,cross,gain=1,fallback;
    public CenterChannel(int rate){alpha=1-Math.exp(-1/(rate*.020));}
    public float process(float left,float right){
        lp+=alpha*(left*left-lp);rp+=alpha*(right*right-rp);cross+=alpha*(left*right-cross);
        double correlation=cross/Math.sqrt(Math.max(1e-20,lp*rp));
        double target=Math.max(0,Math.min(1,(-correlation-.2)/.5));
        fallback+=alpha*(target-fallback);
        double a=.5+.5*fallback,b=1-a;
        double power=a*a*lp+b*b*rp+2*a*b*cross;
        double wanted=power>1e-15?Math.min(2,Math.sqrt(Math.max(lp,rp)/power)):1;
        gain+=alpha*(wanted-gain);
        return (float)Math.max(-1,Math.min(1,(a*left+b*right)*gain));
    }
}
