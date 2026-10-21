package com.longjiahao.asmrplayer;

/** Four FIR paths preserve stereo as two rotating sources. No per-sample allocations. */
public final class BinauralOrbit {
    private final int rate,taps;private final float[] leftHistory,rightHistory,autoLeftHistory,autoRightHistory;private final AutoChannelFill automatic;private final float[] filled=new float[2];private final KernelCache first,second;private long uses;
    private static final class KernelCache {SoundEngine.Settings settings;long frame=Long.MIN_VALUE,used;final float[][] kernels;KernelCache(int taps){kernels=new float[4][taps];}}
    private int write;
    private final CenterChannel alternateAutoCenter;private long alternateAutoFrame=Long.MIN_VALUE;private float alternateAutoMono;
    private final CenterChannel center;private final float[] centerHistory;private final RoomDistance room;
    public BinauralOrbit(int rate){this.rate=rate;taps=Math.max(16,(int)Math.ceil(128*rate/44100.0));leftHistory=new float[taps];rightHistory=new float[taps];autoLeftHistory=new float[taps];autoRightHistory=new float[taps];automatic=new AutoChannelFill(rate);center=new CenterChannel(rate);alternateAutoCenter=new CenterChannel(rate);centerHistory=new float[taps];room=new RoomDistance(rate);first=new KernelCache(taps);second=new KernelCache(taps);}
    public void push(float left,float right){push(left,right,-30);}
    public void push(float left,float right,double thresholdDb){write=(write+1)%taps;leftHistory[write]=left;rightHistory[write]=right;automatic.process(left,right,thresholdDb,filled);autoLeftHistory[write]=filled[0];autoRightHistory[write]=filled[1];centerHistory[write]=center.process(left,right);room.push(left,right,filled[0],filled[1],centerHistory[write]);}
    public void render(long frame,SoundEngine.Settings settings,float[] output){
        if(settings.alternating()){float mono=settings.copyMode()==1?leftHistory[write]:settings.copyMode()==2?rightHistory[write]:centerHistory[write];if(settings.copyMode()==4){if(alternateAutoFrame!=frame){alternateAutoFrame=frame;alternateAutoMono=alternateAutoCenter.process(autoLeftHistory[write],autoRightHistory[write]);}mono=alternateAutoMono;}LeftRightAlternation.process(mono,frame,rate,settings.alternateSeconds(),output);return;}
        float[] sourceL=settings.copyMode()==3?centerHistory:settings.copyMode()==4?autoLeftHistory:leftHistory,sourceR=settings.copyMode()==3?centerHistory:settings.copyMode()==4?autoRightHistory:rightHistory;
        if(!settings.surround()){SoundEngine.process(sourceL[write],sourceR[write],frame,rate,settings,output);room.apply(settings,output);return;}
        KernelCache cache;if(settings.equals(first.settings))cache=first;else if(settings.equals(second.settings))cache=second;else{cache=first.used<=second.used?first:second;cache.settings=settings;cache.frame=Long.MIN_VALUE;}cache.used=++uses;float[][] kernels=cache.kernels;
        if(cache.frame==Long.MIN_VALUE || frame-cache.frame>=32 || frame<cache.frame){double heading=-90+360*(frame/(double)rate)/settings.seconds();double separation=(settings.copyMode()==0 || settings.copyMode()==4)?20:0;fill(kernels[0],heading-separation,0);fill(kernels[1],heading-separation,1);fill(kernels[2],heading+separation,0);fill(kernels[3],heading+separation,1);cache.frame=frame;}
        float l=0,r=0;int index=write;
        for(int t=0;t<taps;t++){float a=sourceL[index],b=sourceR[index];if(settings.copyMode()==1)b=a;else if(settings.copyMode()==2)a=b;else if(settings.copyMode()==3)a=b=(a+b)*.5f;l+=a*kernels[0][t]+b*kernels[2][t];r+=a*kernels[1][t]+b*kernels[3][t];if(--index<0)index=taps-1;}
        float dryL=sourceL[write],dryR=sourceR[write];if(settings.copyMode()==1)dryR=dryL;else if(settings.copyMode()==2)dryL=dryR;else if(settings.copyMode()==3)dryL=dryR=(dryL+dryR)*.5f;
        float wet=(float)Math.min(1,settings.width()*.85);output[0]=dryL*(1-wet)+l*.45f*wet;output[1]=dryR*(1-wet)+r*.45f*wet;
        if(settings.width()>2){double pan=-Math.cos(2*Math.PI*(frame/(double)rate)/settings.seconds()),extra=settings.width()-2;float weak=(float)(1-.4*extra*Math.abs(pan));if(pan<0)output[1]*=weak;else output[0]*=weak;}
        room.apply(settings,output);
    }
    private void fill(float[] kernel,double angle,int ear){angle=((angle%360)+360)%360;if(angle>180){angle=360-angle;ear=1-ear;}double slot=angle/15;int first=Math.min(12,(int)slot),next=Math.min(12,first+1);float mix=(float)(slot-first);for(int t=0;t<taps;t++){double position=t*44100.0/rate;int n=(int)position;float fraction=(float)(position-n);float a=KemarData.sample(first,ear,n)*(1-fraction)+KemarData.sample(first,ear,n+1)*fraction;float b=KemarData.sample(next,ear,n)*(1-fraction)+KemarData.sample(next,ear,n+1)*fraction;kernel[t]=(a*(1-mix)+b*mix)*(44100f/rate);}}
}
