package com.longjiahao.asmrplayer;

/** Fits the original aspect ratio into a viewport, with letterboxing and no crop. */
public final class VideoFit {
    private VideoFit(){}
    public record Size(int width,int height){}
    public static Size fit(int width,int height,double ratio){
        if(width<=0 || height<=0)return new Size(0,0);
        if(!Double.isFinite(ratio) || ratio<=0)ratio=16.0/9;
        if(width/(double)height>ratio)return new Size(Math.max(1,(int)Math.round(height*ratio)),height);
        return new Size(width,Math.max(1,(int)Math.round(width/ratio)));
    }
}
