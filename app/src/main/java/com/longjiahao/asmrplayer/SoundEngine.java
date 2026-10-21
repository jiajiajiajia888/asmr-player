package com.longjiahao.asmrplayer;

/** Pure sample-domain processing, shared by playback and export. */
public final class SoundEngine {
    public record Settings(int copyMode, boolean moving, double seconds, double width, boolean surround,double autoThresholdDb,double distance,boolean alternating,double alternateSeconds) {
        public Settings(int copyMode,boolean moving,double seconds,double width,boolean surround,double autoThresholdDb,double distance){this(copyMode,moving,seconds,width,surround,autoThresholdDb,distance,false,.2);}
        public Settings(int copyMode,boolean moving,double seconds,double width,boolean surround,double autoThresholdDb){this(copyMode,moving,seconds,width,surround,autoThresholdDb,0);}
        public Settings(int copyMode,boolean moving,double seconds,double width,boolean surround){this(copyMode,moving,seconds,width,surround,-30);}
        public Settings(int copyMode,boolean moving,double seconds,double width){this(copyMode,moving,seconds,width,false);}
        public Settings {
            if(!Double.isFinite(alternateSeconds) || alternateSeconds<.05 || alternateSeconds>2 || alternating && (moving || surround))throw new IllegalArgumentException("Invalid alternating settings");
            if(copyMode<0 || copyMode>4 || !Double.isFinite(seconds) || seconds<0.1 || seconds>10 || !Double.isFinite(width) || width<0 || width>3 || !Double.isFinite(autoThresholdDb) || autoThresholdDb< -70 || autoThresholdDb>0 || !Double.isFinite(distance) || distance<0 || distance>1) throw new IllegalArgumentException("Invalid sound settings");
        }
    }
    public static final Settings ORIGINAL=new Settings(0,false,3,1);
    private SoundEngine() {}
    public static void process(float left,float right,long frame,int rate,Settings settings,float[] output) {
        if(settings.copyMode()==1)right=left;
        else if(settings.copyMode()==2)left=right;
        else if(settings.copyMode()==3)left=right=(left+right)*0.5f;
        if(settings.moving()) {
            double phase=2*Math.PI*(frame/(double)rate)/settings.seconds();
            double pan=Math.max(-1,Math.min(1,-Math.cos(phase)*settings.width()));
            double angle=(pan+1)*Math.PI/4;
            left=left*(float)Math.cos(angle);right=right*(float)Math.sin(angle);
        }
        output[0]=Math.max(-1,Math.min(1,left));output[1]=Math.max(-1,Math.min(1,right));
    }
}
